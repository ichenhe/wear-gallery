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

import android.content.res.Resources
import android.graphics.Rect
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

/**
 * Implement drag selection function for [RecyclerView].
 *
 * Usage:
 *
 * ```kotlin
 * val selectListener = DragSelectProcessor(DragSelectProcessor.Mode.Simple, object :DragSelectProcessor.SimpleSelectionHandler() {
 *      override fun updateSelection(start: Int, end: Int, isSelected: Boolean, calledFromOnStart: Boolean) {
 *          adapter.selectRange(start,end,isSelected)
 *      }
 * })

 * val dragSelectTouchListener = DragSelectTouchListener().withSelectListener(selectListener)
 * recyclerView.addOnItemTouchListener(dragSelectTouchListener)
 *
 * // start drag selection (ex. onItemLongClickListener)
 * dragSelectTouchListener.startDragSelection(position)
 * ```
 */
open class DragSelectTouchListener : RecyclerView.SimpleOnItemTouchListener() {

    private var isActive = false

    // Whether should we scroll
    private var inTopSpot = false
    private var inBottomSpot = false

    // The index where finger put down/up. The relationship of size is uncertain.
    private var start = RecyclerView.NO_POSITION
    private var end = RecyclerView.NO_POSITION

    // The range of last touch event's selection, lastStart <=lastEnd
    private var lastStart = RecyclerView.NO_POSITION
    private var lastEnd = RecyclerView.NO_POSITION

    // The location of last touch event
    private var lastX = 0f
    private var lastY = 0f

    private var scrolling = false
    private var scrollDistance = 0  // = maxScrollDistance * scrollSpeedFactor (vector)

    // nested scroll
    private val visibleRect = Rect()
    private val consumed = intArrayOf(0, 0)

    // touch auto scroll regions
    private var topBoundFrom = 0
    private var topBoundTo = 0
    private var bottomBoundFrom = 0
    private var bottomBoundTo = 0

    private lateinit var recyclerView: RecyclerView
    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (scrolling) {
                scrollBy(scrollDistance)
                recyclerView.postOnAnimation(this)
            }
        }
    }

    var selectListener: OnDragSelectListener? = null


    // ----------------------------------
    // settings
    // ----------------------------------

    /**
     * The distance in pixels that the RecyclerView is maximally scrolled per scroll event.
     * Higher values result in higher scrolling speed.
     */
    var maxScrollDistance = 46

    /**
     * Height of auto scroll region.
     */
    var autoScrollDistance = (Resources.getSystem().displayMetrics.density * 56).toInt()

    /**
     * Distance (vector) for the top scroll region from the top.
     */
    var touchRegionTopOffset = 0

    /**
     * Distance (vector) for the bottom scroll region from the bottom.
     *
     * **NOTICE:** The value should be less than 0 if you want to move region upwards.
     */
    var touchRegionBottomOffset = 0

    /**
     * Whether continue scroll even if the touch moves above the top scroll region.
     */
    var scrollAboveTopRegion = true

    /**
     * Whether continue scroll even if the touch moves below the bottom scroll region.
     */
    var scrollBelowBottomRegion = true


    // --------------------------------------
    // Functions
    // --------------------------------------

    init {
        reset()
    }

    fun withSelectListener(listener: OnDragSelectListener): DragSelectTouchListener {
        this.selectListener = listener
        return this
    }

    /**
     * Start the drag selection.
     *
     * @param position The index of the first select item.
     */
    fun startDragSelection(position: Int) {
        isActive = true
        start = position
        end = position
        lastStart = position
        lastEnd = position
        selectListener?.onSelectionStarted(position)
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isActive || (rv.adapter?.itemCount ?: 0) == 0) {
            return false
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> reset(rv)
        }
        recyclerView = rv
        return true
    }

    /**
     * Calculate the region which auto scroll should be triggered.
     *
     * The implement must set [topBoundFrom], [topBoundTo], [bottomBoundFrom], [bottomBoundTo] to indicate the result.
     */
    protected open fun calculateAutoScrollRegion(rv: RecyclerView) {
        rv.getLocalVisibleRect(visibleRect)
        topBoundFrom = touchRegionTopOffset
        topBoundTo = touchRegionTopOffset + autoScrollDistance
        bottomBoundFrom = visibleRect.bottom + touchRegionBottomOffset - autoScrollDistance
        bottomBoundTo = visibleRect.bottom + touchRegionBottomOffset
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!isActive) {
            return
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!inTopSpot && !inBottomSpot) {
                    updateSelectedRange(rv, e.x, e.y)
                }
                calculateAutoScrollRegion(rv)
                processAutoScroll(rv, e)
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                reset(rv)
            }
        }
    }

    private fun updateSelectedRange(rv: RecyclerView, x: Float, y: Float) {
        rv.findChildViewUnder(x, y)?.let { child ->
            val position = rv.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && end != position) {
                end = position
                notifySelectRangeChanged()
            }
        }
    }

    private fun notifySelectRangeChanged() {
        if (start == RecyclerView.NO_POSITION || end == RecyclerView.NO_POSITION) {
            return
        }
        selectListener?.let { listener ->
            val newStart = min(start, end)
            val newEnd = max(start, end)
            if (lastStart == RecyclerView.NO_POSITION || lastEnd == RecyclerView.NO_POSITION) {
                if (newEnd - newStart == 1) {
                    listener.onSelectChange(newStart, newStart, true)
                } else {
                    listener.onSelectChange(newStart, newEnd, true)
                }
            } else {
                if (newStart > lastStart) {
                    listener.onSelectChange(lastStart, newStart - 1, false)
                } else if (newStart < lastStart) {
                    listener.onSelectChange(newStart, lastStart - 1, true)
                }

                if (newEnd > lastEnd) {
                    listener.onSelectChange(lastEnd + 1, newEnd, true)
                } else if (newEnd < lastEnd) {
                    listener.onSelectChange(newEnd + 1, lastEnd, false)
                }
            }
            lastStart = newStart
            lastEnd = newEnd
        }
    }

    /**
     * Check the touch location to calculate speed and start or stop scrolling.
     */
    private fun processAutoScroll(rv: RecyclerView, event: MotionEvent) {
        val y = event.y.toInt()
        if (y in topBoundFrom..topBoundTo) {
            // located at the top scroll region
            lastX = event.x
            lastY = event.y
            val scrollSpeedFactor = ((topBoundTo - topBoundFrom) - (y - topBoundFrom)).toFloat() /
                    (topBoundTo - topBoundFrom)
            scrollDistance = (maxScrollDistance * scrollSpeedFactor * -1).toInt()
            if (!inTopSpot) {
                inTopSpot = true
                startAutoScroll(rv)
            }
        } else if (scrollAboveTopRegion && y < topBoundFrom) {
            // continue scrolling even if above the top scroll region
            lastX = event.x
            lastY = event.y
            scrollDistance = maxScrollDistance * -1 // max speed
            if (!inTopSpot) {
                inTopSpot = true
                startAutoScroll(rv)
            }
        } else if (y in bottomBoundFrom..bottomBoundTo) {
            // located at the bottom scroll region
            lastX = event.x
            lastY = event.y
            val scrollSpeedFactor = (y - bottomBoundFrom) / (bottomBoundTo - bottomBoundFrom).toFloat()
            scrollDistance = (maxScrollDistance * scrollSpeedFactor).toInt()
            if (!inBottomSpot) {
                inBottomSpot = true
                startAutoScroll(rv)
            }
        } else if (scrollBelowBottomRegion && y > bottomBoundTo) {
            // continue scrolling even if below the bottom scroll region
            lastX = event.x
            lastY = event.y
            scrollDistance = maxScrollDistance // max speed
            if (!inBottomSpot) {
                inBottomSpot = true
                startAutoScroll(rv)
            }
        } else {
            // other region - stop scroll
            inBottomSpot = false
            inTopSpot = false
            lastX = Float.MIN_VALUE
            lastY = Float.MIN_VALUE
            stopAutoScroll(rv)
        }
    }

    private fun startAutoScroll(rv: RecyclerView) {
        if (!scrolling) {
            scrolling = true
            rv.removeCallbacks(scrollRunnable)
            rv.postOnAnimation(scrollRunnable)
        }
    }

    private fun stopAutoScroll(rv: RecyclerView) {
        if (scrolling) {
            scrolling = false
            rv.removeCallbacks(scrollRunnable)
        }
    }

    private fun scrollBy(distance: Int) {
        // Because RecyclerView will reset scroll state after calling dispatchToOnItemTouchListeners() which will
        // invoke stopNestedScroll(), so we can't startNestedScroll in onTouchEvent's body since it will return before
        // RV calling stop function.
        // For the same reason we don't need to call stopNestedScroll() ourselves.
        if (distance > 0) {
            // Only upwards is needed in this project.
            recyclerView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            recyclerView.dispatchNestedPreScroll(0, distance, consumed, null, ViewCompat.TYPE_TOUCH)
            recyclerView.scrollBy(0, distance - consumed[1])
        } else {
            recyclerView.scrollBy(0, distance)
        }
        if (lastX != Float.MIN_VALUE && lastY != Float.MIN_VALUE) {
            updateSelectedRange(recyclerView, lastX, lastY)
        }
    }

    private fun reset(rv: RecyclerView? = null) {
        isActive = false
        selectListener?.onSelectionFinished(end)
        start = RecyclerView.NO_POSITION
        end = RecyclerView.NO_POSITION
        lastStart = RecyclerView.NO_POSITION
        lastEnd = RecyclerView.NO_POSITION

        inTopSpot = false
        inBottomSpot = false
        lastX = Float.MIN_VALUE
        lastY = Float.MIN_VALUE
        rv?.let { stopAutoScroll(rv) }
    }

    interface OnDragSelectListener {

        /**
         * @param position The item index on which the drag selection was started at.
         */
        fun onSelectionStarted(position: Int)

        /**
         * @param position The item index on which the drag selection was finished at.
         */
        fun onSelectionFinished(position: Int)

        /**
         * @param start The newly (un)selected range start.
         * @param end The newly (un)selected range end.
         * @param isSelected True, it range got selected, false if not.
         */
        fun onSelectChange(start: Int, end: Int, isSelected: Boolean)
    }

}