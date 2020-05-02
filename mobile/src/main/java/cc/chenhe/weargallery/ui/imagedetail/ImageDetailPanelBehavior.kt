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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.view.ViewCompat
import androidx.customview.view.AbsSavedState
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.utils.setAlpha
import java.lang.ref.WeakReference
import kotlin.math.abs


/**
 * The [CoordinatorLayout.Behavior] for image detail panel view that is positioned vertically below main ImageView.
 *
 * This behavior will be responsible for the drag of the panel, the transparency of panel title, toolbar and system
 * status bar.
 */
class ImageDetailPanelBehavior(context: Context, attrs: AttributeSet?)
    : CoordinatorLayout.Behavior<View>(context, attrs) {

    companion object {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(STATE_HIDDEN, STATE_HALF_EXPANDED, STATE_EXPANDED)
        annotation class PanelState

        const val STATE_HIDDEN = 1
        const val STATE_HALF_EXPANDED = 2
        const val STATE_EXPANDED = 3
    }

    var onStateChangeListener: ((state: Int) -> Unit)? = null

    private val touchSlop: Int
    private val minFlingVelocity: Float

    private var isBeingDragged = false

    @get:PanelState
    @setparam:PanelState
    private var state: Int = STATE_HIDDEN

    private var lastMotionY = 0
    private var velocityTracker: VelocityTracker? = null

    private val barColor = ContextCompat.getColor(context, R.color.imageDetailBarBg)
    private var childRef: WeakReference<View>? = null
    private var parentRef: WeakReference<CoordinatorLayout>? = null
    private var titleRef: WeakReference<TextView>? = null
    private var toolbarRef: WeakReference<Toolbar>? = null

    private val scroller: OverScroller by lazy { OverScroller(context) }
    private val flingRunnable = object : Runnable {
        override fun run() {
            childRef?.get()?.let { view ->
                if (scroller.computeScrollOffset()) {
                    setTop(view, scroller.currY)
                    ViewCompat.postOnAnimation(view, this)
                } else {
                    updateState()
                }
            } ?: scroller.abortAnimation()
        }
    }

    init {
        ViewConfiguration.get(context).also {
            touchSlop = it.scaledPagingTouchSlop
            minFlingVelocity = it.scaledMinimumFlingVelocity.toFloat()
        }
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        childRef = null
        parentRef = null
        titleRef = null
        toolbarRef = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        childRef = null
        parentRef = null
        titleRef = null
        toolbarRef = null
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: View): Parcelable? {
        val superState = super.onSaveInstanceState(parent, child)!!
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
        if (parentRef == null) {
            parentRef = WeakReference(parent)
        }
        if (titleRef == null) {
            child.findViewById<TextView>(R.id.imageDetailPanelTitle)?.let { titleRef = WeakReference(it) }
        }
        if (toolbarRef == null) {
            parent.findViewById<Toolbar>(R.id.imageDetailToolBar)?.let { toolbarRef = WeakReference(it) }
                    ?: throw IllegalArgumentException("Can not find Toolbar.")
        }
        parent.onLayoutChild(child, layoutDirection)
        offsetTopAndBottom(child, getOffset(state))
        return true
    }

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: View, ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isBeingDragged = false
                lastMotionY = ev.y.toInt()
                ensureVelocityTracker()
            }
            MotionEvent.ACTION_MOVE -> {
                val y = ev.y.toInt()
                val yDiff = abs(y - lastMotionY)
                if (yDiff > touchSlop) {
                    isBeingDragged = true
                    lastMotionY = y
                    ensureVelocityTracker()
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
                lastMotionY = ev.y.toInt()
                ensureVelocityTracker()
            }
            MotionEvent.ACTION_MOVE -> {
                val y = ev.y.toInt()
                var dy = y - lastMotionY
                if (!isBeingDragged && abs(dy) > touchSlop) {
                    isBeingDragged = true
                    if (dy > 0) {
                        dy -= touchSlop
                    } else {
                        dy += touchSlop
                    }
                }

                if (isBeingDragged) {
                    lastMotionY = y
                    scroll(child, dy)
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                velocityTracker?.let {
                    it.addMovement(ev)
                    it.computeCurrentVelocity(1000)
                    if (!fling(it.yVelocity)) {
                        // If the fling is not triggered, let's scroll to the nearest standard position
                        startScroll(getOffset(getNearestState(child)))
                    }
                    releaseVelocityTracker()
                }
                isBeingDragged = false
            }
        }
        velocityTracker?.addMovement(ev)
        return true
    }

    /**
     * @param dy Greater than 0 means the finger is down.
     */
    private fun scroll(child: View, dy: Int) {
        if (dy < 0) {
            val min = getOffset(STATE_EXPANDED)
            if (child.top + dy < min) {
                setTop(child, min)
            } else {
                offsetTopAndBottom(child, dy)
            }
        } else {
            val max = getOffset(STATE_HIDDEN)
            if (child.top + dy > max) {
                setTop(child, max)
            } else {
                offsetTopAndBottom(child, dy)
            }
        }
    }

    /**
     * Scroll to the corresponding position according to the fling direction.
     * The speed has nothing to do with [velocityY], it only used to detect the direction.
     *
     * @return Whether start fling.
     */
    private fun fling(velocityY: Float): Boolean {
        val yvel = if (abs(velocityY) < minFlingVelocity) 0f else velocityY
        if (yvel == 0f) {
            return false
        }
        startScroll(getOffset(getNextState(yvel > 0)))
        return true
    }

    private fun setTop(view: View, top: Int) {
        offsetTopAndBottom(view, top - view.top)
    }

    private fun offsetTopAndBottom(view: View, offset: Int) {
        ViewCompat.offsetTopAndBottom(view, offset)
        val progress = calculateExpandedProgress()
        titleRef?.get()?.alpha = progress

        val newColor = Color.BLACK.setAlpha(((1 - progress) * (255 - barColor.alpha) + barColor.alpha).toInt())
        (view.context as AppCompatActivity).window.statusBarColor = newColor
        toolbarRef?.get()?.setBackgroundColor(newColor)
    }

    private fun getNearestState(child: View): Int {
        val hidden = getOffset(STATE_HIDDEN)
        val halfExpanded = getOffset(STATE_HALF_EXPANDED)
        val expanded = getOffset(STATE_EXPANDED)
        return when {
            child.top <= (halfExpanded + expanded) / 2 -> {
                STATE_EXPANDED
            }
            child.top <= (hidden + halfExpanded) / 2 -> {
                STATE_HALF_EXPANDED
            }
            else -> {
                STATE_HIDDEN
            }
        }
    }

    /**
     * @param touchDirection True means downward.
     */
    private fun getNextState(touchDirection: Boolean): Int {
        return if (touchDirection) {
            STATE_HIDDEN
        } else {
            when (state) {
                STATE_HIDDEN -> STATE_HALF_EXPANDED
                else -> STATE_EXPANDED
            }
        }
    }

    private fun getOffset(@PanelState targetState: Int): Int {
        return parentRef?.get()?.let { parent ->
            when (targetState) {
                STATE_HIDDEN -> parent.height
                STATE_HALF_EXPANDED -> (parent.height + (toolbarRef?.get()?.top ?: 0)) / 2
                STATE_EXPANDED -> (toolbarRef?.get()?.top ?: 0)
                else -> throw IllegalArgumentException("Unknown target state: $targetState")
            }
        } ?: throw IllegalStateException("CoordinatorLayout has not been set or has been recycled.")

    }

    /**
     * Calculate the progress between fully expanded and half expanded. Zero means fully expended, one means half.
     *
     * @return From 0 to 1.
     */
    private fun calculateExpandedProgress(): Float {
        val child = childRef?.get() ?: return 1f
        val full = getOffset(STATE_EXPANDED)
        val half = getOffset(STATE_HALF_EXPANDED)
        return if (child.top >= half) {
            1f
        } else {
            (child.top - full) / (half - full).toFloat()
        }
    }

    /**
     * Update [state] if current position equals any standard position, else do nothing.
     */
    private fun updateState() {
        childRef?.get()?.let { child ->
            when (child.top) {
                getOffset(STATE_HIDDEN) -> state = STATE_HIDDEN
                getOffset(STATE_HALF_EXPANDED) -> state = STATE_HALF_EXPANDED
                getOffset(STATE_EXPANDED) -> state = STATE_EXPANDED
            }
            onStateChangeListener?.invoke(state)
        }
    }

    /**
     * Start auto scroll from current top to [targetY]. If scrolling then stop first.
     */
    private fun startScroll(targetY: Int) {
        stopScroll()
        childRef?.get()?.let { child ->
            if (child.top != targetY) {
                scroller.startScroll(0, child.top, 0, targetY - child.top)
                ViewCompat.postOnAnimation(child, flingRunnable)
            } else {
                // Already in standard position, let's update the state
                updateState()
            }
        }
    }

    private fun stopScroll() {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        childRef?.get()?.removeCallbacks(flingRunnable)
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

    private class SavedState(superState: Parcelable, val state: Int) : AbsSavedState(superState) {
        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(state)
        }
    }

    // ---------------------------------------------------------------------------
    // API
    // ---------------------------------------------------------------------------

    @SuppressLint("WrongConstant")
    @PanelState
    fun getState() = state

    fun setState(@PanelState state: Int) {
        startScroll(getOffset(state))
    }

}