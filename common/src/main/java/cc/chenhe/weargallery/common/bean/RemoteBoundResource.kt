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

package cc.chenhe.weargallery.common.bean

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.*

/**
 * A generic class that can provide a resource backed by both the cache and the remote data source.
 *
 * Based on [GoogleSimple](https://github.com/android/architecture-components-samples/blob/master/GithubBrowserSample/app/src/main/java/com/android/example/github/repository/NetworkBoundResource.kt).
 *
 * @param ResultType The type of data finally needed.
 * @param RequestType The type of data returned from remote. Subclasses may need to convert it to [ResultType] in
 * [saveRemoteResult].
 */
abstract class RemoteBoundResource<ResultType, RequestType> {

    fun asFlow() = flow<Resource<ResultType>> {
        emit(Loading())

        val cacheValue = loadFromCache().first()

        if (shouldFetch(cacheValue)) {
            emit(Loading(cacheValue))
            when (val resp = fetchFromRemote()) {
                is ApiSuccessResponse -> {
                    saveRemoteResult(cacheValue, processResponse(resp))
                    emitAll(loadFromCache().map { Success(it) })
                }
                is ApiEmptyResponse -> {
                    // reload from cache whatever we had
                    emitAll(loadFromCache().map { Success(it) })
                }
                is ApiErrorResponse -> {
                    onFetchFailed(resp)
                    emitAll(loadFromCache().map { Error(code = resp.code, message = resp.errorMessage, data = it) })
                }
            }
        } else {
            emitAll(loadFromCache().map { Success(it) })
        }
    }.catch { e ->
        e.printStackTrace()
        emit(Error(code = Error.CODE_EXCEPTION))
    }

    /**
     * A convenient method. [Flow.asLiveData] is called internally.
     */
    fun asLiveData() = asFlow().asLiveData()

    protected abstract fun loadFromCache(): Flow<ResultType?>

    /**
     * @return Whether should we fetch the latest data from remote.
     */
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    protected abstract suspend fun fetchFromRemote(): ApiResponse<RequestType>

    /**
     * Extract data from the wrapper [ApiResponse] class. This method will only be called if [resp] is
     * [ApiSuccessResponse].
     */
    protected open fun processResponse(resp: ApiSuccessResponse<RequestType>): RequestType = resp.data

    /**
     * Save the fresh response to cache.
     *
     * @param cached Cached data. This may be used for difference calculations.
     * @param data New data fetched remotely.
     */
    protected abstract suspend fun saveRemoteResult(cached: ResultType?, data: RequestType)

    /**
     * This method will be called if [resp] is [ApiErrorResponse].
     */
    protected open fun onFetchFailed(resp: ApiErrorResponse<RequestType>) {}
}