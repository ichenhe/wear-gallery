package cc.chenhe.weargallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import cc.chenhe.weargallery.service.UpgradeService
import cc.chenhe.weargallery.uilts.logi

class AppUpgradeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "Receiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
            return
        logi(TAG, "Receive MY_PACKAGE_REPLACED broadcast.")
        if (UpgradeService.isRunning()) {
            logi(TAG, "Upgrade service is running.")
        } else {
            logi(TAG, "Start upgrade service.")
            ContextCompat
                .startForegroundService(context, Intent(context, UpgradeService::class.java))
        }

    }
}