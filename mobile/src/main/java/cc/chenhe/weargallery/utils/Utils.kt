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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.common.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Minimum version required for pairing applications. */
const val MIN_PAIRED_VERSION = 220601061L

const val MIME_GIF = "image/gif"


/** LocalBroadcast with extra 'success' which is a boolean value.*/
const val ACTION_APP_UPGRADE_COMPLETE = "ACTION_APP_UPGRADE_COMPLETE"

fun Toolbar.getTitleTextView(): TextView? {
    var tv: TextView? = null
    try {
        this.javaClass.getDeclaredField("mTitleTextView").let { field ->
            field.isAccessible = true
            tv = field.get(this) as TextView?
        }
    } catch (_: Exception) {
    }
    return tv
}

@ColorInt
fun @receiver:ColorInt Int.setAlpha(@IntRange(from = 0, to = 255) alpha: Int): Int {
    return (alpha and 0xff).shl(24) and this
}

fun requiredStoragePermission(): String = if (Build.VERSION.SDK_INT >= 33) {
    Manifest.permission.READ_MEDIA_IMAGES
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

fun checkStoragePermissions(context: Context): Boolean = ContextCompat.checkSelfPermission(
    context,
    requiredStoragePermission()
) == PackageManager.PERMISSION_GRANTED

suspend fun queryImageFolders(context: Context): List<RemoteImageFolder> =
    withContext(Dispatchers.Default) {
        val folders = ImageUtil.queryImageFolders(context)
        val result = List(folders.size) {
            RemoteImageFolder(
                folders[it].id,
                folders[it].name,
                folders[it].imgNum,
                folders[it].preview.uri,
                folders[it].latestTime,
            )
        }
        result
    }

fun generateRandomString(length: Int): String {
    val nonceScope = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val scopeSize = nonceScope.length
    val nonceItem: (Int) -> Char = { nonceScope[(scopeSize * Math.random()).toInt()] }
    return Array(length, nonceItem).joinToString("")
}

