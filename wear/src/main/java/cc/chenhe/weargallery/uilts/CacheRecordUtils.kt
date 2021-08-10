package cc.chenhe.weargallery.uilts

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object CacheRecordUtils {

    private const val SP_NAME = "cache_metadata"
    private const val ITEM_REMOTE_IMAGES_UPDATE_TIME = "remotePicturesUpdateTime_"

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    /**
     * @return The last update time of a given bucket picture list (second).
     */
    fun shouldRefreshRemoteImageList(context: Context, bucketId: Int): Boolean {
        return System.currentTimeMillis() / 1000 - sp(context).getLong(
            ITEM_REMOTE_IMAGES_UPDATE_TIME + bucketId,
            0L
        ) > REMOTE_IMAGE_LIST_CACHE_TIMEOUT
    }

    /**
     * @param time The last update time of a given bucket picture list (second).
     */
    fun setLastRemoteImagesUpdateTime(
        context: Context,
        bucketId: Int,
        time: Long = System.currentTimeMillis() / 1000
    ) {
        sp(context).edit {
            putLong(ITEM_REMOTE_IMAGES_UPDATE_TIME + bucketId, time)
        }
    }
}