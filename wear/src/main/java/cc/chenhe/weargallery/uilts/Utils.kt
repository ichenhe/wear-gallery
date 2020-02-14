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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.content.ContextCompat
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.common.bean.*
import cc.chenhe.weargallery.wearvision.dialog.AlertDialog
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

const val REQUEST_IMAGE_PREVIEW_TIMEOUT = 8000L
const val REQUEST_IMAGE_HD_TIMEOUT = 20000L

const val NOTIFY_CHANNEL_ID_PERMISSION = "wg.permission"
const val NOTIFY_ID_PERMISSION = 1

const val NOTIFY_CHANNEL_ID_UPGRADE = "wg.upgrade"
const val NOTIFY_ID_UPGRADE = 2

/**
 * Send a local broadcast when the watch face background picture changes and is ready.
 */
const val ACTION_WATCH_FACE_BACKGROUND_CHANGED = "watchFaceBackgroundChanged"

/**
 * Send a local broadcast when application upgrade completes.
 */
const val ACTION_APPLICATION_UPGRADE_COMPLETE = "applicationUpgradeComplete"

/**
 * Folder to save watch face resources.
 */
private const val WATCH_FACE_FOLDER_NAME = "WatchFace"

/**
 * Watch face background picture file name. This file should be saved in [getWatchFaceResFolder].
 */
const val WATCH_FACE_BACKGROUND = "background"

/**
 * The default folder where the received pictures are stored in. The HD version of the pictures are also stored here.
 * That means loading HD pictures and transferring pictures are equivalent.
 *
 * The real path should be [android.os.Environment.DIRECTORY_PICTURES]/[IMAGE_RECEIVE_FOLDER_NAME].
 */
const val IMAGE_RECEIVE_FOLDER_NAME = "WearGallery"

/**
 * A shortcut to get the relative path where the received images should be stored in.
 *
 * @see IMAGE_RECEIVE_FOLDER_NAME
 */
val imageReceiveRelativePath get() = Environment.DIRECTORY_PICTURES + File.separator + IMAGE_RECEIVE_FOLDER_NAME

/**
 * The scale of the change in a single zoom.
 *
 * `targetScale = currentScale *(/) IMAGE_ZOOM_SINGLE_SCALE`
 */
const val IMAGE_ZOOM_SINGLE_SCALE = 1.5f

/**
 * The scale of the change in a consecutive zoom.
 *
 * `targetScale = currentScale *(/) IMAGE_ZOOM_SINGLE_SCALE`
 */
const val IMAGE_ZOOM_CONSECUTIVE_SCALE = 1.05f

/** NOT equal here since requestLegacyExternalStorage=true. */
fun scopeStorageEnabled() = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q

fun shouldShowRetryLayout(resource: Resource<*>?): Boolean = resource is Error && resource.data.isNullOrEmpty()

fun shouldShowEmptyLayout(resource: Resource<*>?): Boolean = resource is Success && resource.data.isNullOrEmpty()

fun shouldShowLoadingLayout(resource: Resource<*>?): Boolean = resource is Loading && resource.data.isNullOrEmpty()

/**
 * Determine if an image has a local cache or file.
 *
 * @param data Can only be [RemoteImage] or [Image].
 */
fun hasLocalFile(data: Any?): Boolean {
    if (data == null) return false
    if (data is RemoteImage) return data.localUri != null
    if (data is Image) return true
    throw IllegalArgumentException("Unexpected class type.")
}

@OptIn(ExperimentalContracts::class)
fun Any?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || ((this is Collection<*> && this.isEmpty()))
}

/**
 * Obtain a folder file to save the watch face resource file.
 */
fun getWatchFaceResFolder(context: Context): File {
    val f = File(context.filesDir, WATCH_FACE_FOLDER_NAME)
    if (!f.isDirectory) {
        f.mkdirs()
    }
    return f
}

/**
 * Determine whether it is a Ticwear system.
 */
fun isTicwear(): Boolean {
    return getProperty("ticwear.version.name", "unknown") != "unknown"
}

@Suppress("SameParameterValue")
private fun getProperty(key: String, defaultValue: String): String {
    var value = defaultValue
    try {
        @SuppressLint("PrivateApi")
        val c = Class.forName("android.os.SystemProperties")
        val get = c.getMethod("get", String::class.java, String::class.java)
        value = get.invoke(c, key, "unknown") as String
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return value
}

fun checkStoragePermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context,
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("InflateParams")
fun AlertDialog.addQrCode(content: String) {
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_view, null, false).apply {
        findViewById<ImageView>(R.id.qrImage).setImageBitmap(ZxingUtils.generateBitmap(content, 200, 200))
    }
    setView(view)
}

// -------------------------------------------------------------------------------------
// Logs
// -------------------------------------------------------------------------------------

private const val TAG = "WearGalleryW"

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

