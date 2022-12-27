package cc.chenhe.weargallery.utils

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import cc.chenhe.weargallery.common.util.checkPermission

class NotificationChecker(context: Context) {
    private val ctx = context.applicationContext

    /**
     * Check if app has post notification runtime permission.
     * @return always true if API level < 33
     */
    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ctx.checkPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Check if overall notification is enabled.
     * @return always true if API level < 19
     */
    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /**
     * Check if notification channel is enabled.
     * @return always true if API level < 26
     */
    fun isNotificationChannelEnabled(channelId: String): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                NotificationManagerCompat.from(ctx)
                    .getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
}