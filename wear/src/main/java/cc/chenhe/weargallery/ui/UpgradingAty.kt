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

package cc.chenhe.weargallery.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.common.util.xlogAppenderFlushSafely
import cc.chenhe.weargallery.service.UpgradeService
import cc.chenhe.weargallery.ui.UpgradingAty.Companion.startIfNecessary
import cc.chenhe.weargallery.uilts.ACTION_APPLICATION_UPGRADE_COMPLETE
import cc.chenhe.weargallery.uilts.lastStartVersion
import timber.log.Timber

/**
 * An Activity to show the progress of upgrading and data migration. In most cases you should use [startIfNecessary]
 * to start to avoid additional overhead.
 *
 * This activity starts [UpgradeService] in [onCreate] method automatically and finish once a local broadcast with
 * action [ACTION_APPLICATION_UPGRADE_COMPLETE] is received.
 */
class UpgradingAty : AppCompatActivity() {

    companion object {
        private const val TAG = "UpgradingAty"

        /** bool. Whether the [UpgradeService] is running before the activity start. */
        private const val EXTRA_RUNNING = "running"

        /**
         * Start the activity only if [UpgradeService.shouldPerformUpgrade] returns `true` or the
         * service itself is running.
         *
         * This activity will start [UpgradeService] automatically.
         *
         * @param launcher The launcher returned by calling [registerForActivityResult] with a [UpgradeContract].
         * @return Whether the activity was started.
         */
        internal fun startIfNecessary(
            ctx: Context,
            launcher: ActivityResultLauncher<Boolean>
        ): Boolean {
            val old = lastStartVersion(ctx)
            val new = getVersionCode(ctx)
            val upgradeServiceRunning = UpgradeService.isRunning()
            return if (upgradeServiceRunning || UpgradeService.shouldPerformUpgrade(old, new)) {
                launcher.launch(upgradeServiceRunning)
                true
            } else {
                Timber.tag(TAG).d("No need to perform data migration.")
                false
            }
        }
    }

    internal class UpgradeContract : ActivityResultContract<Boolean, Boolean>() {
        override fun createIntent(context: Context, upgradeServiceRunning: Boolean): Intent {
            return Intent(context, UpgradingAty::class.java).apply {
                putExtra(EXTRA_RUNNING, upgradeServiceRunning)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }

    private var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.aty_upgrading)

        // register local receiver
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == ACTION_APPLICATION_UPGRADE_COMPLETE) {
                    Timber.tag(TAG).i("Receive upgrade complete signal, finish activity.")
                    onUpgradeComplete()
                }
            }
        }
        val filter = IntentFilter(ACTION_APPLICATION_UPGRADE_COMPLETE)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver!!, filter)

        if (intent.getBooleanExtra(EXTRA_RUNNING, false)) {
            if (!UpgradeService.isRunning()) {
                // Since the service is running before activity start, but it has stopped now.
                // The only reason is the upgrade process has completed before activity fully start.
                // So we just trigger a complete signal.
                Timber.tag(TAG).i("Upgrade service has completed, finish directly.")
                onUpgradeComplete()
            }
            // Otherwise, we should wait for the broadcast signal of service completion.
            Timber.tag(TAG).i("Upgrade service is running, just wait.")
        } else {
            // Start upgrade service and wait for the broadcast.
            Timber.tag(TAG).i("Start upgrade service and wait.")
            UpgradeService.start(this, lastStartVersion(this))
        }
    }

    private fun onUpgradeComplete() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        receiver?.also {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
            receiver = null
        }
        xlogAppenderFlushSafely()
        super.onDestroy()
    }

}