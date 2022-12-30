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
import cc.chenhe.weargallery.repo.PreferenceRepo
import cc.chenhe.weargallery.utils.ACTION_APP_UPGRADE_COMPLETE
import cc.chenhe.weargallery.utils.NotificationUtils
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.CHANNEL_ID_IMPORTANT_PROCESSING
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.NOTIFY_ID_UPGRADING
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

@Suppress("FunctionName")
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
        fun shouldRunUpgrade(preferenceRepo: PreferenceRepo, context: Context): Boolean {
            return preferenceRepo.getLastStartVersionSync() != getVersionCode(context)
        }
    }

    private val preferenceRepo: PreferenceRepo by inject()

    fun ping(): Boolean = true

    private fun setForeground() {
        get<NotificationUtils>().registerNotificationChannel(CHANNEL_ID_IMPORTANT_PROCESSING)
        val title = getString(R.string.notify_upgrading)
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID_IMPORTANT_PROCESSING)
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

    private suspend fun doWork() {
        val success = try {
            doWorkInternal()
            preferenceRepo.setLastStartVersion(getVersionCode(this))
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

    private suspend fun doWorkInternal() {
        val lastStartVersion = preferenceRepo.getLastStartVersionSync() ?: 0
        Timber.tag(TAG).i("Upgrade process started: from $lastStartVersion")
        if (lastStartVersion <= 220603010) { // v6.3.1
            migrate_from_base()
        }
    }

    private suspend fun migrate_from_base() {
        Timber.tag(TAG).i("Upgrade migrate step: from base")
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (sp.contains("tip_with_watch")) {
            preferenceRepo.setTipOnWatchOperating(sp.getBoolean("tip_with_watch", false))
        }
        if (sp.contains("foreground_service")) {
            preferenceRepo.setKeepForegroundService(sp.getBoolean("foreground_service", false))
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            clear()
        }
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("Service destroy")
        super.onDestroy()
        instance = null
    }
}