package cc.chenhe.weargallery.utils

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import cc.chenhe.weargallery.R

class NotificationUtils(context: Context) {
    companion object {
        const val CHANNEL_ID_PERMISSION = "wg.permission"
        const val CHANNEL_ID_SENDING = "wg.send_images"
        const val CHANNEL_ID_SEND_RESULT = "wg.send_images_result"

        /** Use to display important foreground service */
        const val CHANNEL_ID_IMPORTANT_PROCESSING = "wg.important_processing"

        /** Use to prevent this application from being killed on some systems e.g. MiUI */
        const val CHANNEL_ID_FOREGROUND_SERVICE = "wg.foreground_service"


        const val NOTIFY_ID_PERMISSION = 1
        const val NOTIFY_ID_SENDING = 2
        const val NOTIFY_ID_SEND_RESULT = 3
        const val NOTIFY_ID_UPGRADING = 4

        /** See [CHANNEL_ID_FOREGROUND_SERVICE] */
        const val NOTIFY_ID_FOREGROUND_SERVICE = 5
    }

    private val ctx = context.applicationContext

    fun registerNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (channelId) {
                CHANNEL_ID_PERMISSION -> registerPermissionChannel()
                CHANNEL_ID_SENDING -> registerSendImageProgress()
                CHANNEL_ID_SEND_RESULT -> registerSendImageResult()
                CHANNEL_ID_IMPORTANT_PROCESSING -> registerImportantProcessing()
                CHANNEL_ID_FOREGROUND_SERVICE -> registerForegroundService()
            }
        }
    }

    private fun registerPermissionChannel() {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID_PERMISSION, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(ctx.getString(R.string.notify_channel_permission_name))
            .setDescription(ctx.getString(R.string.notify_channel_permission_description))
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }

    private fun registerSendImageProgress() {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID_SENDING, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(ctx.getString(R.string.notify_channel_sending_name))
            .setDescription(ctx.getString(R.string.notify_channel_sending_description))
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }

    private fun registerSendImageResult() {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID_SEND_RESULT, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(ctx.getString(R.string.notify_channel_send_result_name))
            .setDescription(ctx.getString(R.string.notify_channel_send_result_description))
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }

    private fun registerImportantProcessing() {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID_IMPORTANT_PROCESSING, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(ctx.getString(R.string.notify_channel_important_processing))
            .setDescription(ctx.getString(R.string.notify_channel_important_processing_description))
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }

    private fun registerForegroundService() {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID_FOREGROUND_SERVICE, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(ctx.getString(R.string.notify_channel_foreground_name))
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }
}
