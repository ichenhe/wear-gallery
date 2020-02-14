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
import android.util.Log
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.common.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MIME_GIF = "image/gif"

const val NOTIFY_CHANNEL_ID_PERMISSION = "wg.permission"
const val NOTIFY_CHANNEL_ID_SENDING = "wg.send_images"
const val NOTIFY_CHANNEL_ID_SEND_RESULT = "wg.send_images_result"

const val NOTIFY_ID_PERMISSION = 1
const val NOTIFY_ID_SENDING = 2
const val NOTIFY_ID_SEND_RESULT = 3

const val UPDATE_URL = "http://wg.chenhe.cc"

fun Toolbar.getTitleTextView(): TextView? {
    var tv: TextView? = null
    try {
        this.javaClass.getDeclaredField("mTitleTextView").let { field ->
            field.isAccessible = true
            tv = field.get(this) as TextView?
        }
    } catch (e: Exception) {
    }
    return tv
}

@ColorInt
fun @receiver:ColorInt Int.setAlpha(@IntRange(from = 0, to = 255) alpha: Int): Int {
    return (alpha and 0xff).shl(24) and this
}

fun checkStoragePermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context,
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

suspend fun queryImageFolders(context: Context): List<RemoteImageFolder> = withContext(Dispatchers.Default) {
    val folders = mutableListOf<RemoteImageFolder>()
    ImageUtil.groupImagesByFolder(ImageUtil.queryImages(context)).forEach {
        if (it.children.isNotEmpty()) {
            folders += RemoteImageFolder(
                    bucketId = it.bucketId,
                    bucketName = it.bucketName,
                    imageCount = it.children.size,
                    previewUri = it.children.first().uri,
                    latestTime = it.children.first().takenTime
            )
        }
    }
    folders
}

// -------------------------------------------------------------------------------------
// Logs
// -------------------------------------------------------------------------------------

private const val TAG = "WearGalleryM"

internal fun logd(tag: String, msg: String) {
    Log.d(TAG, "[$tag] $msg")
}

internal fun logi(tag: String, msg: String) {
    Log.i(TAG, "[$tag] $msg")
}

internal fun loge(tag: String, msg: String) {
    Log.e(TAG, "[$tag] $msg")
}

internal fun logw(tag: String, msg: String) {
    Log.w(TAG, "[$tag] $msg")
}
