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

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.*
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.comm.CAP_WEAR
import cc.chenhe.weargallery.common.util.fileName
import cc.chenhe.weargallery.common.util.filePath
import cc.chenhe.weargallery.ui.common.getContext
import cc.chenhe.weargallery.utils.fetchImageColumnWidth
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class SendImagesViewModel(application: Application, intent: Intent) :
    AndroidViewModel(application) {

    companion object {
        private const val TAG = "SendImagesViewModel"
    }

    val columnWidth = fetchImageColumnWidth(application)

    var nodes: Set<Node> = emptySet()
        private set(value) {
            field = value
            val target = targetNode.value
            if (target != null) {
                if (!value.contains(target)) {
                    // The selected device is disconnected -> set as the first node or null
                    _targetNode.postValue(if (value.isEmpty()) null else value.first())
                }
            } else {
                // The default setting is the first node
                if (value.isNotEmpty())
                    _targetNode.postValue(value.first())
            }
        }

    private val _targetNode = MutableLiveData<Node?>(null)
    val targetNode: LiveData<Node?> = _targetNode

    private val _targetFolder = MutableLiveData<String?>(null)
    val targetFolder: LiveData<String?> = _targetFolder

    val images: LiveData<List<Image>?> = liveData {
        emit(processIntent(intent))
    }

    private val capChangedListener = CapabilityClient.OnCapabilityChangedListener {
        nodes = it.nodes ?: emptySet()
    }
    private val capClient = Wearable.getCapabilityClient(getContext())

    init {
        viewModelScope.launch {
            loadNodes()
        }
        capClient.addListener(capChangedListener, CAP_WEAR)
    }

    override fun onCleared() {
        capClient.removeListener(capChangedListener)
        super.onCleared()
    }

    private suspend fun processIntent(intent: Intent): List<Image>? {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            else -> {
                Timber.tag(TAG).w("Unknown intent, action=%s", intent.action)
                null
            }
        }
        if (uris.isNullOrEmpty()) return null
        val images = ArrayList<Image>(uris.size)
        uris.forEach { uri ->
            processUri(uri)?.also {
                images += it
            }
        }
        return images
    }

    private suspend fun loadNodes() {
        nodes = try {
            // It will throw a exception if cannot connect to wearable client
            val info = Wearable.getCapabilityClient(getContext())
                .getCapability(CAP_WEAR, CapabilityClient.FILTER_REACHABLE).await()
            info?.nodes ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun setTargetNode(node: Node) {
        if (nodes.contains(node)) {
            _targetNode.value = node
        } else {
            _targetNode.value = null
        }
    }

    /**
     * @return Whether the new path is valid and has been set.
     */
    fun setTargetFolder(s: String?): Boolean {
        if (!isFolderPathValid(s)) {
            return false
        }
        if (s == null || s.isEmpty() || s.isBlank()) {
            _targetFolder.value = null
        } else {
            _targetFolder.value = s
        }
        return true
    }

    private fun isFolderPathValid(s: String?): Boolean {
        return s.isNullOrEmpty() || s.matches(Regex("[^\\\\<>*?|\"]+")) && s.isNotBlank()
    }

    private suspend fun processUri(uri: Uri): Image? {

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            var img = querySignalImage(uri)
            if (img != null) return img
            Timber.tag(TAG).d("Cannot find a record for shared uri, try to parse stream. uri=$uri")
            img = parseImageFromIns(uri)
            if (img != null) return img
            Timber.tag(TAG).i("Cannot parse stream to a image, discard. uri=$uri")
            return null
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            return parseImageFromFile(File(requireNotNull(uri.path)))
        }
        Timber.tag(TAG).i("Unrecognized uri - not content or file. uri=$uri")
        return null
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
        return@withContext getContext().contentResolver.query(uri, projection, null, null, null)
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

    @SuppressLint("RestrictedApi") // For convenience's sake
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatchers
    private suspend fun parseImageFromIns(uri: Uri): Image? = withContext(Dispatchers.IO) {
        val cr = getContext().contentResolver
        var image = cr.openInputStream(uri)?.use { ins ->
            val exif = ExifInterface(ins)
            Image(
                uri,
                name = uri.toString().fileName,
                takenTime = exif.dateTimeOriginal ?: 0L,
                modifiedTime = exif.dateTime ?: 0L,
                addedTime = 0L,
                size = 0L,
                width = 0,
                height = 0,
                mime = cr.getType(uri),
                bucketId = -1,
                bucketName = uri.toString().filePath ?: "",
                file = null
            )
        } ?: return@withContext null

        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { ins ->
                BitmapFactory.decodeStream(ins, null, options)
                image = image.copy(
                    width = options.outWidth,
                    height = options.outHeight,
                    mime = if (image.mime == null) options.outMimeType else image.mime
                )
            }
        } catch (e: Exception) {
            // ignore
        }
        image
    }

    @SuppressLint("RestrictedApi") // For convenience's sake
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatchers
    private suspend fun parseImageFromFile(file: File): Image = withContext(Dispatchers.IO) {
        var image = Image(
            Uri.fromFile(file),
            name = file.name,
            takenTime = 0L,
            modifiedTime = 0L,
            addedTime = 0L,
            size = file.length(),
            width = 0,
            height = 0,
            mime = null,
            bucketId = -1,
            bucketName = file.parent?.fileName ?: "",
            file = file.absolutePath
        )

        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            image = image.copy(
                width = options.outWidth,
                height = options.outHeight,
                mime = options.outMimeType
            )
        } catch (e: Exception) {
            // ignore
        }
        try {
            val exif = ExifInterface(file)
            image = image.copy(
                takenTime = exif.dateTimeOriginal ?: 0L,
                modifiedTime = exif.dateTime ?: 0L,
            )
        } catch (e: Exception) {
            // ignore
        }
        image
    }

}