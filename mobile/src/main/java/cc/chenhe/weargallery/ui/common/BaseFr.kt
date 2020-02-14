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

package cc.chenhe.weargallery.ui.common

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import cc.chenhe.weargallery.ui.main.MainAty

/**
 * A base fragment with some convenience methods.
 */
abstract class BaseFr : Fragment() {

    private var sysUiVisibilityListener: ((visibility: Int) -> Unit)? = null

    fun setStatusBarColor(color: Int) {
        requireActivity().window.statusBarColor = color
    }

    fun resetStatusBarColor() {
        (requireActivity() as MainAty).resetStateBarColor()
    }

    /**
     * Set a [android.view.View.OnSystemUiVisibilityChangeListener] in the form of lambda.
     * The [listener] will be removed automatically in [onDestroyView].
     */
    fun setOnSystemUiVisibilityChangeListener(listener: (visibility: Int) -> Unit) {
        sysUiVisibilityListener = listener
        requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener(listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (sysUiVisibilityListener != null) {
            requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener(null)
            sysUiVisibilityListener = null
        }
    }
}

fun Fragment.requireCompatAty() = requireActivity() as AppCompatActivity

fun Fragment.setupToolbar(toolbar: Toolbar) {
    requireCompatAty().apply {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
    }
}