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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.exifinterface.media.ExifInterface
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.ControlledRunner
import cc.chenhe.weargallery.uilts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.min

private const val TAG = "Painter"

/**
 * Abstract class to draw watch face.
 *
 * This class maintains a [Bitmap] internally indicates the background with [bg] variable. It will update automatically
 * if necessary.
 *
 * @param observeConfig Whether to subscribe to configuration changes and update the watch face. Only read the
 * configuration once for initialization if `false`.
 */
abstract class Painter(
        protected val context: Context,
        private val capacity: Capacity,
        protected val observeConfig: Boolean
) {

    interface Capacity {
        fun isInAmbientMode(): Boolean

        fun getCalendar(): Calendar

        fun is24Hour(): Boolean
    }

    protected var width: Int = 0
        private set
    protected var height: Int = 0
        private set
    protected var centerX: Float = 0f
        private set
    protected var centerY: Float = 0f
        private set

    protected val isInAmbientMode get() = capacity.isInAmbientMode()
    protected val calendar: Calendar get() = capacity.getCalendar()
    protected val is24Hour: Boolean get() = capacity.is24Hour()

    protected var bg: Bitmap? = null
        private set

    private val controller = ControlledRunner<Unit>()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_WATCH_FACE_BACKGROUND_CHANGED -> {
                    logd(TAG, "Background picture changed, reload.")
                    GlobalScope.launch {
                        controller.cancelPreviousThenRun { loadBackgroundImage(width, height) }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Lifecycle functions
    // ------------------------------------------------------------------------------------

    @CallSuper
    open fun onCreate() {
        val filter = IntentFilter().apply {
            addAction(ACTION_WATCH_FACE_BACKGROUND_CHANGED)
        }
        if (observeConfig) {
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        }
    }

    @CallSuper
    open fun onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
    }

    @CallSuper
    open fun onSurfaceChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        centerX = width / 2f
        centerY = height / 2f
        if (bg == null || bg?.width != width || bg?.height != height) {
            GlobalScope.launch {
                controller.cancelPreviousThenRun { loadBackgroundImage(width, height) }
            }
        }
    }

    open fun onAmbientModeChanged(inAmbientMode: Boolean) {}

    abstract fun onDraw(canvas: Canvas, bounds: Rect)

    // ------------------------------------------------------------------------------------
    // Functions
    // ------------------------------------------------------------------------------------

    /**
     * Called at the end of [loadBackgroundImage] function, no matter what the result is.
     */
    open suspend fun onBackgroundImageLoaded() {}

    /**
     * Load background picture to [bg] variable. `null` if encounters any error.
     */
    protected suspend fun loadBackgroundImage(width: Int, height: Int) = withContext(Dispatchers.Default) {
        logd(TAG, "Loading background picture.")
        val file = File(getWatchFaceResFolder(context), WATCH_FACE_BACKGROUND)
        if (!file.isFile) {
            logw(TAG, "Background picture file not exist, use default picture.")
            val img = (context.getDrawable(R.drawable.preview) as BitmapDrawable).bitmap
            bg = Bitmap.createScaledBitmap(img, width, height, true)
            onBackgroundImageLoaded()
            return@withContext
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.apply {
            inJustDecodeBounds = false
            inSampleSize = calculateInSample(options, width, height)
        }
        if (options.outMimeType == "image/gif") {
            Toast.makeText(context, R.string.wf_not_support_gif, Toast.LENGTH_SHORT).show()
        }

        var img = BitmapFactory.decodeFile(file.absolutePath, options)
        val rotate = getExifRotateAngle(file.absolutePath).toFloat()

        // crop & rotate
        val matrix = Matrix().apply { setRotate(rotate, img.width / 2f, img.height / 2f) }
        img = if (img.width > width || img.height > height) {
            Bitmap.createBitmap(img,
                    max(((img.width - width) / 2f).toInt(), 0),
                    max(((img.height - height) / 2f).toInt(), 0),
                    min(img.width, width),
                    min(img.height, height),
                    matrix,
                    true
            )
        } else {
            Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        }
        bg = img
        logd(TAG, "Load background picture finish.")
        onBackgroundImageLoaded()
    }

    private fun calculateInSample(options: BitmapFactory.Options, targetWidth: Int, targetHeight: Int): Int {
        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        var inSample = 1
        if (rawWidth > targetWidth || rawHeight > targetHeight) {
            val halfWidth = rawWidth / 2
            val halfHeight = rawHeight / 2
            while (halfWidth / inSample >= targetWidth && halfHeight / inSample >= targetHeight) {
                inSample *= 2
            }
        }
        return inSample
    }

    private fun getExifRotateAngle(fileName: String): Int {
        var angle = 0
        try {
            val exif = ExifInterface(fileName)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return angle
    }

    protected fun Paint.resetWithAntiAlias() {
        reset()
        isAntiAlias = true
    }
}