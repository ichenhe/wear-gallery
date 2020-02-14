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


package cc.chenhe.weargallery.ui;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.utils.UtilsKt;

public class IntroduceAty extends IntroActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setButtonBackVisible(false);

        if (Build.VERSION.SDK_INT >= 23 && !UtilsKt.checkStoragePermissions(this)) {
            String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            addSlide(new SimpleSlide.Builder()
                    .title(R.string.intro_permission_title)
                    .description(R.string.intro_permission_content)
                    .background(R.color.slide_first)
                    .backgroundDark(R.color.slide_first_dark)
                    .permissions(permissions)
                    .build());
        }

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_install_title)
                .description(R.string.intro_install_content)
                .background(R.color.slide_second)
                .backgroundDark(R.color.slide_second_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_auto_run_title)
                .description(R.string.intro_auto_run_content)
                .background(R.color.slide_third)
                .backgroundDark(R.color.slide_third_dark)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title(R.string.intro_os_title)
                .description(R.string.intro_os_content)
                .background(R.color.slide_fourth)
                .backgroundDark(R.color.slide_fourth_dark)
                .build());
    }

}
