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

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.ImageView
import androidx.core.content.res.use
import cc.chenhe.weargallery.common.R

/**
 * An [ImageView] which can show a mask color if it was marked as checked.
 */
class MaskImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.maskImageViewStyle,
        defStyleRes: Int = R.style.DefaultMaskImageViewStyle
) : ImageView(context, attrs, defStyleAttr, defStyleRes), Checkable {

    var checkedColor: Int = 0
        set(value) {
            if (value != field) {
                field = value
                if (mChecked) {
                    invalidate()
                }
            }
        }

    private var mChecked = false
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MaskImageView, defStyleAttr, defStyleRes).use {
            checkedColor = it.getColor(R.styleable.MaskImageView_checkedColor, 0)
            mChecked = it.getBoolean(R.styleable.MaskImageView_android_checked, false)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mChecked) {
            canvas.drawColor(checkedColor)
        }
    }

    // --------------------------------------------
    // Checkable
    // --------------------------------------------

    override fun isChecked(): Boolean = mChecked

    override fun toggle() {
        mChecked = !mChecked
    }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
    }
}