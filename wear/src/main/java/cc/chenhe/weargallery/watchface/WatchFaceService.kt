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

package cc.chenhe.weargallery.watchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import cc.chenhe.lib.watchfacehelper.BaseWatchFaceService
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.xlogAppenderFlushSafely
import cc.chenhe.weargallery.uilts.fetchWatchFaceDimImage
import cc.chenhe.weargallery.uilts.fetchWatchFaceStyle
import cc.chenhe.weargallery.watchface.painter.AnalogPainter
import cc.chenhe.weargallery.watchface.painter.DigitalPainter
import cc.chenhe.weargallery.watchface.painter.Painter
import timber.log.Timber
import java.util.*

private const val TAG = "WatchFaceSer"

class WatchFaceService : BaseWatchFaceService() {

    private val context: Context = this
    private lateinit var analogStyle: String
    private lateinit var digitalStyle: String
    private lateinit var type: LiveData<StyleMode>
    private lateinit var showImageInDim: LiveData<Boolean>

    override fun onCreate() {
        super.onCreate()
        analogStyle = getString(R.string.wf_preference_type_entry_value_analog)
        digitalStyle = getString(R.string.wf_preference_type_entry_value_digital)
        type = fetchWatchFaceStyle(context, true).map { original ->
            when (original) {
                analogStyle -> StyleMode.Analog
                digitalStyle -> StyleMode.Digital
                else -> {
                    Timber.tag(TAG).w("Unknown watch face style type <$original>, use analog mode.")
                    StyleMode.Analog
                }
            }
        }
        showImageInDim = fetchWatchFaceDimImage(context, true)
    }

    override fun onCreateEngine(): Engine {
        return MyEngine()
    }

    private inner class MyEngine : BaseWatchFaceService.BaseEngine() {

        private val showImageInDimObserver = Observer<Boolean> {
            if (isInAmbientMode)
                invalidate()
        }
        private val styleObserver = StyleObserver()
        private var painter: Painter? = null

        private var holder: SurfaceHolder? = null
        private var width = 0
        private var height = 0

        private val cap = object : Painter.Capacity {
            override fun isInAmbientMode(): Boolean = isInAmbientMode

            override fun getCalendar(): Calendar = calendar

            override fun is24Hour(): Boolean = !is12h

            override fun displayImageInDim(): Boolean = showImageInDim.value ?: true
        }

        private inner class StyleObserver : Observer<StyleMode> {
            override fun onChanged(style: StyleMode) {
                when (style) {
                    StyleMode.Analog -> {
                        if (painter != null && painter is AnalogPainter) {
                            return
                        }
                        painter?.onDestroy()
                        painter = createPainter { AnalogPainter(this@WatchFaceService, cap) }
                    }
                    StyleMode.Digital -> {
                        if (painter != null && painter is DigitalPainter) {
                            return
                        }
                        painter?.onDestroy()
                        painter = createPainter { DigitalPainter(this@WatchFaceService, cap) }
                    }
                }
            }
        }

        /**
         * Create a painter and invoke all necessary lifecycle method.
         *
         * @param creator Function to create a painter.
         */
        private inline fun createPainter(creator: (() -> Painter)): Painter? {
            return holder?.let {
                creator().also {
                    it.onCreate()
                    dispatchOnSurfaceChanged(it)
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setInteractiveUpdateRateMS(50)
            this.holder = holder
            showImageInDim.observeForever(showImageInDimObserver)
            type.observeForever(styleObserver)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            updateDeviceAttr(holder, width, height)
        }

        /**
         * Update device attribute fields and notify the current painter if necessary.
         */
        private fun updateDeviceAttr(holder: SurfaceHolder, width: Int, height: Int) {
            if (holder != this.holder || width != this.width || height != this.height) {
                this.holder = holder
                this.width = width
                this.height = height
                dispatchOnSurfaceChanged(painter)
            }
        }

        /**
         * Notify the painter of OnSurfaceChanged event if the current surface is valid.
         */
        private fun dispatchOnSurfaceChanged(painter: Painter?) {
            holder?.let {
                if (this.width != 0 && this.height != 0) {
                    painter?.onSurfaceChanged(width, height)
                }
            }
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            painter?.onAmbientModeChanged(inAmbientMode)
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            super.onDraw(canvas, bounds)
            painter?.onDraw(canvas, bounds)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            this.holder = null
            this.width = 0
            this.height = 0
        }

        override fun onDestroy() {
            if (::type.isInitialized) {
                type.removeObserver(styleObserver)
            }
            if (::showImageInDim.isInitialized) {
                showImageInDim.removeObserver(showImageInDimObserver)
            }
            super.onDestroy()
            painter?.onDestroy()
        }
    }

    private enum class StyleMode {
        Analog, Digital
    }

    override fun onDestroy() {
        super.onDestroy()
        xlogAppenderFlushSafely()
    }
}