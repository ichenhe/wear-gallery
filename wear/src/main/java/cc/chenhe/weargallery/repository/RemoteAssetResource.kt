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

import android.net.Uri
import androidx.annotation.IntDef
import cc.chenhe.lib.wearmsger.bean.DataCallback
import cc.chenhe.lib.wearmsger.compatibility.data.DataMap
import cc.chenhe.weargallery.common.comm.ITEM_RESULT
import cc.chenhe.weargallery.common.comm.RESULT_ERROR
import cc.chenhe.weargallery.uilts.loge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

private const val TAG = "RemoteAssetResource"

/**
 * A base class that can provide a Asset resource backed by both the cache and the remote data source.
 *
 * Similar to [cc.chenhe.weargallery.common.bean.RemoteBoundResource], but adapted to use case that transfer assets
 * through wear API. This class does not fully adhere to the `single source of truth` pattern for better performance.
 *
 * This class will not fetch the data from the remote by default if cache is available, we assume that the same
 * uri's image should be consistent.
 */
abstract class RemoteAssetResource {

    suspend fun obtain(): RemoteAsset = withContext(Dispatchers.IO) {
        val cache = loadFromCache()
        if (cache != null && !shouldFetch()) {
            return@withContext RemoteAssetSuccess(cache)
        }

        val remote = processRemoteResponse(fetchFromRemote())
        if (remote is RemoteAssetError) {
            onFetchFailed(remote)
        }
        remote
    }

    private suspend fun processRemoteResponse(resp: DataCallback): RemoteAsset {
        if (!resp.isSuccess()) {
            return RemoteAssetError(RemoteAssetError.REASON_COMM_ERROR)
        }
        val dataMap = resp.dataMapItem?.getDataMap() ?: return RemoteAssetError(RemoteAssetError.REASON_REMOTE_ERROR)

        if (dataMap.containsKey(ITEM_RESULT) && dataMap.getInt(ITEM_RESULT, RESULT_ERROR) == RESULT_ERROR) {
            return RemoteAssetError(RemoteAssetError.REASON_REMOTE_ERROR, dataMap)
        }

        val asset = extractAsset(dataMap) ?: return RemoteAssetError(RemoteAssetError.REASON_REMOTE_ERROR, dataMap)
        val newCache = saveToCache(asset) ?: loadFromCache()

        return if (newCache != null) {
            RemoteAssetSuccess(newCache)
        } else {
            loge(TAG, "The cache is still null after save to it, consider as communication error.")
            RemoteAssetError(RemoteAssetError.REASON_COMM_ERROR)
        }
    }

    /**
     * Try to load the data from cache.
     *
     * The return value may be a file form or other type depending on the situation. If this is a file then it should
     * be in a private directory so that we can visit it even on Android Q.
     *
     * @return Cached data, `null` if not exist.
     */
    protected abstract fun loadFromCache(): Uri?

    /**
     * @return Whether should we fetch the latest data from remote.
     */
    protected open fun shouldFetch(): Boolean = false

    protected abstract suspend fun fetchFromRemote(): DataCallback

    protected abstract suspend fun extractAsset(dataMap: DataMap): InputStream?

    /**
     * **Note** It is the implementor's responsibility to close the input stream.
     *
     * @param ins Asset data that extract from [DataMap] by [extractAsset].
     * @return The uri of the new cached file. This is a shortcut for better performance. If it is non-null, the value
     * will be returned directly without redundant function calls on [loadFromCache].
     */
    protected abstract suspend fun saveToCache(ins: InputStream): Uri?

    protected open suspend fun onFetchFailed(data: RemoteAssetError) {}
}

sealed class RemoteAsset {
    /**
     * May be a file form or other type depending on the situation. If this is a file then it should be in a private
     * directory so that we can visit it even on Android Q.
     */
    abstract val asset: Uri?
}

class RemoteAssetSuccess(
        override val asset: Uri
) : RemoteAsset()

class RemoteAssetError(
        @RemoteAssetErrorReason val reason: Int,
        val extra: DataMap? = null
) : RemoteAsset() {

    override val asset: Uri? = null

    companion object {
        /** Communication error. */
        const val REASON_COMM_ERROR = -1

        /** Remote client processing error. */
        const val REASON_REMOTE_ERROR = -2

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(REASON_COMM_ERROR, REASON_REMOTE_ERROR)
        annotation class RemoteAssetErrorReason
    }
}