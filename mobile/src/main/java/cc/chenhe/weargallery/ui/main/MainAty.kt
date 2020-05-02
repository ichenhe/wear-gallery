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
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.HUA_WEI
import cc.chenhe.weargallery.common.util.checkHuaWei
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainAty : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetStateBarColor()

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
        } else {
            setContentView(R.layout.aty_main)
        }
    }

    private var darkBackgroundColor = -1
    private var backgroundColor = -1

    /**
     * Set systemUiVisibility to VISIBLE.
     *
     * **Notice:** System status bar text color will change to default at the same time. Call [resetStateBarColor] to
     * reset it.
     */
    fun resetSystemUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    /**
     * Reset to default system state bar color along with state bar text color.
     *
     * Use dark style if in dark mode or API level less than [Build.VERSION_CODES.M], white otherwise.
     */
    fun resetStateBarColor() {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (mode == Configuration.UI_MODE_NIGHT_YES) {
            if (darkBackgroundColor == -1) {
                val typedArray = obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
                darkBackgroundColor = typedArray.getColor(0, Color.BLACK)
                typedArray.recycle()
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = darkBackgroundColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility
                        and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv())
            }
        } else {
            if (backgroundColor == -1) {
                val typedArray = obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
                backgroundColor = typedArray.getColor(0, Color.WHITE)
                typedArray.recycle()
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            window.statusBarColor = backgroundColor
        }
    }

    fun isSystemUIVisible() = (window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0

}