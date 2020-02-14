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

import androidx.room.*
import cc.chenhe.weargallery.bean.RemoteImageFolder
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RemoteImageFolderDao {

    @Query("SELECT * FROM cache_mobile_image_folder ORDER BY latest_time DESC")
    abstract fun fetchAll(): Flow<List<RemoteImageFolder>>

    @Delete
    abstract suspend fun delete(items: Collection<RemoteImageFolder>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(items: List<RemoteImageFolder>): List<Long>

    @Update
    abstract suspend fun update(items: List<RemoteImageFolder>)

    @Transaction
    open suspend fun upsert(items: List<RemoteImageFolder>) {
        val updateList = mutableListOf<RemoteImageFolder>()
        insert(items).forEachIndexed { index, l ->
            if (l == -1L) {
                updateList += items[index]
            }
        }
        if (updateList.isNotEmpty()) {
            update(updateList)
        }
    }
}