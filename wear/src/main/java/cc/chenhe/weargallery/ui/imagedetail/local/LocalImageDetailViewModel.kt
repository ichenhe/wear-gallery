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
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.util.ImageUtil
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import cc.chenhe.weargallery.uilts.context
import kotlinx.coroutines.flow.Flow

class LocalImageDetailViewModel(
    application: Application,
    source: LocalImageDetailFr.Source,
    bucketId: Int,
) : ImageDetailBaseViewModel<Image>(application) {

    override val pagingImages: Flow<PagingData<Image>>

    private var invalidateCallback: InvalidateCallback? = null

    /** Refresh UI when image list changed. */
    private val observer: ContentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            invalidateCallback?.onInvalidate()
        }
    }

    init {
        pagingImages = Pager(
            config = PagingConfig(pageSize = Int.MAX_VALUE, initialLoadSize = Int.MAX_VALUE)
        ) {
            when (source) {
                LocalImageDetailFr.Source.IMAGES -> {
                    ImageSource().also { invalidateCallback = it }
                }
                LocalImageDetailFr.Source.FOLDER -> {
                    if (bucketId == LocalImageDetailFr.BUCKET_ID_NA) {
                        throw IllegalArgumentException("Unexpected bucket id, do you miss the parameters?")
                    }
                    FolderSource(bucketId).also { invalidateCallback = it }
                }
            }
        }.flow.cachedIn(viewModelScope)

        init()
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    override fun onCleared() {
        context.contentResolver.unregisterContentObserver(observer)
    }

    private inner class FolderSource(
        private val bucketId: Int
    ) : PagingSource<Int, Image>(), InvalidateCallback {
        override fun onInvalidate() {
            invalidate()
        }

        override fun getRefreshKey(state: PagingState<Int, Image>): Int? {
            return state.anchorPosition
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Image> {
            // We load all local data directly for now.
            return LoadResult.Page(
                ImageUtil.queryBucketImages(getApplication(), bucketId),
                null,
                null
            )
        }
    }

    private inner class ImageSource : PagingSource<Int, Image>(), InvalidateCallback {
        override fun onInvalidate() {
            invalidate()
        }

        override fun getRefreshKey(state: PagingState<Int, Image>): Int? {
            return state.anchorPosition
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Image> {
            // We load all local data directly for now.
            return LoadResult.Page(
                ImageUtil.queryImages(getApplication()),
                null,
                null
            )
        }
    }

    private fun interface InvalidateCallback {
        fun onInvalidate()
    }

}