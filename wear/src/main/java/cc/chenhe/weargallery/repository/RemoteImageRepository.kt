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

package cc.chenhe.weargallery.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.bean.toMetadata
import cc.chenhe.weargallery.common.bean.ApiErrorResponse
import cc.chenhe.weargallery.common.bean.ApiResponse
import cc.chenhe.weargallery.common.bean.RemoteBoundResource
import cc.chenhe.weargallery.common.bean.Resource
import cc.chenhe.weargallery.common.comm.*
import cc.chenhe.weargallery.common.comm.bean.ImageHdReq
import cc.chenhe.weargallery.common.comm.bean.ImagePreviewReq
import cc.chenhe.weargallery.common.comm.bean.ImagesReq
import cc.chenhe.weargallery.common.comm.bean.ImagesResp
import cc.chenhe.weargallery.common.util.ControlledRunner
import cc.chenhe.weargallery.db.RemoteImageDao
import cc.chenhe.weargallery.db.RemoteImageFolderDao
import cc.chenhe.weargallery.uilts.REQUEST_IMAGE_HD_TIMEOUT
import cc.chenhe.weargallery.uilts.REQUEST_IMAGE_PREVIEW_TIMEOUT
import cc.chenhe.weargallery.uilts.diskcache.MobilePreviewCacheManager
import com.google.android.gms.wearable.DataMap
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.chenhe.lib.wearmsger.BothWayHub
import me.chenhe.lib.wearmsger.DataHub
import me.chenhe.lib.wearmsger.bean.DataCallback
import timber.log.Timber
import java.io.InputStream

/**
 * Repository that handles [RemoteImageFolder] and [RemoteImage] objects.
 */
class RemoteImageRepository(
    private val moshi: Moshi,
    private val imageFolderDao: RemoteImageFolderDao,
    imageDao: RemoteImageDao,
    private val previewCacheManager: MobilePreviewCacheManager
) : ImageRepository(imageDao) {

    companion object {
        private const val TAG = "ImageFolderRepo"
    }

    private val previewReqRunner by lazy { ControlledRunner<DataCallback>() }
    private val hdReqRunner by lazy { ControlledRunner<DataCallback>() }

    /**
     * Load the image folder list that on the remote device.
     */
    fun loadImageFolder(context: Context): LiveData<Resource<List<RemoteImageFolder>>> {
        return object : RemoteBoundResource<List<RemoteImageFolder>, String>() {
            override fun loadFromCache(): Flow<List<RemoteImageFolder>?> {
                return imageFolderDao.fetchAll()
            }

            // We always try to fetch the latest data.
            override fun shouldFetch(data: List<RemoteImageFolder>?): Boolean = true

            override suspend fun fetchFromRemote(): ApiResponse<String> {
                Timber.tag(TAG).d("Request remote image folders, path=$PATH_REQ_IMAGE_FOLDERS")
                return BothWayHub.requestForMessage(context, null, PATH_REQ_IMAGE_FOLDERS, "")
                    .toApiResp {
                        it.getStringData()!!
                    }
            }

            override fun onFetchFailed(resp: ApiErrorResponse<String>) {
                super.onFetchFailed(resp)
                Timber.tag(TAG).w("Failed to get remote folders. code=%d", resp.code)
            }

            override suspend fun saveRemoteResult(
                cached: List<RemoteImageFolder>?,
                data: String
            ) = withContext(Dispatchers.IO) {
                val type =
                    Types.newParameterizedType(List::class.java, RemoteImageFolder::class.java)
                val adapter: JsonAdapter<List<RemoteImageFolder>> = moshi.adapter(type)

                @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatcher
                val folders = adapter.fromJson(data) ?: listOf()
                cached?.subtract(folders)?.let { subtract ->
                    if (subtract.isNotEmpty()) {
                        Timber.tag(TAG).d("Subtract %d remote picture folders.", subtract.size)
                        imageFolderDao.delete(subtract)
                    }
                }
                Timber.tag(TAG).d("Upsert %d remote picture folders.", folders.size)
                imageFolderDao.upsert(folders)
            }
        }.asLiveData()
    }

    suspend fun loadImagePreview(context: Context, uri: Uri): Uri? {
        return object : RemoteAssetResource() {
            override fun loadFromCache(): Uri? =
                previewCacheManager.getCacheImage(uri)?.let { Uri.fromFile(it) }

            override suspend fun fetchFromRemote(): DataCallback {
                val req = moshi.adapter(ImagePreviewReq::class.java).toJson(ImagePreviewReq(uri))
                return previewReqRunner.joinPreviousOrRun(uri.toString()) {
                    BothWayHub.requestForData(
                        context,
                        null,
                        PATH_REQ_IMAGE_PREVIEW,
                        req,
                        REQUEST_IMAGE_PREVIEW_TIMEOUT
                    )
                }
            }

            override suspend fun extractAsset(dataMap: DataMap): InputStream? {
                return dataMap.getAsset(ITEM_IMAGE)?.let { asset ->
                    DataHub.getInputStreamForAsset(context, asset)
                }
            }

            override suspend fun saveToCache(ins: InputStream): Uri? {
                previewCacheManager.saveImage(uri, ins)
                return null
            }

            override suspend fun onFetchFailed(data: RemoteAssetError) {
                if (data.reason == RemoteAssetError.REASON_REMOTE_ERROR) {
                    // Maybe the picture has been deleted, let's delete the record and preview cache.
                    Timber.tag(TAG)
                        .d("Failed to fetch remote image preview: REMOTE_ERROR, uri=$uri, deleting record and cache.")
                    remoteImageDao.delete(uri)
                    previewCacheManager.deleteCacheImage(uri)
                }
            }
        }.obtain().asset
    }

    /**
     * Request paging picture list in the given bucket. This method do nothing about local cache.
     *
     * @throws RemoteException Failed to get response.
     */
    suspend fun requestImageList(
        context: Context,
        bucketId: Int,
        offset: Int,
        pageSize: Int
    ): ImagesResp = withContext(Dispatchers.IO) {
        val req = moshi.adapter(ImagesReq::class.java).toJson(ImagesReq(bucketId, offset, pageSize))
        val resp = BothWayHub.requestForMessage(context, null, PATH_REQ_IMAGES, req)
        val data = resp.check(true)!!
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            moshi.adapter(ImagesResp::class.java).nonNull().fromJson(data)!!
        } catch (e: JsonDataException) {
            throw RemoteException(RemoteException.Type.EMPTY, e)
        }
    }

    /**
     * Delete all cache records and insert the new data.
     *
     * This method does not delete the cache file.
     */
    suspend fun refreshImagesCache(bucketId: Int, images: List<RemoteImage>) {
        // FIXME: Will uri change if pictures are moved from one folder to another?
        //  If not, there may be something wrong with the cache.
        //  Use case:
        //      Picture (uri=a, bucket=1) was moved to bucket=2. When requesting a list of bucket2,
        //      the original cache record was not cleared and the new insert will be ignored. The
        //      picture still in bucket1 in our cache.
        remoteImageDao.clearAll(bucketId)
        remoteImageDao.insertOrIgnore(images)
        // Cached files are not deleted here because they may still be needed for the next page of data.
        // Cache files are managed by LRU itself
    }

    suspend fun appendImagesCache(images: List<RemoteImage>) {
        remoteImageDao.insertOrIgnore(images)
    }

    suspend fun loadImageHd(context: Context, remoteImage: RemoteImage): Uri? {
        return object : RemoteAssetResource() {
            override fun loadFromCache(): Uri? {
                return remoteImage.localUri
            }

            override suspend fun fetchFromRemote(): DataCallback {
                val req = moshi.adapter(ImageHdReq::class.java).toJson(ImageHdReq(remoteImage.uri))
                return hdReqRunner.joinPreviousOrRun(remoteImage.uri.toString()) {
                    Timber.tag(TAG).d("Fetch HD picture from remote. remoteUri=%s", remoteImage.uri)
                    BothWayHub.requestForData(
                        context,
                        null,
                        PATH_REQ_IMAGE_HD,
                        req,
                        REQUEST_IMAGE_HD_TIMEOUT
                    )
                }.also {
                    Timber.tag(TAG).d("Fetch HD picture from remote. result=%s", it.result)
                }
            }

            override suspend fun extractAsset(dataMap: DataMap): InputStream? {
                Timber.tag(TAG).d("Extract HD picture asset. remoteUri=%s", remoteImage.uri)
                return dataMap.getAsset(ITEM_IMAGE)?.let { asset ->
                    DataHub.getInputStreamForAsset(context, asset)
                }
            }

            override suspend fun saveToCache(ins: InputStream): Uri? {
                val metadata = remoteImage.toMetadata()
                ins.use {
                    val localUri = saveImage(context, metadata, it)
                    if (localUri != null) {
                        // update cache database
                        remoteImageDao.setLocalUri(remoteImage.uri, localUri)
                    } else {
                        Timber.tag(TAG)
                            .w("Failed to save HD picture, uri on remote=%s", remoteImage.uri)
                    }
                    return localUri
                }
            }
        }.obtain().asset
    }

    /**
     * Try to delete a cache of mobile picture.
     *
     * @see deleteLocalImage
     */
    suspend fun deleteHdImage(context: Context, remoteImage: RemoteImage): Pending? {
        return remoteImage.localUri?.let {
            // update remote image cache database
            deleteLocalImage(context, listOf(it))
        }
    }

}