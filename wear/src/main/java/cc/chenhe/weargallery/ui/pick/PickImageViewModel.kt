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
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.Loading
import cc.chenhe.weargallery.common.bean.Resource
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.common.util.ImageUtil

class PickImageViewModel(application: Application) : AndroidViewModel(application) {

    /** Current selected bucket id, `null` indicate all images. */
    private val _currentBucketId = MutableLiveData<Int?>(null)

    /** @see [_currentBucketId] */
    val currentBucketId: LiveData<Int?> = _currentBucketId

    /** All local image folders. `null` means loading state. */
    val folders = ImageUtil.imageFoldersFlow(application).asLiveData()

    /**
     * A collection of images that should be displayed at present. Related to the selected bucket.
     * `null` means loading state.
     */
    private val _images = currentBucketId.switchMap { bucketId ->
        ImageUtil.imagesFlow(application, bucketId).asLiveData()
    }

    /**
     * A state wrapper of collection of images that should be displayed at present.
     * Related to the selected bucket.
     */
    private val _data: LiveData<Resource<List<Image>>> = _images.map { images ->
        if (images == null) Loading() else Success(images)
    }

    /** @see _data */
    val data: LiveData<Resource<List<Image>>> = _data

    val currentBucketTitle: LiveData<String?> = currentBucketId.map { id ->
        if (id == null) application.getString(R.string.pick_image_all)
        else folders.value?.find { it.id == id }?.name
    }

    /**
     * Set the bucket id want to display, `null` indicates all buckets.
     */
    fun setBucketId(bucketId: Int?) {
        _currentBucketId.postValue(bucketId)
    }
}