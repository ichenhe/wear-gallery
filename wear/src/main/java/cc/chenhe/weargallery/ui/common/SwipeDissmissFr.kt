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

@file:Suppress("DEPRECATION") // SwipeDismissFrameLayout

package cc.chenhe.weargallery.ui.common

import android.os.Bundle
import android.support.wearable.view.SwipeDismissFrameLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding

/**
 * A [Fragment] that wrapper it's view with a [SwipeDismissFrameLayout] to implement swipe dismiss feature.
 * Subclass should implement [createView] to inflate the view instead of [onCreateView]. These two functions should be
 * considered equivalent.
 *
 * [onDismissed] will be called when the swipe dismiss is triggered. The default behavior is call `navigateUp()`.
 */
abstract class SwipeDismissFr : Fragment() {

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                    savedInstanceState: Bundle?): View? {
        val view = createView(inflater, container, savedInstanceState).root

        return SwipeDismissFrameLayout(requireContext()).apply {
            addCallback(SwipeCallback())
            addView(view)
        }
    }

    private inner class SwipeCallback : SwipeDismissFrameLayout.Callback() {
        override fun onDismissed(layout: SwipeDismissFrameLayout?) {
            super.onDismissed(layout)
            onDismissed()
        }
    }

    abstract fun createView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): ViewBinding

    open fun onDismissed() {
        findNavController().navigateUp()
    }
}