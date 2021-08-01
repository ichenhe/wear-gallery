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

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.comm.*
import cc.chenhe.weargallery.common.comm.bean.SendResp
import cc.chenhe.weargallery.common.util.fromJsonQ
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.ui.main.MainAty
import cc.chenhe.weargallery.uilts.NOTIFY_CHANNEL_ID_PERMISSION
import cc.chenhe.weargallery.uilts.NOTIFY_ID_PERMISSION
import cc.chenhe.weargallery.uilts.checkStoragePermissions
import com.google.android.gms.wearable.DataMapItem
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.chenhe.lib.wearmsger.BothWayHub
import me.chenhe.lib.wearmsger.DataHub
import me.chenhe.lib.wearmsger.service.WMListenerService
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

private const val TAG = "MobileListenerService"

class MobileListenerService : WMListenerService() {

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

        @SuppressLint("UnspecifiedImmutableFlag")
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, 0)
        }
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

    // -----------------------------------------------------------------------------------------------
    // Processing
    // -----------------------------------------------------------------------------------------------

    override fun onDataChanged(dataMapItem: DataMapItem, id: Long) {
        super.onDataChanged(dataMapItem, id)
        val path = dataMapItem.uri.path
        Timber.tag(TAG).d("onDataChanged path=%s", path)
        when (path) {
            PATH_SEND_IMAGE -> {
                processSendImage(dataMapItem)
            }
        }
    }

    private fun processSendImage(dataMapItem: DataMapItem) {
        val context = this
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            val permission = withContext(Dispatchers.Main) {
                checkPermissions()
            }
            if (!permission) {
                // No permission
                val data =
                    moshi.adapter(SendResp::class.java).toJson(SendResp(RESULT_NO_PERMISSION))
                BothWayHub.response(context, dataMapItem, data)
                return@launch
            }

            val imageRepo: ImageRepository = get()
            val map = dataMapItem.getDataMap()
            val info = map.getString(ITEM_IMAGE_INFO)
                ?.let { moshi.adapter(Image::class.java).fromJsonQ(it) }
            if (info == null) {
                // No image info
                val data = moshi.adapter(SendResp::class.java).toJson(SendResp(RESULT_ERROR))
                BothWayHub.response(context, dataMapItem, data)
                return@launch
            }
            map.getAsset(ITEM_IMAGE)?.let { asset ->
                DataHub.getInputStreamForAsset(context, asset)
            }?.use { ins ->
                val uri = imageRepo.saveImage(
                    context,
                    info.name,
                    info.takenTime,
                    ins,
                    map.getString(ITEM_SAVE_PATH)
                )
                if (uri == null) {
                    // Save error
                    val data = moshi.adapter(SendResp::class.java).toJson(SendResp(RESULT_ERROR))
                    BothWayHub.response(context, dataMapItem, data)
                    return@launch
                } else {
                    // OK
                    val data = moshi.adapter(SendResp::class.java).toJson(SendResp(RESULT_OK))
                    BothWayHub.response(context, dataMapItem, data)
                    return@launch
                }
            } ?: kotlin.run {
                // No file stream
                val data = moshi.adapter(SendResp::class.java).toJson(SendResp(RESULT_ERROR))
                BothWayHub.response(context, dataMapItem, data)
                return@launch
            }
        }
    }
}