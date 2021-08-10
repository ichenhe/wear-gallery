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

package cc.chenhe.weargallery.bean

import android.net.Uri
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import cc.chenhe.weargallery.common.bean.IImage
import cc.chenhe.weargallery.common.bean.Image
import com.squareup.moshi.JsonClass

/**
 * An entity represent a picture on the remote device.
 *
 * Unless otherwise specified, all fields represent attributes on the remote device.
 */
@Entity(
    tableName = "cache_mobile_image",
    indices = [Index(value = ["bucket_id"])],
    foreignKeys = [ForeignKey(
        entity = RemoteImageFolder::class,
        parentColumns = arrayOf("bucket_id"),
        childColumns = arrayOf("bucket_id"),
        onDelete = CASCADE
    )]
)
@JsonClass(generateAdapter = true)
data class RemoteImage(
    @PrimaryKey override val uri: Uri,
    override val name: String,
    @ColumnInfo(name = "taken_time") override val takenTime: Long,
    @ColumnInfo(name = "modified_time") override val modifiedTime: Long,
    @ColumnInfo(name = "added_time") override val addedTime: Long,
    override val size: Long,
    override val width: Int,
    override val height: Int,
    override val mime: String?,
    @ColumnInfo(name = "bucket_name") override val bucketName: String,
    @ColumnInfo(name = "bucket_id") override val bucketId: Int
) : IImage {
    /** The uri on local device. */
    @ColumnInfo(name = "local_uri")
    var localUri: Uri? = null
}

fun Image.toRemoteImage(): RemoteImage = RemoteImage(
    uri, name, takenTime, modifiedTime, addedTime, size, width, height, mime, bucketName, bucketId,
)