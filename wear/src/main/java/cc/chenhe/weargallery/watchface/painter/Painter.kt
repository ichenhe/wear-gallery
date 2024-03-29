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
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.ControlledRunner
import cc.chenhe.weargallery.uilts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

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

        fun displayImageInDim(): Boolean
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
    protected val displayImageInDim: Boolean get() = capacity.displayImageInDim()

    protected var bg: Bitmap? = null
        private set

    private val controller = ControlledRunner<Unit>()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_WATCH_FACE_BACKGROUND_CHANGED -> {
                    Timber.tag(TAG).d("Background picture changed, reload.")
                    ProcessLifecycleOwner.get().lifecycleScope.launch {
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
            ProcessLifecycleOwner.get().lifecycleScope.launch {
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
    protected suspend fun loadBackgroundImage(width: Int, height: Int) =
        withContext(Dispatchers.Default) {
            Timber.tag(TAG).d("Loading background picture.")
            val file = File(getWatchFaceResFolder(context), WATCH_FACE_BACKGROUND)
            if (!file.isFile) {
                Timber.tag(TAG).w("Background picture file not exist, use default picture.")
                val img = (ContextCompat.getDrawable(
                    context,
                    R.drawable.preview
                ) as BitmapDrawable).bitmap
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
                Timber.tag(TAG).w("Gif detected, not support!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.wf_not_support_gif, Toast.LENGTH_SHORT).show()
                }
            }

            var img = BitmapFactory.decodeFile(file.absolutePath, options)
            val rotate = getExifRotateAngle(file.absolutePath)
            if (rotate != 0) {
                // Let's rotate it first to avoid affect the width and height judgment.
                val matrix =
                    Matrix().apply { setRotate(rotate.toFloat(), img.width / 2f, img.height / 2f) }
                img = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, false)
            }
            Timber.tag(TAG).d("Loaded image ${img.width}x${img.height}; exifRotation=$rotate")

            // crop & rotate
            if (img.width != width || img.height != height) {
                val deviceRatio = width / height.toFloat()
                val imgRatio = img.width / img.height.toFloat()

                val scale: Float
                val regionWidth: Int
                val regionHeight: Int
                if (imgRatio >= deviceRatio) {
                    scale = height / img.height.toFloat()
                    regionWidth = (img.height * deviceRatio).toInt()
                    regionHeight = img.height
                } else {
                    scale = width / img.width.toFloat()
                    regionWidth = img.width
                    regionHeight = (img.width / deviceRatio).toInt()
                }
                Timber.tag(TAG).d("scale=$scale; subregion=${regionWidth}x${regionHeight}")

                val matrix = Matrix().apply { postScale(scale, scale) }
                img = Bitmap.createBitmap(
                    img,
                    ((img.width - regionWidth) / 2f).toInt(),
                    ((img.height - regionHeight) / 2f).toInt(),
                    regionWidth,
                    regionHeight,
                    matrix,
                    true
                )
            }
            bg = img
            Timber.tag(TAG).d("Load background picture finish. ${img.width}x${img.height}")
            onBackgroundImageLoaded()
        }

    private fun calculateInSample(
        options: BitmapFactory.Options,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
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
        Timber.tag(TAG)
            .d("raw=${rawWidth}x${rawHeight}; target=${targetWidth}x${targetHeight}; inSample=$inSample")
        return inSample
    }

    private fun getExifRotateAngle(fileName: String): Int {
        var angle = 0
        try {
            val exif = ExifInterface(fileName)
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
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