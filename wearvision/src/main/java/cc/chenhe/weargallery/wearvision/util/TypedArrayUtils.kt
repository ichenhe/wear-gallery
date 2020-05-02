/*
 * Copyright (c) 2020 Chenhe
 * Copyright (C) 2015 The Android Open Source Project
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

package cc.chenhe.weargallery.wearvision.util

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.StyleableRes

/**
 * Compat methods for accessing TypedArray values.
 *
 * All the getNamed*() functions added the attribute name match, to take care of potential ID
 * collision between the private attributes in older OS version (OEM) and the attributes existed in
 * the newer OS version.
 * For example, if an private attribute named "abcdefg" in Kitkat has the
 * same id value as "android:pathData" in Lollipop, we need to match the attribute's namefirst.
 */
object TypedArrayUtils {

    /**
     * @return a boolean value of `index`. If it does not exist, a boolean value of `fallbackIndex`. If it still does
     * not exist, `defaultValue`.
     */
    fun getBoolean(a: TypedArray, @StyleableRes index: Int, @StyleableRes fallbackIndex: Int,
                   defaultValue: Boolean): Boolean {
        val value = a.getBoolean(fallbackIndex, defaultValue)
        return a.getBoolean(index, value)
    }

    /**
     * Retrieves a string array attribute value with the specified fallback ID.
     *
     * @return A string array value of `index`. If it does not exist, a string array value  of `fallbackIndex`. If it
     * still does not exist, `null`.
     */
    fun getTextArray(a: TypedArray, @StyleableRes index: Int,
                     @StyleableRes fallbackIndex: Int): Array<CharSequence>? {
        return a.getTextArray(index) ?: a.getTextArray(fallbackIndex)
    }

    /**
     * @return The resource ID value in the `context` specified by `attr`. If it does not exist, `fallbackAttr`.
     */
    fun getAttr(context: Context, attr: Int, fallbackAttr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) {
            attr
        } else fallbackAttr
    }

}