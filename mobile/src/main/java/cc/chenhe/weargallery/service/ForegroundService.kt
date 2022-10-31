package cc.chenhe.weargallery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.ui.main.MainAty
import cc.chenhe.weargallery.utils.NOTIFY_CHANNEL_ID_FOREGROUND_SERVICE
import cc.chenhe.weargallery.utils.NOTIFY_ID_FOREGROUND_SERVICE
import timber.log.Timber

class ForegroundService : Service() {
    companion object {
        private const val TAG = "ForegroundService"
        private const val ACTION_STOP = "ACTION_STOP_FOREGROUND_SERVICE"

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

    private var receiver: BroadcastReceiver? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("Service start")
        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_STOP)
                        stopSelf()
                }
            }.also {
                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(it, IntentFilter(ACTION_STOP))
            }
        }
        setForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Service destroy")
        super.onDestroy()
    }

    private fun setForeground() {
        registerNotificationChannel(this)

        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainAty::class.java),
            PendingIntent.FLAG_MUTABLE
        )
        val notification =
            NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_FOREGROUND_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notify_foreground_text))
                .setSmallIcon(R.drawable.ic_notify_permission)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(intent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        startForeground(NOTIFY_ID_FOREGROUND_SERVICE, notification)
    }

    private fun registerNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFY_CHANNEL_ID_FOREGROUND_SERVICE,
                getString(R.string.notify_channel_foreground_name),
                NotificationManager.IMPORTANCE_LOW
            )
            context.getSystemService(NotificationManager::class.java)!!
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}