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

package cc.chenhe.weargallery.ui.pick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import cc.chenhe.weargallery.common.bean.Image

/**
 * An activity that lets users choose local picture. The default result is [Activity.RESULT_CANCELED]. If the user
 * selects a picture then the result is [Activity.RESULT_OK] and the uri of the selected picture will be saved in the
 * result intent, you should use [Intent.getData] to get it.
 */
class PickImageAty : AppCompatActivity() {

    private lateinit var fragmentContainerView: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        fragmentContainerView = FragmentContainerView(this).apply {
            id = android.R.id.widget_frame
        }
        val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(fragmentContainerView, lp)

        val fragment = PickImageFr().also {
            it.onPickListener = object : PickImageFr.OnPickListener {
                override fun onPick(image: Image) {
                    val r = Intent().apply {
                        data = image.uri
                    }
                    setResult(Activity.RESULT_OK, r)
                    finish()
                }
            }
        }

        supportFragmentManager.beginTransaction()
                .add(fragmentContainerView.id, fragment)
                .commit()
    }
}