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

package cc.chenhe.weargallery.utils

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import cc.chenhe.weargallery.common.util.SpBooleanLiveData
import cc.chenhe.weargallery.common.util.SpIntLiveData
import kotlin.math.min
import kotlin.math.roundToInt

private const val PREFERENCE_LAST_START_VERSION = "last_start_version"
private const val PREFERENCE_IMAGE_COLUMN_WIDTH = "image_column_width" // int

private const val PREFERENCE_TIP_WITH_WATCH = "tip_with_watch" // boolean
private const val PREFERENCE_FOREGROUND_SERVICE = "foreground_service" // boolean
private const val PREFERENCE_CHECK_UPDATE_TIME = "check_update_time" // long unix second


private fun getSp(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

fun setLastStartVersion(context: Context, version: Long) {
    getSp(context).edit {
        putLong(PREFERENCE_LAST_START_VERSION, version)
    }
}

fun lastCheckUpdateTime(context: Context): Long =
    getSp(context).getLong(PREFERENCE_CHECK_UPDATE_TIME, 0L)

fun lastCheckUpdateTime(context: Context, timeInSecond: Long) {
    getSp(context).edit {
        putLong(PREFERENCE_CHECK_UPDATE_TIME, timeInSecond)
    }
}

fun getLastStartVersion(context: Context): Long =
    getSp(context).getLong(PREFERENCE_LAST_START_VERSION, 0)

fun isTipWithWatch(context: Context): Boolean =
    getSp(context).getBoolean(PREFERENCE_TIP_WITH_WATCH, true)

fun getImageColumnWidth(context: Context): Int {
    val m = context.resources.displayMetrics
    val default = min(m.widthPixels, m.heightPixels) / 3
    return getSp(context).getInt(PREFERENCE_IMAGE_COLUMN_WIDTH, default)
}

fun fetchImageColumnWidth(context: Context): SpIntLiveData {
    val m = context.resources.displayMetrics
    val default = min(m.widthPixels, m.heightPixels) / 3
    return SpIntLiveData(getSp(context), PREFERENCE_IMAGE_COLUMN_WIDTH, default, true)
}

fun fetchForegroundService(context: Context, init: Boolean = false): SpBooleanLiveData {
    return SpBooleanLiveData(getSp(context), PREFERENCE_FOREGROUND_SERVICE, false, init)
}

fun addImageColumnCount(context: Context) {
    val old = getImageColumnWidth(context)
    getSp(context).edit {
        putInt(PREFERENCE_IMAGE_COLUMN_WIDTH, (old / 1.25f).toInt())
    }
}

fun minusImageColumnCount(context: Context) {
    val old = getImageColumnWidth(context)
    val new = old * 1.25f
    val m = context.resources.displayMetrics
    val w = min(m.widthPixels, m.heightPixels)
    if (w.toFloat() / new >= 2) {
        getSp(context).edit {
            putInt(PREFERENCE_IMAGE_COLUMN_WIDTH, new.toInt())
        }
    }
}

fun calculateImageColumnCount(listWidth: Int, itemWidth: Int): Int {
    var r = (listWidth.toFloat() / itemWidth).roundToInt()
    if (r < 2) {
        r = 2
    }
    return r
}
