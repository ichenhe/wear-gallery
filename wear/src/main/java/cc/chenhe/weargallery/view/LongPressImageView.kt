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
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlinx.coroutines.*

/**
 * An [AppCompatImageView] that can trigger events continuously when the user presses and holds.
 */
class LongPressImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var onLongPressListener: OnLongPressListener? = null
    var longPressEventInterval = 30L

    private var longPressJob: Job? = null

    init {
        super.setOnLongClickListener {
            longPressJob?.cancel()
            longPressJob = GlobalScope.launch(Dispatchers.Main) { dispatchLongPressEvent() }
            true
        }
    }

    override fun onDetachedFromWindow() {
        longPressJob?.cancel()
        super.onDetachedFromWindow()
    }

    @Deprecated("Does not support setting long click listener.", replaceWith = ReplaceWith(""),
            level = DeprecationLevel.ERROR)
    override fun setOnLongClickListener(l: OnLongClickListener?) {
        throw NotImplementedError("Does not support setting long click listener.")
    }

    private suspend fun dispatchLongPressEvent() {
        withContext(Dispatchers.Main) {
            while (isPressed) {
                onLongPressListener?.onLongPress(this@LongPressImageView)
                delay(longPressEventInterval)
            }
            onLongPressListener?.onRelease(this@LongPressImageView)
        }
    }

    interface OnLongPressListener {
        fun onLongPress(view: LongPressImageView)

        fun onRelease(view: LongPressImageView)
    }
}