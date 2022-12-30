/**
 * Copyright (C) 2020 Chenhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cc.chenhe.weargallery.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.comm.PATH_CHANNEL_BATCH_SEND
import cc.chenhe.weargallery.common.comm.bean.SendItem
import cc.chenhe.weargallery.common.util.toBytes
import cc.chenhe.weargallery.utils.NotificationUtils
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.CHANNEL_ID_SENDING
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.CHANNEL_ID_SEND_RESULT
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.NOTIFY_ID_SENDING
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.NOTIFY_ID_SEND_RESULT
import cc.chenhe.weargallery.utils.getParcelableArray
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class SendPicturesService : LifecycleService() {
    private val notificationUtil: NotificationUtils by inject()

    @Parcelize
    data class Job(
        val image: Image,
        /**
         * Relative path to save on watch (without / prefix), `null` means use default location decided by watch
         * client.
         */
        val target: String?
    ) : Parcelable

    companion object {
        private const val TAG = "SendPicturesService"
        private const val WATCHDOG_TIMEOUT = 60 * 1000L
        private const val UPDATE_PROGRESS_INTERVAL = 1000L
        private const val EXTRA_JOBS = "jobs"
        private const val EXTRA_NODE_ID = "node-id"
        private const val EXTRA_NODE_NAME = "node-name"

        /**
         * In case the watch becoming unresponsive.
         *
         * Only calculate the waiting time for ACK -- the duration that send file is not recorded.
         */
        private const val MSG_SEND_WATCH_DOG = -1
        private const val MSG_START_NEW_ITEM = 0
        private const val MSG_FINISH = 10
        private const val MSG_CANCEL = 20
        private const val MSG_AKC_FAILED = 30
        private const val MSG_FILE_NOT_FOUND = 50
        private const val MSG_SEND_IO_ERROR = 60
        private const val MSG_SEND_SIZE_MISMATCH = 70
        private const val MSG_FAILED_UNKNOWN = 80
        private const val MSG_UPDATE_PROGRESS = 90 // arg1

        private const val PROGRESS_SENT = "sent_size" // long
        private const val PROGRESS_TOTAL = "total_size" // long

        fun add(context: Context, images: Collection<Image>, device: Node, target: String?) {
            val jobs = images.map { Job(it, target) }.toTypedArray()
            Intent(context.applicationContext, SendPicturesService::class.java).also { intent ->
                intent.putExtra(EXTRA_JOBS, jobs)
                intent.putExtra(EXTRA_NODE_ID, device.id)
                intent.putExtra(EXTRA_NODE_NAME, device.displayName)
                context.applicationContext.startService(intent)
            }
        }
    }

    private val isSending = AtomicBoolean(false)

    private val queue = LinkedBlockingQueue<Job>()
    private var totalCount = 0
    private var sentCount = 0
    private var currentSending: Job? = null

    private val moshi: Moshi by inject()
    private val cr: ContentResolver by lazy { contentResolver }
    private val notifyManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        fun resetWatchDog() {
            removeMessages(MSG_SEND_WATCH_DOG)
            sendEmptyMessageDelayed(MSG_SEND_WATCH_DOG, WATCHDOG_TIMEOUT)
        }

        fun stopWatchDog() {
            removeMessages(MSG_SEND_WATCH_DOG)
        }

        private var flag = false

        override fun handleMessage(msg: Message) {
            if (flag) {
                // Ignore the follow-up results, because it could be a chain reaction.
                return
            }

            if (msg.what == MSG_START_NEW_ITEM) {
                notifyManager.notify(NOTIFY_ID_SENDING, buildSendingNotify(0, 1))
                stopWatchDog()
                return
            } else if (msg.what == MSG_UPDATE_PROGRESS) {
                val sent = msg.data.getLong(PROGRESS_SENT, 0L)
                val total = msg.data.getLong(PROGRESS_TOTAL, 1L)
                notifyManager.notify(NOTIFY_ID_SENDING, buildSendingNotify(sent, total))
                return
            }

            // Take this message as the final signal.
            flag = true
            removeCallbacksAndMessages(null)
            when (msg.what) {
                MSG_FINISH -> onFinish()
                MSG_CANCEL -> onFailed(R.string.send_images_fail_cancel)
                MSG_AKC_FAILED -> onFailed(R.string.send_images_fail_ack_failed)
                MSG_FILE_NOT_FOUND -> onFailed(R.string.send_images_fail_file_not_found)
                MSG_SEND_IO_ERROR -> onFailed(R.string.send_images_fail_io)
                MSG_SEND_SIZE_MISMATCH -> onFailed(R.string.send_images_fail_size_mismatch)
                MSG_FAILED_UNKNOWN -> onFailed(R.string.send_images_fail_unknown)

                MSG_SEND_WATCH_DOG -> {
                    sendThread = null
                    callbackThread = null
                    onFailed(R.string.send_images_fail_no_response)
                }
            }
        }
    }

    private fun getMsg(what: Int, obj: Any?): Message = handler.obtainMessage(what, obj)

    override fun onCreate() {
        super.onCreate()
        notificationUtil.registerNotificationChannel(CHANNEL_ID_SENDING)
        notificationUtil.registerNotificationChannel(CHANNEL_ID_SEND_RESULT)
    }

    private lateinit var nodeId: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val jobs = intent?.getParcelableArray<Job>(EXTRA_JOBS)
        nodeId = intent?.getStringExtra(EXTRA_NODE_ID) ?: ""
        if (jobs.isNullOrEmpty()) {
            Timber.tag(TAG).w("Can not find job information, drop this command.")
            return START_NOT_STICKY
        }
        if (nodeId.isEmpty()) {
            Timber.tag(TAG).w("Can not find target node id, drop this command.")
            return START_NOT_STICKY
        }
        totalCount += jobs.size
        queue.addAll(jobs)
        startSendIfNecessary()
        return START_NOT_STICKY
    }

    private fun startSendIfNecessary() {
        // update notification
        startForeground(NOTIFY_ID_SENDING, buildSendingNotify(0, 1))
        if (isSending.get()) {
            return
        }

        // not in sending, still has job, let's start
        if (isSending.compareAndSet(false, true)) {
            send()
        }
    }

    private var sendThread: SendThread? = null
        set(value) {
            field?.interrupt()
            field = value
        }
    private var callbackThread: AckThread? = null
        set(value) {
            field?.interrupt()
            field = value
        }

    /**
     * Send data in a loop until the [queue] is empty.
     *
     * It is the caller's responsibility to ensure that this function is not called repeatedly.
     */
    private fun send() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                channel = channelClient.openChannel(nodeId, PATH_CHANNEL_BATCH_SEND).await()
                callbackThread = AckThread(channel!!).apply { start() }
                sendThread = SendThread(channel!!).apply { start() }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to open channel.")
                onFailed(R.string.send_images_fail_io)
            }
        }
    }


    // Graph::Easy DSL - http://bloodgate.com/perl/graph/manual/syntax.html
    // [ Send 4B (int)\nsize of header] -> [ Send header\n(SendItem's utf8 json)] -> [Send\nfile] -> [Wait\nfor ACK] -> [ Send 4B (int)\nsize of header]
    // [Wait\nfor ACK] ->{ label: END; } [Close\nstream]
    /*
    Protocol:
      +------------------------------------------------------------------+
      v                                                                  |
    +----------------+     +------------------------+     +------+     +---------+        +--------+
    | Send 4B (int)  |     |      Send header       |     | Send |     |  Wait   |  END   | Close  |
    | size of header | --> | (SendItem's utf8 json) | --> | file | --> | for ACK | -----> | stream |
    +----------------+     +------------------------+     +------+     +---------+        +--------+
    */

    private val channelClient by lazy { Wearable.getChannelClient(this) }
    private val sendBarrier = CyclicBarrier(2)
    private var channel: ChannelClient.Channel? = null

    /**
     * Send data in a loop until the [queue] is empty.
     *
     * It is the caller's responsibility to ensure that this thread is not started repeatedly.
     */
    private inner class SendThread(private val channel: ChannelClient.Channel) : Thread() {

        private fun sendInternal(ous: OutputStream) {
            while (!isInterrupted) {
                val current = queue.poll().also { currentSending = it } ?: break
                handler.sendMessage(getMsg(MSG_START_NEW_ITEM, current.image))

                cr.openInputStream(current.image.uri)?.use { ins ->
                    val entry = SendItem(current.image, current.target, sentCount + 1, totalCount)
                    val entryBytes = moshi.adapter(SendItem::class.java).toJson(entry).toByteArray()
                    ous.write(entryBytes.size.toBytes())
                    ous.write(entryBytes)

                    // send file content
                    var bytesCopied: Long = 0
                    val buffer = ByteArray(4 * 1024)
                    var bytes = ins.read(buffer)
                    while (bytes >= 0) {
                        ous.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (bytesCopied > current.image.size) {
                            // something goes wrong
                            break
                        }
                        updateProgress(bytesCopied, current.image.size)
                        bytes = ins.read(buffer)
                    }
                    assert(bytesCopied == current.image.size) {
                        "content len ($bytesCopied) != image size (${current.image.size})"
                    }
                }
                // finish the sending of an entry
                ous.flush()
                Timber.tag(TAG).v("SendThread: Send item done. %s", current.image.uri.toString())
                handler.resetWatchDog()
                sendBarrier.await()
                sentCount++
            } // ~while
        }


        private var lastUpdateTime = 0L
        private fun updateProgress(sent: Long, total: Long) {
            val t = SystemClock.uptimeMillis()
            if (t - lastUpdateTime < UPDATE_PROGRESS_INTERVAL) {
                return
            }
            lastUpdateTime = t
            handler.obtainMessage(MSG_UPDATE_PROGRESS).apply {
                data.putLong(PROGRESS_SENT, sent)
                data.putLong(PROGRESS_TOTAL, total)
            }.also {
                handler.sendMessage(it)
            }
        }

        override fun run() {
            Timber.tag(TAG).i("SendThread: start")
            Tasks.await(channelClient.getOutputStream(channel))?.use { ous ->
                try {
                    sendInternal(ous)
                } catch (e: FileNotFoundException) {
                    Timber.tag(TAG)
                        .w("SendThread: File not found. %s", currentSending?.image?.uri.toString())
                    handler.sendMessage(getMsg(MSG_FILE_NOT_FOUND, currentSending?.image))
                } catch (e: IOException) {
                    Timber.tag(TAG).w("SendThread: IOException")
                    handler.sendMessage(getMsg(MSG_SEND_IO_ERROR, currentSending?.image))
                } catch (e: InterruptedException) {
                    // dealt with later
                } catch (e: BrokenBarrierException) {
                    Timber.tag(TAG).w("SendThread: Barrier broken")
                    handler.sendEmptyMessage(MSG_AKC_FAILED)
                } catch (e: AssertionError) {
                    Timber.tag(TAG).w(e, "SendThread: AssertionError")
                    handler.sendEmptyMessage(MSG_SEND_SIZE_MISMATCH)
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "SendThread: Unknown error.")
                    handler.sendEmptyMessage(MSG_FAILED_UNKNOWN)
                }
                if (isInterrupted) {
                    Timber.tag(TAG).i("SendThread: Interrupted")
                    handler.sendEmptyMessage(MSG_CANCEL)
                }
            } ?: kotlin.run {
                Timber.tag(TAG).w("SendThread: Failed to get stream")
                handler.sendEmptyMessage(MSG_SEND_IO_ERROR)
            }
            Timber.tag(TAG).i("SendThread: exit")
        } // ~run
    }

    private inner class AckThread(private val channel: ChannelClient.Channel) : Thread() {
        override fun run() {
            Timber.tag(TAG).i("AckThread: start")
            Tasks.await(channelClient.getInputStream(channel))?.use { ins ->
                while (!isInterrupted) {
                    try {
                        val v = ins.read()
                        handler.stopWatchDog()
                        if (v == -1) {
                            Timber.tag(TAG).i("AckThread: ACK said it was done.")
                            handler.sendEmptyMessage(MSG_FINISH)
                            break
                        }
                        if (v == 1) {
                            // success ACK
                            Timber.tag(TAG).d("AckThread: Receive ACK.")
                            sendBarrier.await()
                        } else {
                            // unknown ACK
                            Timber.tag(TAG).w("AckThread: Unknown ACK=%d", v)
                            handler.sendEmptyMessage(MSG_AKC_FAILED)
                            break
                        }
                    } catch (e: IOException) {
                        Timber.tag(TAG).w(e, "AckThread: Failed to read ACK.")
                        handler.sendEmptyMessage(MSG_AKC_FAILED)
                        break
                    } catch (e: BrokenBarrierException) {
                        Timber.tag(TAG).w("AckThread: Barrier broken")
                        break
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "AckThread: Unknown error.")
                        handler.sendEmptyMessage(MSG_FAILED_UNKNOWN)
                    }
                } // ~while
                if (isInterrupted) {
                    Timber.tag(TAG).i("AckThread: Interrupted")
                }
            } ?: kotlin.run {
                Timber.tag(TAG).w("AckThread: Failed to get stream")
                handler.sendEmptyMessage(MSG_SEND_IO_ERROR)
                return
            }
        }
    }

    private fun onFailed(@StringRes message: Int) {
        sendThread = null
        callbackThread = null
        channel?.also { channelClient.close(it) }
        stopForeground(true)
        notifySendFailed(message, sentCount)
        stopSelf()
    }

    private fun onFinish() {
        sendThread = null
        callbackThread = null
        channel?.also { channelClient.close(it) }
        stopForeground(true)
        notifySendFinish(sentCount)
        stopSelf()
    }

    private val sendingNotifyBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID_SENDING)
            .setSmallIcon(R.drawable.ic_send)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
    }

    private fun buildSendingNotify(sentSize: Long, totalSize: Long): Notification {
        return sendingNotifyBuilder
            .setContentTitle(
                getString(
                    R.string.notify_send_images_title,
                    sentCount + 1,
                    totalCount
                )
            )
            .setContentText(
                (currentSending?.image?.name ?: currentSending?.image?.uri?.toString() ?: "")
                        + "  " + if (totalSize > 1) totalSize.fileSizeStr() else ""
            )
            .setProgress(100, (sentSize / totalSize.toDouble() * 100).toInt(), false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun notifySendFinish(sentCount: Int) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID_SEND_RESULT)
            .setSmallIcon(R.drawable.ic_notify_done)
            .setContentTitle(getString(R.string.notify_send_images_success_title))
            .setContentText(getString(R.string.notify_send_images_success_content, sentCount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFY_ID_SEND_RESULT, n)
    }

    private fun notifySendFailed(@StringRes message: Int, sentCount: Int) {
        val text =
            getString(R.string.notify_send_images_failed_content, getString(message), sentCount)

        val n = NotificationCompat.Builder(this, CHANNEL_ID_SEND_RESULT)
            .setSmallIcon(R.drawable.ic_notify_error)
            .setContentTitle(getString(R.string.notify_send_images_failed_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFY_ID_SEND_RESULT, n)
    }

    private fun Long.fileSizeStr(): String {
        return when {
            this < 1024 -> "$this Byte"
            this < 1024 * 1024 -> String.format("%.2f KB", this / 1024f)
            else -> String.format("%.2f MB", this / (1024 * 1024f))
        }
    }
}