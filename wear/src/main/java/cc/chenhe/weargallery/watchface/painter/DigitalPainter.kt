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

package cc.chenhe.weargallery.watchface.painter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import cc.chenhe.weargallery.uilts.*
import cc.chenhe.weargallery.watchface.ITimeHolder
import java.util.*

class DigitalPainter(context: Context, capacity: Capacity, observeConfig: Boolean = true) :
    Painter(context, capacity, observeConfig), ITimeHolder {

    private val tagParser = TagParser(this)

    private val paint = TextPaint().apply {
        resetWithAntiAlias()
        textAlign = Paint.Align.CENTER
        textSize = 0f
    }

    private lateinit var timeXLiveData: LiveData<Float>
    private val timeXObserver = object : Observer<Float> {
        override fun onChanged(timeX: Float?) {
            if (!observeConfig) {
                timeXLiveData.removeObserver(this)
            }
            x = if (timeX == null || timeX == -1f) {
                if (centerX != 0f) centerX else -1f
            } else {
                timeX
            }
        }
    }

    private lateinit var timeYLiveData: LiveData<Float>
    private val timeYObserver = object : Observer<Float> {
        override fun onChanged(timeY: Float?) {
            if (!observeConfig) {
                timeYLiveData.removeObserver(this)
            }
            y = if (timeY == null || timeY == -1f) {
                if (centerY != 0f) centerY else -1f
            } else {
                timeY
            }
        }
    }

    private lateinit var timeTextSize: LiveData<Float>
    private val timeTextSizeObserver = object : Observer<Float> {
        override fun onChanged(textSize: Float?) {
            if (!observeConfig) {
                timeYLiveData.removeObserver(this)
            }
            paint.textSize = textSize ?: 0f
        }
    }

    private lateinit var timeTextColor: LiveData<Int>
    private val timeTextColorObserver = object : Observer<Int> {
        override fun onChanged(t: Int?) {
            if (!observeConfig) {
                timeTextColor.removeObserver(this)
            }
            if (!isInAmbientMode) {
                t?.let { paint.color = it }
            }
        }
    }

    private lateinit var timeFormat: LiveData<String?>
    private val timeFormatObserver = object : Observer<String?> {
        override fun onChanged(t: String?) {
            if (!observeConfig) {
                timeFormat.removeObserver(this)
            }
        }
    }

    var x = 0f
    var y = 0f
    var textSize
        get() = paint.textSize
        set(value) {
            paint.textSize = value
        }

    override fun is24HourFormat(): Boolean = this.is24Hour

    override fun calendar(): Calendar = calendar

    override fun onCreate() {
        super.onCreate()
        // We only read preferences if the attribute is not set or observeConfig is enabled.
        if (x == 0f || observeConfig) {
            // Do NOT use `apply` here because observer uses live data variable internally.
            timeXLiveData = fetchWatchFaceTimeX(context, true)
            timeXLiveData.observeForever(timeXObserver)
        }
        if (y == 0f || observeConfig) {
            timeYLiveData = fetchWatchFaceTimeY(context, true)
            timeYLiveData.observeForever(timeYObserver)
        }
        if (paint.textSize == 0f || observeConfig) {
            timeTextSize = fetchWatchFaceTimeTextSize(context, true)
            timeTextSize.observeForever(timeTextSizeObserver)
        }
        timeTextColor = fetchWatchFaceTimeTextColor(context, true)
        timeTextColor.observeForever(timeTextColorObserver)

        timeFormat = fetchWatchFaceTimeFormat(context, true)
        timeFormat.observeForever(timeFormatObserver)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)
        if (x == -1f) {
            x = centerX
        }
        if (y == -1f) {
            y = centerY
        }
    }

    override fun onAmbientModeChanged(inAmbientMode: Boolean) {
        super.onAmbientModeChanged(inAmbientMode)
        if (inAmbientMode) {
            paint.color = Color.WHITE
        } else {
            paint.color = timeTextColor.value ?: Color.BLACK
        }
    }

    override fun onDraw(canvas: Canvas, bounds: Rect) {
        canvas.drawColor(Color.BLACK)
        tagParser.onFrameStart()
        if (!isInAmbientMode || displayImageInDim) {
            bg?.let { bg ->
                canvas.drawBitmap(bg, (width - bg.width) / 2f, (height - bg.height) / 2f, null)
            }
        }
        tagParser.parseString(timeFormat.value)?.let { text ->
            drawMessage(canvas, text, x, y)
        }
    }

    private fun drawMessage(canvas: Canvas, msg: String, x: Float, y: Float) {
        @Suppress("DEPRECATION")
        val staticLayout =
            StaticLayout(msg, paint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true)
        canvas.save()
        canvas.translate(x, y - staticLayout.height / 2f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeXLiveData.removeObserver(timeXObserver)
        timeYLiveData.removeObserver(timeYObserver)
        timeTextSize.removeObserver(timeTextSizeObserver)
        timeTextColor.removeObserver(timeTextColorObserver)
        timeFormat.removeObserver(timeFormatObserver)
    }
}