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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.service.UpgradeService
import cc.chenhe.weargallery.ui.UpgradingAty.Companion.startIfNecessary
import cc.chenhe.weargallery.uilts.*

private const val TAG = "UpgradingAty"

/**
 * An Activity to show the progress of upgrading and data migration. In most cases you should use [startIfNecessary]
 * to start to avoid additional overhead.
 *
 * This activity starts [UpgradeService] in [onCreate] method automatically and finish once a local broadcast with
 * action [ACTION_APPLICATION_UPGRADE_COMPLETE] is received.
 */
class UpgradingAty : AppCompatActivity() {

    companion object {

        /**
         * Start the activity only if the version has been change and [UpgradeService.shouldPerformUpgrade] returns
         * `true`.
         *
         * If there's no need to perform data migration, this function will update the last start version.
         *
         * @param before A callback that will be invoked before start [UpgradingAty].
         *
         * @return Whether the activity was started.
         */
        fun startIfNecessary(fragment: Fragment, requestCode: Int, before: (() -> Unit)?): Boolean {
            val old = lastStartVersion(fragment.requireContext())
            val new = getVersionCode(fragment.requireContext())
            if (old == new) {
                logd(TAG, "Last start version equals to the current, no need to upgrade.")
                return false
            } else if (old > new) {
                // should never happen
                loge(TAG, "Current version is lower than old version! current=$new, old=$old")
                return false
            }
            return if (UpgradeService.shouldPerformUpgrade(old, new)) {
                before?.invoke()
                fragment.startActivityForResult(
                        Intent(fragment.requireContext(), UpgradingAty::class.java), requestCode)
                true
            } else {
                logi(TAG, "No need to perform data migration, update version code directly.")
                lastStartVersion(fragment.requireContext(), new)
                false
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_APPLICATION_UPGRADE_COMPLETE) {
                LocalBroadcastManager.getInstance(this@UpgradingAty).unregisterReceiver(this)
                onUpgradeComplete()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.aty_upgrading)

        // register local receiver
        val filter = IntentFilter(ACTION_APPLICATION_UPGRADE_COMPLETE)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        // start upgrade service
        UpgradeService.start(this, lastStartVersion(this))
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun onUpgradeComplete() {
        logd(TAG, "Upgrade complete, finish activity.")
        setResult(Activity.RESULT_OK)
        finish()
    }

}