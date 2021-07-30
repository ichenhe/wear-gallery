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

package cc.chenhe.weargallery.ui.imagedetail.mobile

import android.app.Application
import androidx.lifecycle.*
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.common.bean.Loading
import cc.chenhe.weargallery.common.bean.Resource
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.repository.RemoteImageRepository
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import kotlinx.coroutines.launch

class MobileImageDetailViewModel(
    application: Application,
    private val repo: RemoteImageRepository
) : ImageDetailBaseViewModel<RemoteImage>(application) {


    private val _bucketId = MutableLiveData(-1)
    override val images: LiveData<Resource<List<RemoteImage>>> = _bucketId.switchMap { bucketId ->
        if (bucketId != -1) {
            repo.loadImages(getApplication(), bucketId)
        } else {
            MutableLiveData(Loading())
        }
    }

    private val _deleteRequestEvent = MutableLiveData<ImageRepository.Pending?>()
    val deleteRequestEvent: LiveData<ImageRepository.Pending?> = _deleteRequestEvent

    init {
        init()
    }

    fun setBucketId(bucketId: Int) {
        _bucketId.value = bucketId
    }

    fun retry() {
        if (_bucketId.value ?: -1 != -1) {
            _bucketId.value = _bucketId.value
        }
    }

    fun loadHd(image: RemoteImage) {
        viewModelScope.launch {
            repo.loadImageHd(getApplication(), image)
        }
    }

    fun deleteHd(image: RemoteImage) {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            // Signal to the Fragment that it needs to request permission and try the delete again if it succeeds.
            val pending = repo.deleteHdImage(getApplication(), image)
            if (pending != null) {
                _deleteRequestEvent.postValue(pending)
            }
        }
    }
}