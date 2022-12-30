package cc.chenhe.weargallery.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.repo.PreferenceRepo
import cc.chenhe.weargallery.ui.main.MainAty
import cc.chenhe.weargallery.utils.NotificationUtils
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.CHANNEL_ID_FOREGROUND_SERVICE
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

class ForegroundService : Service() {
    companion object {
        private const val TAG = "ForegroundService"
        private const val ACTION_STOP = "ACTION_STOP_FOREGROUND_SERVICE"

        /**
         * Disable foreground service in settings and stop it
         *
         * Only effective if [ForegroundService] is running.
         */
        private const val ACTION_DISABLE = "cc.chenhe.weargallery.ACTION_DISABLE_FOREGROUND_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context.applicationContext, ForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.applicationContext.startForegroundService(intent)
            } else {
                context.applicationContext.startService(intent)
            }
        }

        fun stop(context: Context) {
            LocalBroadcastManager.getInstance(context).sendBroadcastSync(Intent(ACTION_STOP))
        }
    }

    private val preferenceRepo: PreferenceRepo by inject()
    private var localReceiver: LocalReceiver? = null
    private var disableReceiver: DisableReceiver? = null

    private inner class LocalReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP)
                stopSelf()
        }
    }

    private inner class DisableReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DISABLE) {
                runBlocking { preferenceRepo.setKeepForegroundService(false) }
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("Service start")
        if (localReceiver == null) {
            localReceiver = LocalReceiver().also {
                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(it, IntentFilter(ACTION_STOP))
            }
        }
        if (disableReceiver == null) {
            disableReceiver = DisableReceiver().also {
                registerReceiver(it, IntentFilter(ACTION_DISABLE))
            }
        }
        setForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Service destroy")
        localReceiver?.also {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
            localReceiver = null
        }
        disableReceiver?.also {
            unregisterReceiver(it)
            disableReceiver = null
        }
        super.onDestroy()
    }

    private fun setForeground() {
        get<NotificationUtils>().registerNotificationChannel(CHANNEL_ID_FOREGROUND_SERVICE)

        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainAty::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val hideIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // open foreground notification channel settings
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID_FOREGROUND_SERVICE)
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }
        val disableIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            Intent(ACTION_DISABLE).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notify_foreground_text))
                .setSmallIcon(R.drawable.ic_notify_permission)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(intent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(
                    R.drawable.ic_notify_button_shutdown,
                    getString(R.string.notify_foreground_disable_button),
                    disableIntent
                )
        hideIntent?.also {
            notification.addAction(
                R.drawable.ic_notify_button_hide,
                getString(R.string.notify_foreground_hide_button),
                it
            )
        }
        startForeground(NotificationUtils.NOTIFY_ID_FOREGROUND_SERVICE, notification.build())
    }

    override fun onBind(intent: Intent?): IBinder? = null
}