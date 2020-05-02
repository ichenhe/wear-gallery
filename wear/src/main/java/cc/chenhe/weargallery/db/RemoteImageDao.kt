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
import androidx.room.*
import cc.chenhe.weargallery.bean.RemoteImage
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RemoteImageDao {

    @Query("SELECT * FROM cache_mobile_image WHERE bucket_id = :bucketId ORDER BY taken_time DESC, ROWID ASC")
    abstract fun fetchAll(bucketId: Int): Flow<List<RemoteImage>>

    @Delete
    abstract suspend fun delete(items: Collection<RemoteImage>)

    /**
     * @param uri The picture's Uri on the phone.
     */
    @Query("DELETE FROM cache_mobile_image WHERE uri = :uri")
    abstract suspend fun delete(uri: Uri)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(items: List<RemoteImage>): List<Long>

    @Query("UPDATE cache_mobile_image SET local_uri = :localUri WHERE uri = :remoteUri")
    abstract suspend fun setLocalUri(remoteUri: Uri, localUri: Uri)

    /**
     * Clear field `local_uri` with value [localUri].
     */
    @Query("UPDATE cache_mobile_image SET local_uri = null WHERE local_uri = :localUri")
    abstract suspend fun clearLocalUri(localUri: Uri)

}