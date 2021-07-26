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

package cc.chenhe.weargallery.ui.imagedetail.local

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.Loading
import cc.chenhe.weargallery.common.bean.Resource
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.common.util.ImageUtil
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import cc.chenhe.weargallery.uilts.loge
import cc.chenhe.weargallery.uilts.logw

private const val TAG = "LocalImageDetailVM"

class LocalImageDetailViewModel(application: Application) :
    ImageDetailBaseViewModel<Image>(application) {

    private val _images = MediatorLiveData<Resource<List<Image>>>().apply { value = Loading() }
    override val images: LiveData<Resource<List<Image>>> = _images

    private var dataSourceAdded = false

    init {
        init()
    }

    fun addImageDataSource(ds: LiveData<Success<List<Image>>>) {
        if (dataSourceAdded) {
            logw(TAG, "Data source has been added, drop this request.")
            return
        }
        dataSourceAdded = true
        _images.addSource(ds) { newData ->
            _images.value = newData
        }
    }

    fun addFolderDataSource(bucketId: Int) {
        if (bucketId == LocalImageDetailFr.BUCKET_ID_NA) {
            loge(TAG, "Unexpected bucket id, do you miss the parameters?")
            return
        }
        if (dataSourceAdded) {
            logw(TAG, "Data source has been added, drop this request.")
            return
        }
        dataSourceAdded = true
        _images.addSource(ImageUtil.imagesFlow(getApplication(), bucketId).asLiveData()) { imgs ->
            if (imgs == null) _images.postValue(Loading()) else _images.postValue(Success(imgs))
        }
    }

}