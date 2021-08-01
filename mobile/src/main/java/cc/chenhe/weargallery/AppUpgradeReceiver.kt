package cc.chenhe.weargallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import cc.chenhe.weargallery.service.AppUpgradeService
import timber.log.Timber

class AppUpgradeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Receiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
            return
        Timber.tag(TAG).i("Receive MY_PACKAGE_REPLACED broadcast.")
        if (AppUpgradeService.isRunning()) {
            Timber.tag(TAG).i("Upgrade service is running.")
        } else {
            Timber.tag(TAG).i("Start upgrade service.")
            ContextCompat
                .startForegroundService(context, Intent(context, AppUpgradeService::class.java))
        }

    }
}