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

package cc.chenhe.weargallery.ui.main

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.*
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageFolderGroup
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.common.util.ImageLiveData
import cc.chenhe.weargallery.common.util.ImageUtil
import cc.chenhe.weargallery.repository.RemoteImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SharedViewModel(
    application: Application,
    private val imageRepo: RemoteImageRepository
) : AndroidViewModel(application) {

    private val _localImages = ImageScopeLiveData().map { Success(it) }
    val localImages: LiveData<Success<List<Image>>> = _localImages

    val localFolderImages: LiveData<Success<List<ImageFolderGroup>>> = localImages.switchMap {
        liveData {
            emit(Success(ImageUtil.groupImagesByFolder(it.data!!)))
        }
    }

    /**
     * Where the user clicks to jump to the details fragment. This is used to display the correct detail data in grid
     * mode, and must be updated before jump to the details page.
     *
     * In addition, it may also be used to scroll the list to the appropriate position when returning. To achieve this,
     * the detail fragment also needs to update this value.
     *
     * PS: For folder mode, we pass `bucketId` as a parameter to indicate which bucket should be shown in details page.
     * Because only in this way can we accurately determine whether the user deletes the last picture of a specific
     * bucket.
     */
    var currentPosition = -1

    private val _fetchRemoteImageFolders = MutableLiveData(true)
    val remoteImageFolders =
        _fetchRemoteImageFolders.switchMap { imageRepo.loadImageFolder(application) }

    private var pendingDeleteImage: Uri? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    private inner class ImageScopeLiveData : ImageLiveData(getApplication()) {
        override fun getCoroutineScope(): CoroutineScope = viewModelScope
    }

    fun retryFetchRemoteImageFolders() {
        _fetchRemoteImageFolders.value = true
    }

    fun deleteLocalImage(localUri: Uri) {
        viewModelScope.launch {
            val intentSender = imageRepo.deleteLocalImage(getApplication(), localUri)
            if (intentSender != null) {
                pendingDeleteImage = localUri
            }
            _permissionNeededForDelete.postValue(intentSender)
        }
    }

    fun deletePendingImage() {
        pendingDeleteImage?.let {
            pendingDeleteImage = null
            deleteLocalImage(it)
        }
    }
}