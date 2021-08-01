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

package cc.chenhe.weargallery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.comm.*
import cc.chenhe.weargallery.common.comm.bean.ImageHdReq
import cc.chenhe.weargallery.common.comm.bean.ImagePreviewReq
import cc.chenhe.weargallery.common.comm.bean.ImagesReq
import cc.chenhe.weargallery.common.util.HUA_WEI
import cc.chenhe.weargallery.common.util.ImageUtil
import cc.chenhe.weargallery.common.util.checkHuaWei
import cc.chenhe.weargallery.common.util.fromJsonQ
import cc.chenhe.weargallery.ui.main.MainAty
import cc.chenhe.weargallery.utils.*
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.MessageEvent
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import id.zelory.compressor.constraint.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.chenhe.lib.wearmsger.BothWayHub
import me.chenhe.lib.wearmsger.service.WMListenerService
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

private const val TAG = "WearListenerService"

class WearListenerService : WMListenerService() {

    private val moshi: Moshi by inject()

    // -------------------------------------------------------------------------------------------------
    // Check and Notify
    // -------------------------------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createPermissionNotificationChannel()
    }

    /**
     * Check the necessary permissions, fire a notification if not.
     *
     * @return Whether we have been authorized.
     */
    private fun checkPermissions(): Boolean {
        if (checkStoragePermissions(this)) {
            return true
        }
        val intent = Intent(this, MainAty::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notify = NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_PERMISSION)
            .setSmallIcon(R.drawable.ic_notify_permission)
            .setContentTitle(getString(R.string.notify_permission_title))
            .setContentText(getString(R.string.notify_permission_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTicker(getString(R.string.notify_permission_title))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(NOTIFY_ID_PERMISSION, notify)
        return false
    }

    /**
     * Check the Huawei device, fire a notification if yes.
     *
     * @return Whether it is a Huawei device.
     */
    private fun checkHuaWeiDevice(): Boolean {
        if (checkHuaWei()) {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(HUA_WEI)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val notify = NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_PERMISSION)
                .setSmallIcon(R.drawable.ic_notify_permission)
                .setContentTitle(getString(R.string.notify_huawei_title))
                .setContentText(getString(R.string.notify_huawei_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(this).notify(NOTIFY_ID_PERMISSION, notify)
            return true
        }
        return false
    }

    private fun createPermissionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFY_CHANNEL_ID_PERMISSION,
                getString(R.string.notify_channel_permission_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notify_channel_permission_description)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private suspend fun toastIfEnabled(@StringRes resId: Int) {
        if (isTipWithWatch(this)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@WearListenerService, resId, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Processing
    // -----------------------------------------------------------------------------------------------

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Timber.tag(TAG).d("onMessageReceived, path=${messageEvent.path}")
        if (checkHuaWeiDevice()) {
            Timber.tag(TAG).i("This is a Huawei device, discard msg request.")
            return
        }
        if (!checkPermissions()) {
            Timber.tag(TAG).w("Missing necessary permissions, discard msg request.")
            return
        }
        when (messageEvent.path) {
            PATH_REQ_IMAGE_FOLDERS -> processRequestImageFolders(messageEvent)
            PATH_REQ_IMAGE_PREVIEW -> processRequestImagePreview(messageEvent)
            PATH_REQ_IMAGES -> processRequestImages(messageEvent)
            PATH_REQ_IMAGE_HD -> processRequestImageHd(messageEvent)
        }
    }

    private fun processRequestImageFolders(request: MessageEvent) {
        lifecycleScope.launch {
            toastIfEnabled(R.string.watch_operation_search_gallery_ing)
            val folders = queryImageFolders(this@WearListenerService)
            val type = Types.newParameterizedType(List::class.java, RemoteImageFolder::class.java)
            val adapter: JsonAdapter<List<RemoteImageFolder>> = moshi.adapter(type)
            BothWayHub.response(this@WearListenerService, request, adapter.toJson(folders))
        }
    }

    private fun processRequestImagePreview(request: MessageEvent) {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = moshi.adapter(ImagePreviewReq::class.java).fromJsonQ(String(request.data))
                ?: return@launch
            val resp = BothWayHub.obtainResponseDataRequest(request)
            val startTime = SystemClock.uptimeMillis()

            var compressed: File? = null
            try {
                @Suppress("DEPRECATION") // require api 30
                compressed = compressImage(this@WearListenerService, data.uri) {
                    resolution(500, 500)
                    format(Bitmap.CompressFormat.WEBP)
                    quality(60)
                }
                val b = compressed.readBytes()
                Timber.tag(TAG)
                    .v("Compress complete, uri=${data.uri}, size=${b.size / 1024}KB, time=${SystemClock.uptimeMillis() - startTime}")
                resp.dataMap.apply {
                    putInt(ITEM_RESULT, RESULT_OK)
                    putAsset(ITEM_IMAGE, Asset.createFromBytes(b))
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Compress image error. uri=${data.uri}")
                resp.dataMap.putInt(ITEM_RESULT, RESULT_ERROR)
            } finally {
                compressed?.delete()
                BothWayHub.response(this@WearListenerService, resp)
            }
        }
    }

    private fun processRequestImages(request: MessageEvent) {
        lifecycleScope.launch(Dispatchers.IO) {
            toastIfEnabled(R.string.watch_operation_get_pics_list_ing)
            val data = moshi.adapter(ImagesReq::class.java).fromJsonQ(String(request.data))
                ?: return@launch
            val images = ImageUtil.queryBucketImages(this@WearListenerService, data.bucketId)
            val type = Types.newParameterizedType(List::class.java, Image::class.java)
            val adapter: JsonAdapter<List<Image>> = moshi.adapter(type)
            BothWayHub.response(this@WearListenerService, request, adapter.toJson(images))
        }
    }

    private fun processRequestImageHd(request: MessageEvent) {
        lifecycleScope.launch(Dispatchers.IO) {
            toastIfEnabled(R.string.watch_operation_send_hd_picture_ing)
            val data = moshi.adapter(ImageHdReq::class.java).fromJsonQ(String(request.data))
                ?: return@launch
            val resp = BothWayHub.obtainResponseDataRequest(request)
            try {
                // Test if file is exist and make sure it will be closed with `use`.
                this@WearListenerService.contentResolver.openAssetFileDescriptor(data.uri, "r")
                    .use {}
                resp.dataMap.apply {
                    putAsset(ITEM_IMAGE, createImageAsset(data.uri))
                    putInt(ITEM_RESULT, RESULT_OK)
                }
            } catch (e: FileNotFoundException) {
                toastIfEnabled(R.string.watch_operation_send_hd_picture_fail)
                Timber.tag(TAG).w("Image file not exist, uri=%s", data.uri)
                resp.dataMap.putInt(ITEM_RESULT, RESULT_ERROR)
            } catch (e: Exception) {
                toastIfEnabled(R.string.watch_operation_send_hd_picture_fail)
                resp.dataMap.putInt(ITEM_RESULT, RESULT_ERROR)
                e.printStackTrace()
            }
            BothWayHub.response(this@WearListenerService, resp)
        }
    }

    private suspend fun createImageAsset(uri: Uri): Asset = withContext(Dispatchers.IO) {
        Asset.createFromUri(uri)
    }

}