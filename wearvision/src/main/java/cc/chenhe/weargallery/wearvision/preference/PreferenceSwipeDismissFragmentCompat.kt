/*
 * Copyright (c) 2020 Chenhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION") // For compatibility with ticwear (API 21)

package cc.chenhe.weargallery.wearvision.preference

import android.os.Bundle
import android.support.wearable.view.SwipeDismissFrameLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * A subclass of [PreferenceFragmentCompat] that wrapper it's view with a [SwipeDismissFrameLayout] to implement swipe
 * dismiss feature.
 *
 * [onDismissed] will be called when the swipe dismiss is triggered. Subclass should back to the previous view. e.g.
 * `findNavController().navigateUp()`.
 */
abstract class PreferenceSwipeDismissFragmentCompat : PreferenceFragmentCompat() {

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
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

    abstract fun onDismissed()
}