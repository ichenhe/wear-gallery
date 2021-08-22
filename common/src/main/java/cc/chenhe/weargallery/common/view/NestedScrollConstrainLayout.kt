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

package cc.chenhe.weargallery.common.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * An ConstraintLayout which will dispatch nested scroll events to [androidx.core.view.NestedScrollingParent].
 * This is used to add scrolling support to the collapse header layout block.
 *
 * Call [setNestedScrollingEnabled] with `false` will be ignored since you should use [ConstraintLayout] directly
 * instead.
 */
open class NestedScrollConstrainLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), NestedScrollingChild2 {

    private val helper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private var lastTouchX = 0
    private var lastTouchY = 0

    private val minFlingVelocity: Float
    private val maxFlingVelocity: Float
    private val scroller: OverScroller = OverScroller(context)
    private val velocityTracker: VelocityTracker by lazy { VelocityTracker.obtain() }

    init {
        val vc = ViewConfiguration.get(context)
        minFlingVelocity = vc.scaledMinimumFlingVelocity.toFloat()
        maxFlingVelocity = vc.scaledMaximumFlingVelocity.toFloat()
        isNestedScrollingEnabled = true
        helper.isNestedScrollingEnabled = true
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        if (!enabled) {
            Timber.tag("NestedScrollCL")
                .w("This layout is designed to dispatch nested scroll events so disable request will be ignored.")
            return
        }
        super.setNestedScrollingEnabled(enabled)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return helper.hasNestedScrollingParent(type)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return helper.hasNestedScrollingParent()
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return helper.startNestedScroll(axes, type)
    }

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return helper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?, type: Int
    ): Boolean {
        return helper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type
        )
    }

    override fun stopNestedScroll(type: Int) {
        helper.stopNestedScroll(type)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return helper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return helper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    @SuppressLint("ClickableViewAccessibility") // Delegate to super
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        velocityTracker.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_TOUCH)
            }
            MotionEvent.ACTION_MOVE -> {
                var dx = lastTouchX - x
                var dy = lastTouchY - y
                lastTouchX = x
                lastTouchY = y
                if (dispatchNestedPreScroll(dx, dy, scrollConsumed, scrollOffset, TYPE_TOUCH)) {
                    dx -= scrollConsumed[0]
                    dy -= scrollConsumed[1]
                    // This view never consume scroll distance because it is non-scrollable.
                    dispatchNestedScroll(0, 0, dx, dy, scrollOffset, TYPE_TOUCH)
                    // For the same reason we never deal with remaining distance.
                }
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity)
                fling(velocityTracker.xVelocity, velocityTracker.yVelocity)
                velocityTracker.clear()
                stopNestedScroll(TYPE_TOUCH)
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker.clear()
                stopNestedScroll(TYPE_TOUCH)
            }
        }
        super.onTouchEvent(event)
        return true
    }

    private fun fling(velocityX: Float, velocityY: Float): Boolean {
        val vx = if (abs(velocityX) < minFlingVelocity) 0f else velocityX
        val vy = if (abs(velocityY) < minFlingVelocity) 0f else velocityY
        if (vx == 0f && vy == 0f) {
            return false
        }
        if (!dispatchNestedPreFling(vx, vy)) {
            dispatchNestedFling(vx, vy, true)

            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, TYPE_NON_TOUCH)
            doFling(
                max(-maxFlingVelocity, min(vx, maxFlingVelocity)),
                max(-maxFlingVelocity, min(vy, maxFlingVelocity))
            )
            return true
        }
        return false
    }

    private var lastFlingX = 0
    private var lastFlingY = 0

    private fun doFling(velocityX: Float, velocityY: Float) {
        scroller.fling(
            0, 0, velocityX.toInt(), velocityY.toInt(), Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE,
            Int.MAX_VALUE
        )
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val x = scroller.currX
            val y = scroller.currY
            // When finger flings upwards, velocityY < 0, result in smaller and smaller currY.
            // But dy should > 0 if scroll up.
            var dx = lastFlingX - x
            var dy = lastFlingY - y

            lastFlingX = x
            lastFlingY = y

            if (dispatchNestedPreScroll(dx, dy, scrollConsumed, scrollOffset, TYPE_NON_TOUCH)) {
                dx -= scrollConsumed[0]
                dy -= scrollConsumed[1]
                // This view does not consume any scroll distance actually because it is non-scrollable.
                dispatchNestedScroll(0, 0, dx, dy, scrollOffset, TYPE_NON_TOUCH)
            }
            invalidate()
        } else {
            stopNestedScroll(TYPE_NON_TOUCH)
        }
    }

}