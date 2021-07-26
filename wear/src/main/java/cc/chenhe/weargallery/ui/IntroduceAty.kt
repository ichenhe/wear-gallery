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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cc.chenhe.weargallery.R
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import me.chenhe.wearvision.dialog.AlertDialog

const val REQUEST_PERMISSION = 1

class IntroduceAty : IntroActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isButtonBackVisible = false
        isPagerIndicatorVisible = false

        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        for (item in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    item
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, REQUEST_PERMISSION)
                    break
                }
            }
        }

        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_gif_title)
                .description(R.string.intro_gif_content)
                .background(R.color.slide_first)
                .backgroundDark(R.color.slide_first_dark)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_lan_title)
                .description(R.string.intro_lan_content)
                .background(R.color.slide_second)
                .backgroundDark(R.color.slide_second_dark)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_wf_title)
                .description(R.string.intro_wf_content)
                .background(R.color.slide_third)
                .backgroundDark(R.color.slide_third_dark)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_os_title)
                .description(R.string.intro_os_content)
                .background(R.color.slide_fourth)
                .backgroundDark(R.color.slide_fourth_dark)
                .build()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSION) {
            return
        }

        var allGrant = true
        for (item in grantResults) {
            if (item != PackageManager.PERMISSION_GRANTED) {
                allGrant = false
                break
            }
        }

        if (!allGrant) {
            AlertDialog(this).apply {
                setTitle(R.string.local_err_permission_title)
                setMessage(R.string.local_err_permission_content)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { _, _ ->
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this@IntroduceAty,
                            permissions[0]
                        )
                    ) {
                        // never show again
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(this)
                        }
                        finish()
                    } else {
                        ActivityCompat.requestPermissions(
                            this@IntroduceAty,
                            permissions,
                            REQUEST_PERMISSION
                        )
                    }
                }
                setNegativeButtonIcon(R.drawable.ic_dialog_close) { _, _ ->
                    finish()
                }
                setCancelable(false)
            }.show()
        }
    }
}