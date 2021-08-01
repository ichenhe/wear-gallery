package cc.chenhe.weargallery.service

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.utils.*
import kotlinx.coroutines.launch
import timber.log.Timber

class AppUpgradeService : LifecycleService() {

    companion object {
        private const val TAG = "AppUpgradeService"

        private var instance: AppUpgradeService? = null

        fun isRunning(): Boolean = try {
            instance?.ping() ?: false
        } catch (e: Exception) {
            false
        }

        /**
         * 判断是否应该执行数据迁移。
         */
        fun shouldRunUpgrade(context: Context): Boolean {
            return getLastStartVersion(context) != getVersionCode(context)
        }
    }


    fun ping(): Boolean = true

    private fun setForeground() {
        registerImportantPrecessingNotificationChannel(this)
        val title = getString(R.string.notify_upgrading)
        val notification =
            NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_IMPORTANT_PROCESSING)
                .setContentTitle(title)
                .setTicker(title)
                .setContentText(getString(R.string.notify_upgrading_content))
                .setSmallIcon(R.drawable.ic_notify_upgrade)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        startForeground(NOTIFY_ID_UPGRADING, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (instance != null) {
            Timber.tag(TAG).i("Service is running, ignore this start command. id=$startId")
            return START_NOT_STICKY
        }
        instance = this
        setForeground()
        lifecycleScope.launch {
            doWork()
        }
        return START_NOT_STICKY
    }

    private fun doWork() {
        val success = try {
            doWorkInternal()
            setLastStartVersion(this, getVersionCode(this))
            Timber.tag(TAG).i("Upgrade successful")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to upgrade.")
            false
        }
        val i = Intent(ACTION_APP_UPGRADE_COMPLETE).putExtra("success", success)
        LocalBroadcastManager.getInstance(this).sendBroadcast(i)
        instance = null
        stopSelf()
    }

    private fun doWorkInternal() {

        if (getLastStartVersion(this) <= 220600010) { // v6.0.1-pre
            // Delete GMS/MMS and preview compress preference
            PreferenceManager.getDefaultSharedPreferences(this).edit {
                remove("wear_mode")
                remove("preview_compress")
            }
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Service destroy")
        super.onDestroy()
        instance = null
    }
}