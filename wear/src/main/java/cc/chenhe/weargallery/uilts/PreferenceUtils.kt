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

package cc.chenhe.weargallery.uilts

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.SpBooleanLiveData
import cc.chenhe.weargallery.common.util.SpFloatLiveData
import cc.chenhe.weargallery.common.util.SpIntLiveData
import cc.chenhe.weargallery.common.util.SpStringLiveData

private const val PREFERENCE_SHOW_HW = "show_hw" // boolean
const val PREFERENCE_KEEP_SCREEN_ON = "keep_screen_on" // boolean
private const val PREFERENCE_SHOW_PHONE_IMAGES = "show_phone_images" // boolean
private const val LAST_START_VERSION = "last_start_version_l" // long

private fun getSp(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

fun showHuawei(context: Context): Boolean = getSp(context).getBoolean(PREFERENCE_SHOW_HW, true)

fun showHuawei(context: Context, show: Boolean) {
    getSp(context).edit {
        putBoolean(PREFERENCE_SHOW_HW, show)
    }
}

fun fetchKeepScreenOn(context: Context) = SpBooleanLiveData(getSp(context), PREFERENCE_KEEP_SCREEN_ON, false)

fun fetchShowPhoneImages(context: Context) = SpBooleanLiveData(getSp(context), PREFERENCE_SHOW_PHONE_IMAGES,
        default = true, init = true)

fun lastStartVersion(context: Context, version: Long) {
    getSp(context).edit {
        putLong(LAST_START_VERSION, version)
    }
}

fun lastStartVersion(context: Context): Long {
    return getSp(context).getLong(LAST_START_VERSION, 0)
}


// ----------------------------------------------------------------------------------------------
// Watch Face
// ----------------------------------------------------------------------------------------------

// --------------------------
// -     Time Text Tag      -
// --------------------------

const val TIME_TAG_LINE = "{nl}"
const val TIME_TAG_HOUR = "{h}"
const val TIME_TAG_MIN = "{m}"
const val TIME_TAG_COLON = "{colon}"
const val TIME_TAG_YEAR = "{y}"
const val TIME_TAG_MONTH = "{mo}"
const val TIME_TAG_DAY = "{d}"
const val TIME_TAG_SLASH = "{slash}"

private const val PREFERENCE_WF_STYLE = "watchface_type"

/** Float. Default = -1f means center. */
private const val PREFERENCE_WF_TIME_X = "watchface_time_x"

/** Float. Default = -1f means center. */
private const val PREFERENCE_WF_TIME_Y = "watchface_time_y"
private const val PREFERENCE_WF_TIME_TEXT_SIZE = "watchface_time_text_size" // Float
private const val PREFERENCE_WF_TIME_TEXT_COLOR = "watchface_time_text_color" // Int

/**
 * String.
 *
 * The value should be a string consisting of tags that are listed as constants `TIME_TAG_*`.
 *
 * e.g. `{h}{colon}{m}`
 */
private const val PREFERENCE_WF_TIME_FORMAT = "watchface_time_format"

private const val DEFAULT_WF_TIME_FORMAT = TIME_TAG_HOUR + TIME_TAG_COLON + TIME_TAG_MIN
fun fetchWatchFaceStyle(context: Context, init: Boolean) = SpStringLiveData(getSp(context), PREFERENCE_WF_STYLE,
        context.getString(R.string.wf_preference_type_entry_value_analog), init)

fun fetchWatchFaceTimeX(context: Context, init: Boolean) =
        SpFloatLiveData(getSp(context), PREFERENCE_WF_TIME_X, -1f, init)

fun fetchWatchFaceTimeY(context: Context, init: Boolean) =
        SpFloatLiveData(getSp(context), PREFERENCE_WF_TIME_Y, -1f, init)

fun fetchWatchFaceTimeTextSize(context: Context, init: Boolean) =
        SpFloatLiveData(getSp(context), PREFERENCE_WF_TIME_TEXT_SIZE,
                context.resources.getDimension(R.dimen.wv_text_size_extra_extra_large), init)

fun setWatchFaceTimePosition(context: Context, x: Float, y: Float) {
    getSp(context).edit {
        putFloat(PREFERENCE_WF_TIME_X, x)
        putFloat(PREFERENCE_WF_TIME_Y, y)
    }
}

fun setWatchFaceTextSize(context: Context, size: Float) {
    getSp(context).edit {
        putFloat(PREFERENCE_WF_TIME_TEXT_SIZE, size)
    }
}

fun getWatchFaceTimeFormat(context: Context): List<String> = parseWatchFaceTagList(getSp(context)
        .getString(PREFERENCE_WF_TIME_FORMAT, DEFAULT_WF_TIME_FORMAT)!!)

fun setWatchFaceTimeFormat(context: Context, value: List<String>) {
    getSp(context).edit {
        putString(PREFERENCE_WF_TIME_FORMAT, value.joinToString(separator = ""))
    }
}

fun fetchWatchFaceTimeFormat(context: Context, init: Boolean) =
        SpStringLiveData(getSp(context), PREFERENCE_WF_TIME_FORMAT, DEFAULT_WF_TIME_FORMAT, init)

fun resetWatchFaceTimeFormat(context: Context) {
    getSp(context).edit {
        putString(PREFERENCE_WF_TIME_FORMAT, DEFAULT_WF_TIME_FORMAT)
    }
}

fun fetchWatchFaceTimeTextColor(context: Context, init: Boolean) =
        SpIntLiveData(getSp(context), PREFERENCE_WF_TIME_TEXT_COLOR, Color.BLACK, init)

fun getWatchFaceTimeTextColor(context: Context): Int = getSp(context)
        .getInt(PREFERENCE_WF_TIME_TEXT_COLOR, Color.BLACK)

fun setWatchFaceTimeTextColor(context: Context, @ColorInt newColor: Int) {
    getSp(context).edit {
        putInt(PREFERENCE_WF_TIME_TEXT_COLOR, newColor)
    }
}

fun parseWatchFaceTagList(value: String): List<String> {
    val builder = StringBuilder(10)
    val tags = mutableListOf<String>()
    value.forEach { c ->
        if (c == '{') {
            builder.clear()
            builder.append(c)
        } else {
            if (builder.isNotEmpty()) {
                // at least one {
                builder.append(c)
                if (c == '}') {
                    tags.add(builder.toString())
                    builder.clear()
                }
            }
        }
    }
    return tags
}
