/*
 * Copyright (c) 2020 Chenhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.chenhe.weargallery.wearvision.preference

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.preference.DialogPreference
import cc.chenhe.weargallery.wearvision.R
import cc.chenhe.weargallery.wearvision.util.TypedArrayUtils

/**
 * A base class for [androidx.preference.Preference]s that are dialog-based. When clicked, these preferences will open
 * a dialog showing the actual preference controls.
 *
 * Compared to androidx, this class supports the following additional attributes:
 *
 * - [R.attr.wv_positiveButtonIcon]
 * - [R.attr.wv_negativeButtonIcon]
 *
 * @see [androidx.preference.DialogPreference]
 */
abstract class DialogPreference : DialogPreference {

    var positiveButtonIcon: Drawable? = null
    var negativeButtonIcon: Drawable? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs,
            TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle))

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    @SuppressLint("Recycle")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes) {
        context.obtainStyledAttributes(attrs, R.styleable.DialogPreference, defStyleAttr, defStyleRes).use {
            positiveButtonIcon = it.getDrawable(R.styleable.DialogPreference_wv_positiveButtonIcon)
            negativeButtonIcon = it.getDrawable(R.styleable.DialogPreference_wv_negativeButtonIcon)
        }
    }

    fun setPositiveButtonIcon(@DrawableRes iconRes: Int) {
        positiveButtonIcon = context.getDrawable(iconRes)
    }

    fun setNegativeButtonIcon(@DrawableRes iconRes: Int) {
        negativeButtonIcon = context.getDrawable(iconRes)
    }
}