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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.HUA_WEI
import cc.chenhe.weargallery.common.util.checkHuaWei
import cc.chenhe.weargallery.common.util.xlogAppenderFlushSafely
import cc.chenhe.weargallery.databinding.AtyMainBinding
import cc.chenhe.weargallery.service.AppUpgradeService
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainAty : AppCompatActivity() {

    private lateinit var binding: AtyMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (checkHuaWei()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_hw_title)
                .setMessage(R.string.app_hw_message)
                .setPositiveButton(R.string.app_hw_view) { _, _ ->
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse(HUA_WEI)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton(R.string.app_hw_exit) { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
            return
        }
        if (AppUpgradeService.shouldRunUpgrade(this) && !AppUpgradeService.isRunning())
            ContextCompat.startForegroundService(this, Intent(this, AppUpgradeService::class.java))
        init()
    }

    private fun init() {
        binding = AtyMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        xlogAppenderFlushSafely()
    }
}