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
import android.net.Uri
import androidx.lifecycle.*
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.common.util.ImageLiveData
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.repository.RemoteImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SharedViewModel(
    application: Application,
    private val imageRepo: RemoteImageRepository,
) : AndroidViewModel(application) {

    private val _localImages = ImageScopeLiveData().map { Success(it) }
    val localImages: LiveData<Success<List<Image>>> = _localImages

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

    private val _fetchRemoteFolders = MutableLiveData(true)
    val remoteFolders = _fetchRemoteFolders.switchMap { imageRepo.loadImageFolder(application) }


    /** @see deleteRequestEvent */
    private val _deleteRequestEvent = MutableLiveData<ImageRepository.Pending?>(null)

    /**
     * If value change to non-null, a request is needed to make.
     *
     * **Note: This represents a event, not a state, so it's value is meaningless unless it just changed.**
     */
    val deleteRequestEvent: LiveData<ImageRepository.Pending?> = _deleteRequestEvent

    private inner class ImageScopeLiveData : ImageLiveData(getApplication()) {
        override fun getCoroutineScope(): CoroutineScope = viewModelScope
    }

    fun retryFetchRemoteImageFolders() {
        _fetchRemoteFolders.value = true
    }

    /**
     * @see deleteLocalImages
     */
    fun deleteLocalImage(localUri: Uri) {
        deleteLocalImages(listOf(localUri))
    }

    /**
     * Delete given images. UI should observe [deleteRequestEvent] to get result.
     *
     * Initial value is `null`. If user's confirmation is requested, value will change to non-null.
     */
    fun deleteLocalImages(localUris: Collection<Uri>) {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val pending = imageRepo.deleteLocalImage(getApplication(), localUris)
            if (pending != null) {
                _deleteRequestEvent.postValue(pending)
            }
        }
    }

    /**
     * @see deleteLocalImages
     */
    fun deleteLocalImageFolders(folders: Collection<ImageFolder>) {
        val ids = folders.map { it.id }
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val pending = imageRepo.deleteLocalImageFolders(getApplication(), ids)
            if (pending != null) {
                _deleteRequestEvent.postValue(pending)
            }
        }
    }
}