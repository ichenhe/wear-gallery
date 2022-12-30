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

package cc.chenhe.weargallery.ui.sendimages

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.*
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.comm.CAP_WEAR
import cc.chenhe.weargallery.common.util.ImageExifUtil
import cc.chenhe.weargallery.common.util.fileName
import cc.chenhe.weargallery.common.util.filePath
import cc.chenhe.weargallery.service.SendPicturesService
import cc.chenhe.weargallery.ui.common.getContext
import cc.chenhe.weargallery.utils.*
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.CHANNEL_ID_SENDING
import cc.chenhe.weargallery.utils.NotificationUtils.Companion.CHANNEL_ID_SEND_RESULT
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class SendImagesUiState(
    val targetFolder: String? = null,
    val devices: List<SendImagesViewModel.NodeInfo> = emptyList(),
    val targetDevice: SendImagesViewModel.NodeInfo? = null,
    val images: List<Image> = emptyList(),
    val notificationPermission: PermissionState = PermissionState.Granted
) {
    sealed class PermissionState {
        object Granted : PermissionState()
        object NotGranted : PermissionState()
        object Disabled : PermissionState()
        class ChannelDisabled(val channelId: String) : PermissionState()
    }
}

sealed class SendImagesIntent {
    data class SelectTargetFolder(val folder: String?) : SendImagesIntent()
    data class SetTargetDevice(val device: SendImagesViewModel.NodeInfo?) : SendImagesIntent()
    object CheckNotificationPermission : SendImagesIntent()
    object StartSend : SendImagesIntent()
}

class SendImagesViewModel(
    application: Application,
    private val notificationChecker: NotificationChecker,
    intent: Intent,
) :
    AndroidViewModel(application) {
    companion object {
        private const val TAG = "SendImagesViewModel"

        fun isFolderPathValid(s: String?): Boolean {
            return s.isNullOrEmpty() || s.matches(Regex("[^\\\\<>*?|\"]+")) && s.isNotBlank()
        }
    }

    data class NodeInfo(val id: String, val name: String)

    private var _uiState = mutableStateOf(SendImagesUiState())
    val uiState: State<SendImagesUiState> = _uiState

    private val intents: MutableSharedFlow<SendImagesIntent> = MutableSharedFlow()

    /**
     * Sending or send complete
     */
    private val sending: AtomicBoolean = AtomicBoolean(false)

    private val capChangedListener = CapabilityClient.OnCapabilityChangedListener {
        updateDeviceList(it.nodes)
    }
    private val capClient = Wearable.getCapabilityClient(getContext())

    init {
        viewModelScope.launch {
            updateDeviceList(getDevices())
            _uiState.value =
                _uiState.value.copy(images = extractImagesFromIntent(intent) ?: emptyList())
        }
        capClient.addListener(capChangedListener, CAP_WEAR)
        subscribeIntent()
    }

    override fun onCleared() {
        capClient.removeListener(capChangedListener)
        super.onCleared()
    }

    fun sendIntent(intent: SendImagesIntent) {
        viewModelScope.launch {
            intents.emit(intent)
        }
    }

    private fun subscribeIntent() {
        viewModelScope.launch {
            intents.collect { intent ->
                when (intent) {
                    is SendImagesIntent.SelectTargetFolder -> updateTargetFolder(intent.folder)
                    is SendImagesIntent.SetTargetDevice -> updateTargetDevice(intent.device)
                    SendImagesIntent.CheckNotificationPermission -> checkPermission()
                    SendImagesIntent.StartSend -> startSend()
                }
            }
        }
    }

    private fun updateTargetFolder(folder: String?) {
        _uiState.value = _uiState.value.copy(
            targetFolder = folder?.takeIf { it.isNotEmpty() && it.isNotBlank() }
        )
    }

    private fun updateTargetDevice(device: NodeInfo?) {
        _uiState.value = _uiState.value.copy(
            targetDevice = device?.takeIf { _uiState.value.devices.contains(device) }
        )
    }

    private fun checkPermission() {
        if (!notificationChecker.hasNotificationPermission()) {
            _uiState.value = _uiState.value.copy(
                notificationPermission = SendImagesUiState.PermissionState.NotGranted
            )
            return
        }
        if (!notificationChecker.areNotificationsEnabled()) {
            _uiState.value = _uiState.value.copy(
                notificationPermission = SendImagesUiState.PermissionState.Disabled
            )
            return
        }
        if (!notificationChecker.isNotificationChannelEnabled(CHANNEL_ID_SENDING)) {
            _uiState.value = _uiState.value.copy(
                notificationPermission = SendImagesUiState.PermissionState.ChannelDisabled(
                    CHANNEL_ID_SENDING
                )
            )
            return
        }
        if (!notificationChecker.isNotificationChannelEnabled(CHANNEL_ID_SEND_RESULT)) {
            _uiState.value = _uiState.value.copy(
                notificationPermission = SendImagesUiState.PermissionState.ChannelDisabled(
                    CHANNEL_ID_SEND_RESULT
                )
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            notificationPermission = SendImagesUiState.PermissionState.Granted
        )
    }

    private suspend fun startSend() {
        _uiState.value.apply {
            val device = getDevices()?.find { it.id == targetDevice?.id }
            val images = _uiState.value.images
            if (device != null && images.isNotEmpty()) {
                if (sending.compareAndSet(false, true)) {
                    SendPicturesService.add(getApplication(), images, device, targetFolder)
                }
            }
        }
    }

    private suspend fun extractImagesFromIntent(intent: Intent): List<Image>? {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelable<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayList(Intent.EXTRA_STREAM)
            }
            else -> {
                Timber.tag(TAG).w("Unknown intent, action=%s", intent.action)
                null
            }
        }
        if (uris.isNullOrEmpty()) return null
        return uris.mapNotNull { extractImageFromUri(it) }
    }

    private suspend fun getDevices(): Set<Node>? {
        return try {
            // It will throw a exception if cannot connect to wearable client
            Wearable.getCapabilityClient(getContext())
                .getCapability(CAP_WEAR, CapabilityClient.FILTER_REACHABLE).await()?.nodes
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update device list and select the first one as default if
     * [SendImagesUiState.targetDevice] is null.
     *
     * If current selected device is no more available, change it to the first one.
     */
    private fun updateDeviceList(nodes: Set<Node>?) {
        val nodeList = nodes?.mapTo(ArrayList()) { NodeInfo(it.id, it.displayName) }
            ?.apply { sortBy { it.name } } ?: emptyList()
        val selected = _uiState.value.targetDevice
            ?.takeIf { t -> nodeList.find { it.id == t.id } != null }
            ?: nodeList.firstOrNull()
        _uiState.value = _uiState.value.copy(devices = nodeList, targetDevice = selected)
    }

    private suspend fun extractImageFromUri(uri: Uri): Image? {
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                var img = querySignalImage(uri)
                if (img != null) {
                    if (img.mime?.startsWith("image/") != true) {
                        Timber.tag(TAG).d("Not a image, discard. mime=${img.mime}, uri=$uri")
                        return null
                    }
                    return img
                }
                Timber.tag(TAG)
                    .d("Cannot find a record for shared uri, try to parse stream. uri=$uri")
                img = ImageExifUtil.parseImageFromIns(getContext(), uri)
                if (img != null) return img
                Timber.tag(TAG).i("Cannot parse stream to a image, discard. uri=$uri")
                return null
            }
            ContentResolver.SCHEME_FILE -> {
                return ImageExifUtil.parseImageFromFile(File(requireNotNull(uri.path)))
            }
            else -> {
                Timber.tag(TAG).i("Unrecognized uri - not content or file. uri=$uri")
                return null
            }
        }
    }

    @Suppress("DEPRECATION") // We use `DATA` field to show file path information.
    private suspend fun querySignalImage(uri: Uri): Image? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )

        @Suppress("DEPRECATION") // We use `DATA` field to show file path information.
        return@withContext getContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    val dateModifiedIndex =
                        cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                    val dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val widthIndex = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                    val heightIndex = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                    val mimeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val bucketNameIndex =
                        cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)

                    val file: String? = if (dataIndex >= 0) cursor.getString(dataIndex) else null
                    return@withContext Image(
                        uri = uri,
                        name = cursor.getStringOrNull(nameIndex) ?: file?.fileName ?: "",
                        takenTime = cursor.getLongOrNull(dateTakenIndex) ?: 0L,
                        modifiedTime = cursor.getLongOrNull(dateModifiedIndex) ?: 0L,
                        addedTime = cursor.getLongOrNull(dateAddedIndex) ?: 0L,
                        size = cursor.getLongOrNull(sizeIndex) ?: 0L,
                        width = cursor.getIntOrNull(widthIndex) ?: 0,
                        height = cursor.getIntOrNull(heightIndex) ?: 0,
                        mime = cursor.getStringOrNull(mimeIndex),
                        bucketName = cursor.getStringOrNull(bucketNameIndex)
                            ?: file?.filePath?.fileName
                            ?: "",
                        bucketId = cursor.getIntOrNull(bucketIndex) ?: -1,
                        file = file
                    )
                } else {
                    null
                }
            }
    }
}