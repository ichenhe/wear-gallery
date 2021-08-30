package cc.chenhe.weargallery.repository

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.bean.toRemoteImage
import cc.chenhe.weargallery.common.comm.RemoteException
import cc.chenhe.weargallery.uilts.CacheRecordUtils
import timber.log.Timber

@ExperimentalPagingApi
class RemoteImageMediator(
    context: Context,
    private val bucketId: Int,
    private val remoteRepo: RemoteImageRepository
) : RemoteMediator<Int, RemoteImage>() {

    companion object {
        private const val TAG = "RemoteImageMediator"
    }

    private val ctx = context.applicationContext

    override suspend fun initialize(): InitializeAction {
        return if (CacheRecordUtils.shouldRefreshRemoteImageList(ctx, bucketId))
            InitializeAction.LAUNCH_INITIAL_REFRESH
        else
            InitializeAction.SKIP_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, RemoteImage>
    ): MediatorResult {
        val offset = when (loadType) {
            LoadType.REFRESH -> 0
            // we never need to prepend, since REFRESH will always load the first page in the list.
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> remoteRepo.getCachedCount(bucketId)
        }
        val pageSize = if (loadType == LoadType.REFRESH && state.pages.isEmpty()) {
            state.config.initialLoadSize
        } else {
            state.config.pageSize
        }
        Timber.tag(TAG).d(
            "Request paging remote image list. type=%s, offset=%d, size=%d",
            loadType,
            offset,
            pageSize
        )
        val resp = try {
            remoteRepo.requestImageList(ctx, bucketId, offset, pageSize)
        } catch (e: RemoteException) {
            Timber.tag(TAG).w(e, "Failed to load paging remote image list. offset=%d", offset)
            return MediatorResult.Error(e)
        }
        val end = resp.data.size < pageSize
        Timber.tag(TAG)
            .d(
                "Get remote image list. end=%s, returnSize=%d, pageSize=%d",
                end,
                resp.data.size,
                pageSize
            )
        return when (loadType) {
            LoadType.PREPEND -> throw IllegalStateException("This type should not be here.")
            LoadType.REFRESH -> {
                remoteRepo.refreshImagesCache(bucketId, resp.data.map { it.toRemoteImage() })
                CacheRecordUtils.setLastRemoteImagesUpdateTime(ctx, bucketId)
                MediatorResult.Success(endOfPaginationReached = end)
            }
            LoadType.APPEND -> {
                remoteRepo.appendImagesCache(resp.data.map { it.toRemoteImage() })
                MediatorResult.Success(endOfPaginationReached = end)
            }
        }
    }
}