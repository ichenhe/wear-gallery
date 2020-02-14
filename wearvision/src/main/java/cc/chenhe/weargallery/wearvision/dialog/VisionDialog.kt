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

@file:Suppress("DEPRECATION") // SwipeDismissFrameLayout

package cc.chenhe.weargallery.wearvision.dialog

import android.app.Dialog
import android.content.Context
import android.support.wearable.view.SwipeDismissFrameLayout
import android.view.View
import android.view.ViewGroup
import cc.chenhe.weargallery.wearvision.R

/**
 * Base class for Wear Vision dialogs. This class wrap the dialog content view with a [SwipeDismissFrameLayout] so that
 * we can programmatically control whether swipe dismiss is enabled. The `android:windowSwipeToDismiss` attribute
 * should be set to `false` in dialog's theme.
 */
abstract class VisionDialog(context: Context, themeResId: Int) : Dialog(context, themeResId) {

    private lateinit var mSwipeDismissLayout: SwipeDismissFrameLayout

    private var mCancelable = true
        set(value) {
            field = value
            if (::mSwipeDismissLayout.isInitialized) {
                mSwipeDismissLayout.isDismissEnabled = value
            }
        }

    override fun setContentView(view: View) {
        super.setContentView(wrapSwipeDismissLayout(view))
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(wrapSwipeDismissLayout(layoutInflater.inflate(layoutResID, null)))
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        super.setContentView(wrapSwipeDismissLayout(view), params)
    }

    private fun wrapSwipeDismissLayout(view: View): SwipeDismissFrameLayout {
        return SwipeDismissFrameLayout(context).apply {
            addCallback(object : SwipeDismissFrameLayout.Callback() {
                override fun onSwipeStart() {
                    super.onSwipeStart()
                    // A workaround: call this function in `onDismissed` is invalid.
                    window?.setWindowAnimations(R.style.Animation_WearVision_Dialog_NonSwipe)
                }

                override fun onSwipeCancelled() {
                    super.onSwipeCancelled()
                    // Reset animations if swipe is canceled.
                    window?.setWindowAnimations(R.style.Animation_WearVision_Dialog)
                }

                override fun onDismissed(layout: SwipeDismissFrameLayout?) {
                    cancel()
                }
            })
            addView(view)
            isDismissEnabled = mCancelable
            mSwipeDismissLayout = this
        }
    }

    override fun setCancelable(flag: Boolean) {
        super.setCancelable(flag)
        mCancelable = flag
    }
}