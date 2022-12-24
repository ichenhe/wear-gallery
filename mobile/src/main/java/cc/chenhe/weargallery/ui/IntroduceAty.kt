/*
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
import android.os.Build
import android.os.Bundle
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.utils.checkStoragePermissions
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide

class IntroduceAty : IntroActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isButtonBackVisible = false
        if (Build.VERSION.SDK_INT >= 23 && !checkStoragePermissions(this)) {
            val permissions = if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            addSlide(
                SimpleSlide.Builder()
                    .title(R.string.intro_permission_title)
                    .description(R.string.intro_permission_content)
                    .background(R.color.slide_first)
                    .backgroundDark(R.color.slide_first_dark)
                    .permissions(permissions)
                    .build()
            )
        }
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_install_title)
                .description(R.string.intro_install_content)
                .background(R.color.slide_second)
                .backgroundDark(R.color.slide_second_dark)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_auto_run_title)
                .description(R.string.intro_auto_run_content)
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
}