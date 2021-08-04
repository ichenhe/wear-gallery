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

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.comm.*
import cc.chenhe.weargallery.common.comm.bean.SendResp
import cc.chenhe.weargallery.ui.main.MainAty
import cc.chenhe.weargallery.utils.*
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import me.chenhe.lib.wearmsger.BothWayHub
import me.chenhe.lib.wearmsger.bean.BothWayCallback
import org.koin.android.ext.android.get
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private const val EXTRA_JOBS = "jobs"
private const val TIMEOUT = 1000 * 40L

private const val TAG = "SendPicturesService"

private const val SEND_OK = 0
private const val SEND_FILE_NOT_EXIST = 1
private const val SEND_REQUEST_FAILED = 2
private const val SEND_TIMEOUT = 3
private const val SEND_RECEIVE_ERROR = 4
private const val SEND_FAILED_UNKNOWN = 5

class SendPicturesService : LifecycleService() {

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
        fun add(context: Context, images: Collection<Image>, target: String?) {
            val jobs = images.map { Job(it, target) }.toTypedArray()
            Intent(context.applicationContext, SendPicturesService::class.java).also { intent ->
                intent.putExtra(EXTRA_JOBS, jobs)
                context.applicationContext.startService(intent)
            }
        }
    }

    private val isSending = AtomicBoolean(false)

    private val queue = LinkedBlockingQueue<Job>()
    private var successCount = 0
    private var sendCount = 0
    private var currentSending: Job? = null
    private val failed = mutableListOf<Job>()

    private val totalCount get() = sendCount + queue.size + if (currentSending != null) 1 else 0

    override fun onCreate() {
        super.onCreate()
        createSendingNotificationChannel()
        createSendResultNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val jobs = intent?.getParcelableArrayExtra(EXTRA_JOBS)?.filterIsInstance<Job>()
        if (jobs.isNullOrEmpty()) {
            Timber.tag(TAG).w("Can not find job information, drop this command.")
            return START_NOT_STICKY
        }
        queue.addAll(jobs)
        startSendIfNecessary()
        return START_NOT_STICKY
    }

    private fun startSendIfNecessary() {
        // update notification
        startForeground(NOTIFY_ID_SENDING, buildSendingNotification())
        if (isSending.get()) {
            return
        }
        if (queue.isEmpty()) {
            stopSelf()
            return
        }

        // not in sending, still has job, let's start
        if (isSending.compareAndSet(false, true)) {
            send()
        }
    }

    /**
     * Send data in a loop until the [queue] is empty.
     *
     * It is the caller's responsibility to ensure that this function is not called repeatedly.
     */
    private fun send() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                val current = queue.poll().also { currentSending = it }
                if (current == null) {
                    onFinish()
                    break
                }
                val r = realSend(current)
                sendCount++
                if (r != SEND_OK) {
                    failed.add(current)
                } else {
                    successCount++
                }
                val abort = when (r) {
                    SEND_TIMEOUT -> {
                        // The watch is not responding and there is no need to continue sending.
                        failed.addAll(queue)
                        queue.clear()
                        onFailed(R.string.send_images_send_data_time_out)
                        true
                    }
                    SEND_RECEIVE_ERROR -> {
                        // The watch failed to save file, maybe no permissions.
                        // There is no need to continue sending.
                        failed.addAll(queue)
                        queue.clear()
                        onFailed(R.string.send_images_receive_error)
                        true
                    }
                    else -> false
                }
                if (abort) {
                    break
                }
            }
            isSending.set(false)
        }
    }

    /**
     * Can be only called from [send].
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun realSend(job: Job): Int {
        withContext(Dispatchers.Main) {
            // update notification
            startForeground(NOTIFY_ID_SENDING, buildSendingNotification())
        }
        return withContext(Dispatchers.IO) {
            try {
                // Test if file is exist and make sure it will be closed with `use`.
                this@SendPicturesService.contentResolver.openAssetFileDescriptor(job.image.uri, "r")
                    .use {}
                val req = PutDataMapRequest.create(PATH_SEND_IMAGE)
                req.dataMap.apply {
                    putAsset(ITEM_IMAGE, createImageAsset(job.image.uri))
                    job.target?.let { putString(ITEM_SAVE_PATH, it) }
                    putInt(ITEM_INDEX, sendCount + 1)
                    putInt(ITEM_TOTAL, totalCount)
                    val imageInfo = get<Moshi>().adapter(Image::class.java).toJson(job.image)
                    putString(ITEM_IMAGE_INFO, imageInfo)
                }
                BothWayHub.requestForMessage(this@SendPicturesService, req, TIMEOUT)
                    .let { callback ->
                        when (callback.result) {
                            BothWayCallback.Result.OK -> {
                                val resp = callback.getStringData()?.let {
                                    get<Moshi>().adapter(SendResp::class.java).fromJson(it)
                                }
                                if (resp == null || resp.result != RESULT_OK) {
                                    return@withContext SEND_RECEIVE_ERROR
                                } else {
                                    return@withContext SEND_OK
                                }
                            }
                            BothWayCallback.Result.REQUEST_FAIL -> return@withContext SEND_REQUEST_FAILED
                            BothWayCallback.Result.TIMEOUT -> return@withContext SEND_TIMEOUT
                        }
                    }
            } catch (e: FileNotFoundException) {
                Timber.tag(TAG).d("Image file not exist, uri=%s", job.image.uri)
                return@withContext SEND_FILE_NOT_EXIST
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext SEND_FAILED_UNKNOWN
            }
        }
    }

    /**
     * Called only the watch is not responding.
     */
    private fun onFailed(@StringRes message: Int) {
        stopForeground(true)
        notifySendFailed(message)
        stopSelf()
    }

    private fun onFinish() {
        stopForeground(true)
        notifySendFinish()
        stopSelf()
    }

    private fun createSendingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFY_CHANNEL_ID_SENDING,
                getString(R.string.notify_channel_sending_name), NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.notify_channel_sending_description)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createSendResultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFY_CHANNEL_ID_SEND_RESULT,
                getString(R.string.notify_channel_send_result_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = getString(R.string.notify_channel_send_result_description)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildSendingNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_SENDING)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle(getString(R.string.notify_send_images_title))
            .setContentText(
                getString(
                    R.string.notify_send_images_content, sendCount, totalCount,
                    currentSending?.image?.name
                )
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun notifySendFinish() {
        val intent = Intent(this, MainAty::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val n = NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_SEND_RESULT)
            .setSmallIcon(R.drawable.ic_notify_done)
            .setContentTitle(getString(R.string.notify_send_images_success_title))
            .setContentText(getString(R.string.notify_send_images_success_content, successCount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFY_ID_SEND_RESULT, n)
    }

    private fun notifySendFailed(@StringRes message: Int) {
        val intent = Intent(this, MainAty::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val text =
            getString(R.string.notify_send_images_failed_content, getString(message), successCount)

        val n = NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_SEND_RESULT)
            .setSmallIcon(R.drawable.ic_notify_error)
            .setContentTitle(getString(R.string.notify_send_images_failed_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFY_ID_SEND_RESULT, n)
    }

    private suspend fun createImageAsset(uri: Uri): Asset = withContext(Dispatchers.IO) {
        Asset.createFromUri(uri)
    }
}