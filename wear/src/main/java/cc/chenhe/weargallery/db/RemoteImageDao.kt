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

package cc.chenhe.weargallery.db

import android.net.Uri
import androidx.paging.PagingSource
import androidx.room.*
import cc.chenhe.weargallery.bean.RemoteImage

@Dao
abstract class RemoteImageDao {

    // There is no additional sorting here, because it must match the paging load.
    @Query("SELECT * FROM cache_mobile_image WHERE bucket_id = :bucketId ORDER BY ROWID ASC")
    abstract fun fetchPaging(bucketId: Int): PagingSource<Int, RemoteImage>

    @Delete
    abstract suspend fun delete(items: Collection<RemoteImage>)

    /**
     * @param uri The picture's Uri on the phone.
     */
    @Query("DELETE FROM cache_mobile_image WHERE uri = :uri")
    abstract suspend fun delete(uri: Uri)

    @Query("DELETE FROM cache_mobile_image WHERE bucket_id = :bucketId")
    abstract suspend fun clearAll(bucketId: Int)

    // We assume that the picture content will not change, so ignore here.
    // REPLACE will clear local_uri field and affect the efficiency of caching.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOrIgnore(items: List<RemoteImage>)

    @Query("UPDATE cache_mobile_image SET local_uri = :localUri WHERE uri = :remoteUri")
    abstract suspend fun setLocalUri(remoteUri: Uri, localUri: Uri)

    /**
     * Clear field `local_uri` with value [localUri].
     */
    @Query("UPDATE cache_mobile_image SET local_uri = null WHERE local_uri = :localUri")
    abstract suspend fun clearLocalUri(localUri: Uri)

    /**
     * Clear field `local_uri` with value in [uris].
     */
    @Query("UPDATE cache_mobile_image SET local_uri = null WHERE local_uri in (:uris)")
    abstract suspend fun clearLocalUri(uris: Collection<Uri>): Int
}