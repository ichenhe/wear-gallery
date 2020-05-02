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

package cc.chenhe.weargallery.ui.imagedetail

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.annotation.IntDef
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import java.lang.ref.WeakReference
import kotlin.math.abs

class ImageDetailOperationBehavior(context: Context?, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<View>(context, attrs) {

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(STATE_HIDDEN, STATE_EXPANDED)
        annotation class MenuState

        const val STATE_HIDDEN = 1
        const val STATE_EXPANDED = 2
        const val STATE_DRAGGING = 3
        const val STATE_SETTING = 4
    }

    private val touchSlop: Int
    private val minVelocity: Float
    private var parentWidth = 0

    @get: MenuState
    @setparam:MenuState
    private var state = STATE_HIDDEN
    private var childRef: WeakReference<View>? = null

    private var isBeingDragged = false
    private var lastMotionX = 0
    private var velocityTracker: VelocityTracker? = null
    private val scroller: OverScroller by lazy { OverScroller(context) }
    private val flingRunnable = object : Runnable {
        override fun run() {
            childRef?.get()?.let { view ->
                if (scroller.computeScrollOffset()) {
                    setLeft(view, scroller.currX)
                    ViewCompat.postOnAnimation(view, this)
                } else {
                    updateState(view)
                }
            } ?: scroller.abortAnimation()
        }

    }

    init {
        ViewConfiguration.get(context).also {
            touchSlop = it.scaledPagingTouchSlop
            minVelocity = it.scaledMinimumFlingVelocity.toFloat()
        }
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        childRef = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        childRef = null
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: View): Parcelable? {
        val superState = super.onSaveInstanceState(parent, child)
        return SavedState(superState, state)
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: View, state: Parcelable) {
        super.onRestoreInstanceState(parent, child, state)
        if (state is SavedState) {
            this.state = state.state
        }
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: View, layoutDirection: Int): Boolean {
        if (childRef == null) {
            childRef = WeakReference(child)
        }
        parentWidth = parent.width
        if (isIdle) {
            parent.onLayoutChild(child, layoutDirection)
            ViewCompat.offsetLeftAndRight(child, if (state == STATE_HIDDEN) parentWidth else 0)
        }
        return true
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: View, ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isBeingDragged = false
                lastMotionX = ev.x.toInt()
                parent.parent.requestDisallowInterceptTouchEvent(true)
                ensureVelocityTracker()
            }
            MotionEvent.ACTION_MOVE -> {
                val x = ev.x.toInt()
                val dx = x - lastMotionX
                if (canScroll(child, dx)) {
                    if (abs(dx) > touchSlop) {
                        state = STATE_DRAGGING
                        isBeingDragged = true
                        ensureVelocityTracker()
                    }
                } else {
                    parent.parent.requestDisallowInterceptTouchEvent(false)
                }

            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                isBeingDragged = false
                releaseVelocityTracker()
            }
        }
        velocityTracker?.addMovement(ev)
        return isBeingDragged
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: View, ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastMotionX = ev.x.toInt()
                ensureVelocityTracker()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isBeingDragged) {
                    val x = ev.x.toInt()
                    val dx = x - lastMotionX
                    lastMotionX = x
                    scroll(child, dx)
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                velocityTracker?.let {
                    it.addMovement(ev)
                    it.computeCurrentVelocity(1000)
                    if (!fling(child, it.xVelocity)) {
                        // If the fling is not triggered, let's scroll to the nearest standard position
                        startScroll(child, getX(getNearestState(child)))
                    }
                    releaseVelocityTracker()
                }
                isBeingDragged = false
            }
        }
        velocityTracker?.addMovement(ev)
        return true
    }

    private fun ensureVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun releaseVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    /**
     * Whether scrolling is possible in a given direction.
     *
     * @param [direction] Positive number means scroll to right.
     */
    private fun canScroll(child: View, direction: Int): Boolean {
        // When scroll to the right in the ticwear system, MotionEvent.x always stays the same.
        // I don't have time to figure out why, so let's consider dx=0 as a swipe to the right.
        return if (direction >= 0) {
            child.left < parentWidth
        } else {
            child.left > 0
        }
    }

    private fun scroll(child: View, dx: Int) {
        if (dx == 0) {
            return
        }
        if (dx < 0) {
            if (child.left + dx < 0) {
                setLeft(child, 0)
            } else {
                ViewCompat.offsetLeftAndRight(child, dx)
            }
        } else {
            if (child.left + dx > parentWidth) {
                setLeft(child, parentWidth)
            } else {
                ViewCompat.offsetLeftAndRight(child, dx)
            }
        }
    }

    private fun fling(child: View, velocityX: Float): Boolean {
        if (abs(velocityX) < minVelocity) {
            return false
        }
        startScroll(child, getX(if (velocityX > 0) STATE_HIDDEN else STATE_EXPANDED))
        return true
    }

    private fun startScroll(child: View, targetX: Int) {
        stopScroll()
        if (child.left != targetX) {
            state = STATE_SETTING
            scroller.startScroll(child.left, 0, targetX - child.left, 0)
            ViewCompat.postOnAnimation(child, flingRunnable)
        } else {
            updateState(child)
        }
    }

    private fun stopScroll() {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        childRef?.get()?.removeCallbacks(flingRunnable)
    }

    @MenuState
    private fun getNearestState(child: View): Int {
        return if (child.left < (getX(STATE_HIDDEN) - getX(STATE_EXPANDED)) / 2f) {
            STATE_EXPANDED
        } else {
            STATE_HIDDEN
        }
    }

    private fun updateState(child: View) {
        when (child.left) {
            0 -> state = STATE_EXPANDED
            parentWidth -> state = STATE_HIDDEN
        }
    }

    private fun getX(@MenuState targetState: Int): Int {
        return when (targetState) {
            STATE_HIDDEN -> parentWidth
            STATE_EXPANDED -> 0
            else -> throw IllegalArgumentException("Unknown target state: $targetState.")
        }
    }

    private fun setLeft(view: View, left: Int) {
        ViewCompat.offsetLeftAndRight(view, left - view.left)
    }


    private class SavedState : View.BaseSavedState {

        @get:MenuState
        @setparam:MenuState
        var state: Int = STATE_HIDDEN

        constructor(superState: Parcelable?, @MenuState state: Int) : super(superState) {
            this.state = state
        }

        constructor(source: Parcel) : super(source) {
            this.state = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(state)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(p0: Parcel): SavedState = SavedState(p0)

            override fun newArray(p0: Int): Array<SavedState?> = arrayOfNulls(p0)
        }
    }

    // ------------------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------------------

    val isIdle get() = state == STATE_HIDDEN || state == STATE_EXPANDED

    fun setMenuState(@MenuState state: Int) {
        childRef?.get()?.let { view ->
            startScroll(view, getX(state))
        } ?: kotlin.run { this.state = state }
    }
}