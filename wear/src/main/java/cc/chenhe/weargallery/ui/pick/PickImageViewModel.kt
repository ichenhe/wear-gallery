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

package cc.chenhe.weargallery.ui.pick

import android.app.Application
import androidx.lifecycle.*
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.*
import cc.chenhe.weargallery.common.util.ImageLiveData
import cc.chenhe.weargallery.common.util.ImageUtil
import kotlinx.coroutines.CoroutineScope

class PickImageViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val BUCKET_ALL = -1
    }

    private val _currentBucketId = MutableLiveData(-1)
    val currentBucketId = _currentBucketId

    private val _localImages = ImageScopeLiveData()
    val localFolderImages: LiveData<List<ImageFolderGroup>> = _localImages.switchMap {
        liveData {
            emit(ImageUtil.groupImagesByFolder(it))
        }
    }

    private val _data = MediatorLiveData<Resource<List<Image>>>()
    val data: LiveData<Resource<List<Image>>> = _data

    val currentBucketTitle: LiveData<String?> = data.map {
        val images = it.data
        if (images == null) {
            null
        } else {
            val currentId = _currentBucketId.value
            val name = if (currentId == -1) {
                application.getString(R.string.pick_image_all)
            } else {
                localFolderImages.value?.find { it.bucketId == currentId }?.bucketName
            }
            application.getString(R.string.pick_image_folder_name, name, images.size)
        }
    }

    init {
        _data.value = Loading()

        _data.addSource(localFolderImages) { folders ->
            if (folders == null) {
                // Still loading, do nothing.
                return@addSource
            }
            val bucketId = _currentBucketId.value
            if (bucketId == BUCKET_ALL) {
                // All
                _data.value = Success(_localImages.value)
            } else {
                val folder = folders.find { it.bucketId == bucketId }
                if (folder == null) {
                    // The specified folder does not exist, reset.
                    _currentBucketId.postValue(BUCKET_ALL)
                } else {
                    _data.value = Success(folder.children)
                }
            }
        }

        _data.addSource(_currentBucketId) { bucketId ->
            val folders = localFolderImages.value
            if (_localImages.value == null || folders == null) {
                // Still loading, do nothing.
                return@addSource
            }
            if (bucketId == BUCKET_ALL) {
                // All
                _data.value = Success(_localImages.value)
            } else {
                val folder = folders.find { it.bucketId == bucketId }
                if (folder == null) {
                    // The specified folder does not exist, reset.
                    _currentBucketId.postValue(BUCKET_ALL)
                } else {
                    _data.value = Success(folder.children)
                }
            }
        }
    }

    /**
     * Set the bucket id want to display, [BUCKET_ALL] indicates all buckets.
     */
    fun setBucketId(bucketId: Int) {
        _currentBucketId.postValue(bucketId)
    }

    private inner class ImageScopeLiveData : ImageLiveData(getApplication()) {
        override fun getCoroutineScope(): CoroutineScope = viewModelScope
    }
}