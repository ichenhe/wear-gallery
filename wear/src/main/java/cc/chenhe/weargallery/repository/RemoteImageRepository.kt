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
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.LiveData
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.common.bean.ApiResponse
import cc.chenhe.weargallery.common.bean.RemoteBoundResource
import cc.chenhe.weargallery.common.bean.Resource
import cc.chenhe.weargallery.common.comm.*
import cc.chenhe.weargallery.common.comm.bean.ImageHdReq
import cc.chenhe.weargallery.common.comm.bean.ImagePreviewReq
import cc.chenhe.weargallery.common.comm.bean.ImagesReq
import cc.chenhe.weargallery.common.util.ControlledRunner
import cc.chenhe.weargallery.common.util.fromJsonQ
import cc.chenhe.weargallery.db.RemoteImageDao
import cc.chenhe.weargallery.db.RemoteImageFolderDao
import cc.chenhe.weargallery.uilts.*
import cc.chenhe.weargallery.uilts.diskcache.MobilePreviewCacheManager
import com.google.android.gms.wearable.DataMap
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.chenhe.lib.wearmsger.BothWayHub
import me.chenhe.lib.wearmsger.DataHub
import me.chenhe.lib.wearmsger.bean.DataCallback
import java.io.InputStream

private const val TAG = "ImageFolderRepo"

/**
 * Repository that handles [RemoteImageFolder] and [RemoteImage] objects.
 */
class RemoteImageRepository(
    private val moshi: Moshi,
    private val imageFolderDao: RemoteImageFolderDao,
    private val imageDao: RemoteImageDao,
    private val previewCacheManager: MobilePreviewCacheManager
) : ImageRepository() {

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
                logd(TAG, "Request remote image folders, path=$PATH_REQ_IMAGE_FOLDERS")
                return BothWayHub.requestForMessage(context, null, PATH_REQ_IMAGE_FOLDERS, "")
                    .toApiResp {
                        it.getStringData()!!
                    }
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
                        logd(TAG, "Subtract ${subtract.size} remote picture folders.")
                        imageFolderDao.delete(subtract)
                    }
                }
                logd(TAG, "Upsert ${folders.size} remote picture folders.")
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
                    logd(
                        TAG,
                        "Failed to fetch remote image preview: REMOTE_ERROR, uri=${uri}, deleting record and cache."
                    )
                    imageDao.delete(uri)
                    previewCacheManager.deleteCacheImage(uri)
                }
            }
        }.obtain().asset
    }

    /**
     * Load the image list for a specific folder.
     */
    fun loadImages(context: Context, bucketId: Int): LiveData<Resource<List<RemoteImage>>> {
        return object : RemoteBoundResource<List<RemoteImage>, String>() {
            override fun loadFromCache(): Flow<List<RemoteImage>?> {
                return imageDao.fetchAll(bucketId)
            }

            override fun shouldFetch(data: List<RemoteImage>?): Boolean = true

            override suspend fun fetchFromRemote(): ApiResponse<String> {
                val req = moshi.adapter(ImagesReq::class.java).toJson(ImagesReq(bucketId))
                return BothWayHub.requestForMessage(context, null, PATH_REQ_IMAGES, req).toApiResp {
                    it.getStringData()!!
                }
            }

            override suspend fun saveRemoteResult(
                cached: List<RemoteImage>?,
                data: String
            ) = withContext(Dispatchers.IO) {
                val type = Types.newParameterizedType(List::class.java, RemoteImage::class.java)
                val adapter: JsonAdapter<List<RemoteImage>> = moshi.adapter(type)
                val images = adapter.fromJsonQ(data) ?: return@withContext
                cached?.subtract(images)?.let { subtract ->
                    if (subtract.isNotEmpty()) {
                        logd(
                            TAG,
                            "Subtract ${subtract.size} remote pictures in bucket <${bucketId}>."
                        )
                        loge(TAG, subtract.toString())
                        // The picture has been deleted, let's delete the record and preview cache.
                        imageDao.delete(subtract)
                        previewCacheManager.deleteCacheImage(subtract)
                    }
                }
                // We don't use `update` here because we assume that picture of the same uri should be constant.
                // Otherwise things get messy since we have to judge whether the cache is invalid which means we should
                // query the database before try to update them and will cause serious performance issues.
                logd(TAG, "Try to insert ${images.size} remote pictures in bucket <${bucketId}>.")
                imageDao.insert(images)
            }
        }.asLiveData()
    }

    suspend fun loadImageHd(context: Context, remoteImage: RemoteImage): Uri? {
        return object : RemoteAssetResource() {
            override fun loadFromCache(): Uri? {
                return remoteImage.localUri
            }

            override suspend fun fetchFromRemote(): DataCallback {
                val req = moshi.adapter(ImageHdReq::class.java).toJson(ImageHdReq(remoteImage.uri))
                return hdReqRunner.joinPreviousOrRun(remoteImage.uri.toString()) {
                    logd(TAG, "Fetch HD picture from remote. remoteUri=${remoteImage.uri}")
                    BothWayHub.requestForData(
                        context,
                        null,
                        PATH_REQ_IMAGE_HD,
                        req,
                        REQUEST_IMAGE_HD_TIMEOUT
                    )
                }.also {
                    logd(TAG, "Fetch HD picture from remote. result=${it.result}")
                }
            }

            override suspend fun extractAsset(dataMap: DataMap): InputStream? {
                logd(TAG, "Extract HD picture asset. remoteUri=${remoteImage.uri}")
                return dataMap.getAsset(ITEM_IMAGE)?.let { asset ->
                    DataHub.getInputStreamForAsset(context, asset)
                }
            }

            override suspend fun saveToCache(ins: InputStream): Uri? {
                ins.use {
                    val localUri = saveImage(context, remoteImage.name, remoteImage.takenTime, it)
                    if (localUri != null) {
                        // update cache database
                        imageDao.setLocalUri(remoteImage.uri, localUri)
                    } else {
                        logw(TAG, "Failed to save HD picture, uri on remote=${remoteImage.uri}")
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
    suspend fun deleteHdImage(context: Context, remoteImage: RemoteImage): IntentSender? {
        return remoteImage.localUri?.let {
            // update remote image cache database
            imageDao.clearLocalUri(it)
            deleteLocalImage(context, it)
        }
    }

}