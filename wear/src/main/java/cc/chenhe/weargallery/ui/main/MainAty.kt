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

package cc.chenhe.weargallery.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.HUA_WEI
import cc.chenhe.weargallery.common.util.checkHuaWei
import cc.chenhe.weargallery.common.util.xlogAppenderFlushSafely
import cc.chenhe.weargallery.db.RemoteImageDao
import cc.chenhe.weargallery.ui.IntroduceAty
import cc.chenhe.weargallery.ui.UpgradingAty
import cc.chenhe.weargallery.uilts.NOTIFY_ID_PERMISSION
import cc.chenhe.weargallery.uilts.addQrCode
import cc.chenhe.weargallery.uilts.showHuawei
import kotlinx.coroutines.launch
import me.chenhe.wearvision.dialog.AlertDialog
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainAty : AppCompatActivity() {
    companion object {
        private const val TAG = "MainAty"
    }

    private val sharedViewModel: SharedViewModel by viewModel()
    private val remoteImageDao: RemoteImageDao by inject()

    private val introduceLauncher =
        registerForActivityResult(object : ActivityResultContract<Unit, Unit>() {
            override fun createIntent(context: Context, input: Unit?): Intent {
                return Intent(context, IntroduceAty::class.java)
            }

            override fun parseResult(resultCode: Int, intent: Intent?) {
            }
        }) {
            if (!hasPermission()) {
                finish()
                return@registerForActivityResult
            }
            NotificationManagerCompat.from(this).cancel(NOTIFY_ID_PERMISSION)
            checkUpgradeProcess()
        }

    private val upgradeAtyLauncher =
        registerForActivityResult(UpgradingAty.UpgradeContract()) { ok ->
            if (ok)
                init()
            else
                finish()
        }

    private var pendingUris: Collection<Uri>? = null
    private val deleteRequestLauncher = registerForActivityResult(StartIntentSenderForResult()) {
        if (it.resultCode != RESULT_OK) {
            Timber.tag(TAG).d("${pendingUris?.size ?: 0} Image deletion request is rejected.")
            return@registerForActivityResult
        }
        Timber.tag(TAG)
            .d("Image deletion is approved, try to clear ${pendingUris?.size ?: 0}$ fields.")
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            pendingUris?.also { uris -> remoteImageDao.clearLocalUri(uris) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1 Check Huawei
        if (checkHuaWei() && showHuawei(this)) {
            AlertDialog(this).apply {
                setTitle(R.string.app_hw_title)
                setMessage(R.string.app_hw_message)
                skipText = getString(R.string.dont_show_again)
                showSkipLayout = true
                addQrCode(HUA_WEI)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { _, _ ->
                    showHuawei(this@MainAty, !isSkipChecked)
                }
                setOnDismissListener {
                    checkPermission()
                }
            }.show()
            return
        }
        checkPermission()
    }

    // 2 Check permission
    private fun checkPermission() {
        if (hasPermission()) {
            checkUpgradeProcess()
        } else {
            introduceLauncher.launch(Unit)
        }
    }

    // 3 Check upgrade process
    private fun checkUpgradeProcess() {
        if (UpgradingAty.startIfNecessary(this, upgradeAtyLauncher))
            return
        init()
    }

    // 4 init
    private fun init() {
        setContentView(R.layout.aty_main)

        sharedViewModel.deleteRequestEvent.observe(this) { pending ->
            if (pending != null) {
                pendingUris = pending.uris
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(pending.intentSender).build()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        xlogAppenderFlushSafely()
    }

    /**
     * @return Whether has permissions.
     */
    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED)
    }

}