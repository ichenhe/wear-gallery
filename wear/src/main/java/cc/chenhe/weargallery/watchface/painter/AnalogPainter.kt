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
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private const val CENTER_GAP_AND_CIRCLE_RADIUS = 4f

class AnalogPainter(context: Context, capacity: Capacity, observeConfig: Boolean = true)
    : Painter(context, capacity, observeConfig) {

    private lateinit var paint: Paint

    private var handColor = Color.WHITE
    private var handHighlightColor = Color.RED
    private var handShadowColor = Color.GRAY

    private var secondHandLength = 0f
    private var minuteHandLength = 0f
    private var hourHandLength = 0f

    override fun onCreate() {
        super.onCreate()
        paint = Paint()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        super.onSurfaceChanged(width, height)
        secondHandLength = (centerX * 0.875).toFloat()
        minuteHandLength = (centerX * 0.75).toFloat()
        hourHandLength = (centerX * 0.5).toFloat()
    }

    override fun onDraw(canvas: Canvas, bounds: Rect) {
        canvas.drawColor(Color.BLACK)
        if (!isInAmbientMode) {
            bg?.let { bg ->
                canvas.drawBitmap(bg, (width - bg.width) / 2f, (height - bg.height) / 2f, null)
            }
        }

        // scale
        val innerTickRadius: Float = centerX - 10
        val outerTickRadius: Float = centerX
        for (tickIndex in 0..11) {
            val tickRot = (tickIndex * Math.PI * 2 / 12).toFloat()
            val innerX = sin(tickRot.toDouble()).toFloat() * innerTickRadius
            val innerY = (-cos(tickRot.toDouble())).toFloat() * innerTickRadius
            val outerX = sin(tickRot.toDouble()).toFloat() * outerTickRadius
            val outerY = (-cos(tickRot.toDouble())).toFloat() * outerTickRadius
            canvas.drawLine(centerX + innerX, centerY + innerY,
                    centerX + outerX, centerY + outerY, scalePaint)
        }

        // time

        /*
         * These calculations reflect the rotation in degrees per unit of time, e.g.,
         * 360 / 60 = 6 and 360 / 12 = 30.
         */
        val seconds: Float = calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f
        val secondsRotation = seconds * 6f

        val minutesRotation: Float = calendar.get(Calendar.MINUTE) * 6f

        val hourHandOffset: Float = calendar.get(Calendar.MINUTE) / 2f
        val hoursRotation: Float = calendar.get(Calendar.HOUR) * 30 + hourHandOffset


        canvas.save()

        canvas.rotate(hoursRotation, centerX, centerY)
        canvas.drawLine(
                centerX,
                centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY - hourHandLength,
                hourPaint)

        canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
        canvas.drawLine(
                centerX,
                centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY - minuteHandLength,
                minutePaint)

        if (!isInAmbientMode) {
            canvas.rotate(secondsRotation - minutesRotation, centerX, centerY)
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - secondHandLength,
                    secondPaint)
        }

        canvas.drawCircle(
                centerX,
                centerY,
                CENTER_GAP_AND_CIRCLE_RADIUS,
                scalePaint)


        canvas.restore()
    }

    override suspend fun onBackgroundImageLoaded() = withContext(Dispatchers.Default) {
        val img = bg ?: return@withContext
        Palette.from(img).generate().let { palette ->
            handColor = palette.getLightVibrantColor(Color.WHITE)
            handHighlightColor = palette.getVibrantColor(Color.RED)
            handShadowColor = palette.getDarkMutedColor(Color.GRAY)
        }
    }

    private val hourPaint: Paint
        get() = paint.apply {
            resetWithAntiAlias()
            color = handColor
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(6f, 0f, 0f, handShadowColor)
        }


    private val minutePaint: Paint
        get() = paint.apply {
            resetWithAntiAlias()
            color = handColor
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(6f, 0f, 0f, handShadowColor)
        }


    private val secondPaint: Paint
        get() = paint.apply {
            resetWithAntiAlias()
            color = handHighlightColor
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(6f, 0f, 0f, handShadowColor)
        }


    private val scalePaint: Paint
        get() = paint.apply {
            resetWithAntiAlias()
            color = handColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
            setShadowLayer(3f, 0f, 0f, handShadowColor)
        }

}