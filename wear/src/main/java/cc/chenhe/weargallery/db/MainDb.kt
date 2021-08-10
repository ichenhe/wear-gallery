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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.bean.RemoteImageFolder

@Database(
    entities = [RemoteImageFolder::class, RemoteImage::class],
    version = 3
)
@TypeConverters(TypeConverter::class)
abstract class MainDb : RoomDatabase() {

    abstract fun remoteImageFolderDao(): RemoteImageFolderDao

    abstract fun remoteImageDao(): RemoteImageDao

    companion object {
        @Volatile
        private var instance: MainDb? = null

        fun getInstance(context: Context): MainDb {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, MainDb::class.java, "main-db")
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
    }
}