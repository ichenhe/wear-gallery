package cc.chenhe.weargallery.service

import android.content.Context
import cc.chenhe.weargallery.common.comm.PATH_CHANNEL_BATCH_SEND
import cc.chenhe.weargallery.common.comm.bean.SendItem
import cc.chenhe.weargallery.common.util.toInt
import cc.chenhe.weargallery.repository.ImageRepository
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier

class ChannelListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "ChannelService"

        private const val CACHE_TMP_FILE = "batchReceiveCache.tmp"
    }

    private val channelClient: ChannelClient by lazy { Wearable.getChannelClient(this) }
    private val moshi: Moshi by inject()
    private val context: Context get() = this

    private val imageRepo: ImageRepository by inject()

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        Timber.tag(TAG).d("ChannelOpened: %s, from %s", channel.path, channel.nodeId)
        if (channel.path != PATH_CHANNEL_BATCH_SEND) {
            return
        }
        val rec = ReceiveThread(channel).apply { start() }
        AckThread(channel, rec).apply { start() }
    }

    private val receiveBarrier = CyclicBarrier(2)

    //    [ACK Start]{ border: double; } -> [ A await ]{ label: "await"; } ->{ label: continue; } [Send ACK] -> [ A await ]
    //    [ A await ]{ label: "await"; } -> {label: "Rec finished"; } [A END]{ label: END; border: double; }
    //
    //    [Receiver Start]{ border: double; } -> [ Receive ] ->{ label: success; } [R1 await]{ label: "await"; } -> [Receive]
    //    [ Receive ] ->{ label: eof; } [ set finish ] -> [R2 await]{ label: "await"; } -> [R END]{ label: END; border: double; }
    /*
                                         +---------------------------------------+
                                         v                                       |
             #================#        +------------+  Rec finished   #=======#  |
             H   ACK Start    H -----> |   await    | --------------> H  END  H  |
             #================#        +------------+                 #=======#  |
                                         |                                       |
                                         | continue                              |
                                         v                                       |
             #================#        +------------+                            |
             H Receiver Start H        |  Send ACK  | ---------------------------+
             #================#        +------------+
               |
               |
               v
             +----------------+  eof   +------------+                 +-------+     #=====#
          +> |    Receive     | -----> | set finish | --------------> | await | --> H END H
          |  +----------------+        +------------+                 +-------+     #=====#
          |    |
          |    | success
          |    v
          |  +----------------+
          +- |     await      |
             +----------------+
    */

    private inner class ReceiveThread(private val channel: ChannelClient.Channel) : Thread() {
        var isFinished = false
            private set(value) {
                assert(value) // cannot reset to false
                field = value
            }

        private fun saveToGallery(sendItem: SendItem, tmpFile: File) {
            runBlocking {
                tmpFile.inputStream().use { ins ->
                    imageRepo.saveImage(
                        context,
                        sendItem.image.name,
                        takenTime = sendItem.image.takenTime,
                        ins = ins,
                        folderName = sendItem.folder
                    )
                }
            }
        }

        private val buffer = ByteArray(1024 * 4)

        private fun receive(ins: InputStream) {
            while (true) {
                if (ins.read(buffer, 0, 4) == -1) {
                    // all done
                    isFinished = true
                    receiveBarrier.await() // let ACK thread go ahead
                    break
                }
                val headerLen = buffer.toInt()
                assert(headerLen <= buffer.size) { "Header is too large to buffer." }
                var len = ins.read(buffer, 0, headerLen)
                assert(len == headerLen) { "Header length error." }
                val entry =
                    moshi.adapter(SendItem::class.java).fromJson(String(buffer, 0, headerLen))!!
                // receive file
                var fileLen = entry.image.size
                assert(fileLen > 0) { "File length error ($fileLen)." }
                val tmpFile = File(context.cacheDir, CACHE_TMP_FILE)
                if (tmpFile.exists()) tmpFile.delete()
                FileOutputStream(tmpFile).use { ous ->
                    while (true) {
                        len = ins.read(buffer, 0, minOf(fileLen.toInt(), buffer.size))
                        fileLen -= len
                        assert(len > 0)
                        ous.write(buffer, 0, len)
                        if (fileLen == 0L) {
                            break
                        }
                    }
                }
                Timber.tag(TAG).v("ReceiveThread: Receive finish: %s", entry.image.name)
                saveToGallery(entry, tmpFile)
                tmpFile.delete()
                Timber.tag(TAG).v("ReceiveThread: save finish: %s", entry.image.name)
                receiveBarrier.await()
            }
        }

        override fun run() {
            Tasks.await(channelClient.getInputStream(channel))?.use { ins ->
                try {
                    receive(ins)
                } catch (e: JsonDataException) {
                    Timber.tag(TAG).w(e, "ReceiveThread: Failed to parse entry json.")
                    receiveBarrier.reset() // free ack thread
                } catch (e: AssertionError) {
                    Timber.tag(TAG).w(e, "ReceiveThread: Assertion error.")
                    receiveBarrier.reset()
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "ReceiveThread: Unknown error.")
                    receiveBarrier.reset()
                }
            } ?: kotlin.run {
                Timber.tag(TAG).w("ReceiveThread: Failed to get stream")
                return
            }
            isFinished = true
            Timber.tag(TAG).i("ReceiveThread: exit")
        }
    }

    private inner class AckThread(
        private val channel: ChannelClient.Channel,
        private val receiveThread: ReceiveThread,
    ) : Thread() {
        override fun run() {
            Tasks.await(channelClient.getOutputStream(channel))?.use { ins ->
                try {
                    while (!isInterrupted) {
                        receiveBarrier.await()
                        if (receiveThread.isFinished) {
                            Timber.tag(TAG).d("AckThread: SendThread report finished, exit.")
                            break
                        }
                        ins.write(1)
                        ins.flush()
                    }
                } catch (e: InterruptedException) {
                    Timber.tag(TAG).w(e, "AckThread: Interrupted while await.")
                    receiveBarrier.reset()
                } catch (e: BrokenBarrierException) {
                    Timber.tag(TAG).w(e, "AckThread: Barrier broken.")
                    receiveBarrier.reset()
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "AckThread: Unknown exception.")
                    receiveBarrier.reset()
                }
            }
            Timber.tag(TAG).i("AckThread: exit")
        }
    }
}
