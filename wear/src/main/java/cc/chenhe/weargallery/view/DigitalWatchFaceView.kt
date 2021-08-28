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

package cc.chenhe.weargallery.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import cc.chenhe.weargallery.watchface.painter.DigitalPainter
import cc.chenhe.weargallery.watchface.painter.Painter
import timber.log.Timber
import java.util.*

private const val TAG = "WatchFaceView"

/**
 * A view to display digital watch face. This view can ensure consistent behavior with the actual watch face service.
 * Full screen size is highly recommended.
 *
 * Attention: watch face properties in preference will not be updated to the view after being changed, see [painter]'s
 * construction parameters.
 */
class DigitalWatchFaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val NO_VALUE = -1f

        private const val UPDATE_INTERVAL = 1000L
    }

    val timeX get() = painter?.x ?: NO_VALUE
    val timeY get() = painter?.y ?: NO_VALUE
    val timeTextSize get() = painter?.textSize ?: NO_VALUE

    private var painter: DigitalPainter? = null
    private val bounds: Rect = Rect()

    private var is24Hour = DateFormat.is24HourFormat(context)

    private val capacity = object : Painter.Capacity {
        private val calendar = Calendar.getInstance()

        override fun isInAmbientMode(): Boolean = false

        override fun getCalendar(): Calendar = calendar

        override fun is24Hour(): Boolean = is24Hour
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        painter = DigitalPainter(context, capacity, false).also {
            it.onCreate()
        }
        is24Hour = DateFormat.is24HourFormat(context)
    }

    val dm: DisplayMetrics = DisplayMetrics()

    @Suppress("DEPRECATION") // no compat function
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // always use max size
        var w = MeasureSpec.getSize(widthMeasureSpec)
        var h = MeasureSpec.getSize(heightMeasureSpec)

        // fit screen ratio
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(dm)
        val ratio = dm.widthPixels / dm.heightPixels.toDouble()
        if (w / h.toDouble() >= ratio) {
            w = (h * ratio).toInt()
        } else {
            h = (w / ratio).toInt()
        }
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
        ) {
            Timber.tag(TAG).w("Automatically resize to fit the screen ratio. w=$w, h=$h")
        }
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(0, 0, width, height)
        painter?.onSurfaceChanged(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        painter?.onDraw(canvas, bounds)
        postInvalidateDelayed(UPDATE_INTERVAL)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        painter?.onDestroy()
        painter = null
    }


    // ---------------------------------------------------------------------------------------------
    // API
    // ---------------------------------------------------------------------------------------------

    fun increaseTimeX(value: Float) {
        painter?.let { painter ->
            val i = painter.x + value
            if (i in 0f..width.toFloat()) {
                painter.x = i
                invalidate()
            }
        }
    }

    fun increaseTimeY(value: Float) {
        painter?.let { painter ->
            val i = painter.y + value
            if (i in 0f..height.toFloat()) {
                painter.y = i
                invalidate()
            }
        }
    }

    fun increaseTimeTextSize(value: Float) {
        painter?.let {
            val n = it.textSize + value
            if (n > 0) {
                it.textSize = n
                invalidate()
            }
        }
    }
}