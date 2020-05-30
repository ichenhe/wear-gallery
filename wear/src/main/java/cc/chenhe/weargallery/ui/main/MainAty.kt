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

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.HUA_WEI
import cc.chenhe.weargallery.common.util.checkHuaWei
import cc.chenhe.weargallery.uilts.addQrCode
import cc.chenhe.weargallery.uilts.showHuawei
import cc.chenhe.weargallery.wearvision.dialog.AlertDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
 */
private const val DELETE_PERMISSION_REQUEST = 1

class MainAty : AppCompatActivity() {

    private val sharedViewModel: SharedViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkHuaWei() && showHuawei(this)) {
            AlertDialog(this).apply {
                setTitle(R.string.app_hw_title)
                setMessage(R.string.app_hw_message)
                skipText = getString(R.string.dont_show_again)
                showSkipLayout = true
                addQrCode(HUA_WEI)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, DialogInterface.OnClickListener { _, _ ->
                    showHuawei(this@MainAty, !isSkipChecked)
                })
                setOnDismissListener {
                    init()
                }
            }.show()
        } else {
            init()
        }
    }

    private fun init() {
        setContentView(R.layout.aty_main)

        sharedViewModel.permissionNeededForDelete.observe(this) { intentSender ->
            intentSender?.let {
                startIntentSenderForResult(intentSender, DELETE_PERMISSION_REQUEST, null, 0, 0, 0, null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            sharedViewModel.deletePendingImage()
        }
    }
}