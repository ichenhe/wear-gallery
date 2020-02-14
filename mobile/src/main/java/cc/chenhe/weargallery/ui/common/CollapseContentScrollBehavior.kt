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

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import cc.chenhe.weargallery.R
import java.lang.ref.WeakReference

private const val DURATION = 600

/**
 * Behavior for scrollable view which usually is the container of main content.
 *
 * @see [CollapseHeaderScrollBehavior]
 */
class CollapseContentScrollBehavior(context: Context, attrs: AttributeSet?) :
        CoordinatorLayout.Behavior<View>(context, attrs) {

    private var headerHeight = context.resources.getDimensionPixelSize(R.dimen.title_block_height)
    private var headerHeightOffset = 0
    private var scroller: OverScroller? = null

    private var isFullyShown: Boolean = false

    private var childRef: WeakReference<View>? = null
    private val scrollRunnable = object : Runnable {
        override fun run() {
            scroller?.let { scroller ->
                childRef?.get()?.let { view ->
                    if (scroller.computeScrollOffset()) {
                        view.translationY = scroller.currY.toFloat()
                        ViewCompat.postOnAnimation(view, this)
                    } else {
                        updateState()
                    }
                } ?: scroller.abortAnimation()
            }
        }
    }

    init {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.CollapseContentScrollBehavior_Layout)
            isFullyShown = ta.getBoolean(R.styleable.CollapseContentScrollBehavior_Layout_fullyShown, isFullyShown)
            ta.recycle()
        }
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        childRef = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        childRef = null
        scroller?.abortAnimation()
        scroller = null
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: View): Parcelable? {
        val superState = super.onSaveInstanceState(parent, child)
        return SavedState(superState, isFullyShown)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: View, state: Parcelable) {
        super.onRestoreInstanceState(parent, child, state)
        if (state is SavedState) {
            setFullyShown(state.fullyShown, false)
        }
    }

    override fun onMeasureChild(parent: CoordinatorLayout, child: View, parentWidthMeasureSpec: Int,
                                widthUsed: Int, parentHeightMeasureSpec: Int, heightUsed: Int): Boolean {
        val header = findHeader(parent)
        header?.height?.let {
            // A workaround with zero height in the initial state which result in a flash.
            if (it != 0) headerHeight = it
        }
        headerHeightOffset = headerHeight - (header?.getToolbarHeight() ?: 0)

        val childLpHeight = child.layoutParams.height
        if (childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT
                || childLpHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
            if (header != null) {
                val availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec)
                val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        availableHeight - header.getToolbarHeight(),
                        if (childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT) View.MeasureSpec.EXACTLY
                        else View.MeasureSpec.AT_MOST)

                // Now measure the scrolling view with the correct height
                parent.onMeasureChild(child, parentWidthMeasureSpec, widthUsed, heightMeasureSpec, heightUsed)
                return true
            }
        }
        return false
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: View, layoutDirection: Int): Boolean {
        if (childRef == null) {
            childRef = WeakReference(child)
        }
        parent.onLayoutChild(child, layoutDirection)
        ViewCompat.offsetTopAndBottom(child, headerHeight)
        if (child.translationY <= getTransY(true) || child.translationY >= getTransY(false)) {
            /**
             * workaround: Drag selection will trigger `onLayoutChild` and we can't accurately determine whether
             * it is scrolling (see [DragSelectTouchListener.scrollBy]). So let's Let's judge the state by location
             * to avoid interrupting scrolling.
             */
            child.translationY = getTransY(isFullyShown)
        }
        return true
    }

    private fun getTransY(fullyShown: Boolean): Float {
        return if (fullyShown) -headerHeightOffset.toFloat() else 0f
    }

    private fun findHeader(parent: CoordinatorLayout): CollapseHeaderLayout? {
        parent.children.forEach { child ->
            if (child is CollapseHeaderLayout) {
                return child
            }
        }
        return null
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View,
                                     target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dx: Int, dy: Int,
                                   consumed: IntArray, type: Int) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        stopAutoScroll()
        if (dy > 0) {
            // scroll up
            val newTransY = child.translationY - dy
            if (newTransY >= -headerHeightOffset) {
                consumed[1] = dy
                child.translationY = newTransY
            } else {
                consumed[1] = headerHeightOffset + child.translationY.toInt()
                child.translationY = -headerHeightOffset.toFloat()
            }
        }
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dxConsumed: Int,
                                dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                type, consumed)
        stopAutoScroll()
        if (dyUnconsumed < 0) {
            // scroll down, and RecyclerView has reached the top
            val newTransY = child.translationY - dyUnconsumed
            if (newTransY <= 0) {
                child.translationY = newTransY
            } else {
                child.translationY = 0f
            }
        }
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, type: Int) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        if (child.translationY >= 0f || child.translationY <= -headerHeightOffset) {
            updateState()
            return
        }
        if (child.translationY <= -headerHeightOffset * 0.5f) {
            stopAutoScroll()
            startAutoScroll(child.translationY.toInt(), -headerHeightOffset)
        } else {
            stopAutoScroll()
            startAutoScroll(child.translationY.toInt(), 0)
        }
    }

    private fun startAutoScroll(current: Int, target: Int) {
        childRef?.get()?.let { view ->
            if (scroller == null) {
                scroller = OverScroller(view.context)
            }
            if (scroller!!.isFinished) {
                view.removeCallbacks(scrollRunnable)
                scroller!!.startScroll(0, current, 0, target - current, DURATION)
                ViewCompat.postOnAnimation(view, scrollRunnable)
            }
        }
    }

    private fun stopAutoScroll() {
        scroller?.let {
            if (!it.isFinished) {
                it.abortAnimation()
                childRef?.get()?.removeCallbacks(scrollRunnable)
            }
        }
    }

    private fun updateState() {
        isFullyShown = (childRef?.get()?.translationY ?: 0f) < 0
    }

    private class SavedState : View.BaseSavedState {
        var fullyShown: Boolean = false

        constructor(superState: Parcelable?, fullyShown: Boolean) : super(superState) {
            this.fullyShown = fullyShown
        }

        constructor(source: Parcel) : super(source) {
            this.fullyShown = source.readByte() != 0.toByte()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            // writeByte() will cause 'No virtual method' exception when open Recents Screen on API 28
            dest.writeByte(if (fullyShown) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    // ---------------------------------------------------------------
    // API
    // ---------------------------------------------------------------

    fun setFullyShown(fullyShown: Boolean, animation: Boolean) {
        this.isFullyShown = fullyShown
        childRef?.get()?.let { view ->
            if (animation) {
                startAutoScroll(view.translationY.toInt(), getTransY(fullyShown).toInt())
            } else {
                view.translationY = getTransY(fullyShown)
            }
        }
    }
}