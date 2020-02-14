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

package cc.chenhe.weargallery.wearvision.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.wearvision.widget.ObservableScrollView.OnScrollStateChangedListener

/**
 * A [NestedScrollView] can set a [OnScrollStateChangedListener].
 */
class ObservableScrollView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    companion object {
        const val SCROLL_STATE_IDLE = RecyclerView.SCROLL_STATE_IDLE
        const val SCROLL_STATE_DRAGGING = RecyclerView.SCROLL_STATE_DRAGGING
        const val SCROLL_STATE_SETTLING = RecyclerView.SCROLL_STATE_SETTLING
    }

    /**
     * Current scroll state. The value is one of `SCROLL_STATE_*`.
     * @param
     */
    var scrollState: Int = RecyclerView.SCROLL_STATE_IDLE
        private set
    var onScrollStateChangedListener: OnScrollStateChangedListener? = null

    override fun stopNestedScroll(type: Int) {
        super.stopNestedScroll(type)
        if ((type == ViewCompat.TYPE_TOUCH && scrollState == SCROLL_STATE_DRAGGING) ||
                (type == ViewCompat.TYPE_NON_TOUCH && scrollState == SCROLL_STATE_SETTLING)) {
            dispatchScrollState(RecyclerView.SCROLL_STATE_IDLE)
        }
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return super.startNestedScroll(axes, type).apply {
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                dispatchScrollState(RecyclerView.SCROLL_STATE_SETTLING)
            }
        }
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        val r = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
        if (type == ViewCompat.TYPE_TOUCH && r && (consumed?.get(1) ?: 0) != 0) {
            dispatchScrollState(RecyclerView.SCROLL_STATE_DRAGGING)
        }
        return r
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int, consumed: IntArray) {
        super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
        if (type == ViewCompat.TYPE_TOUCH && (dyConsumed != 0 || consumed[1] != 0)) {
            dispatchScrollState(RecyclerView.SCROLL_STATE_DRAGGING)
        }
    }

    private fun dispatchScrollState(newState: Int) {
        if (scrollState != newState) {
            val old = scrollState
            scrollState = newState
            onScrollStateChangedListener?.onScrollStateChanged(this, old, newState)
        }
    }

    interface OnScrollStateChangedListener {

        /**
         * Callback method to be invoked when NestedScrollView's scroll state changes.
         *
         * @param scrollView The ObservableScrollView whose scroll state has changed.
         * @param oldState The original scroll state.
         * @param newState The updated scroll state. One of [SCROLL_STATE_IDLE], [SCROLL_STATE_DRAGGING] or
         * [SCROLL_STATE_SETTLING].
         */
        fun onScrollStateChanged(scrollView: ObservableScrollView, oldState: Int, newState: Int)
    }
}