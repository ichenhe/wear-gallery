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

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

private const val KB = 1024f
private const val MB = KB * 1024f

interface IImage {
    val uri: Uri
    val name: String

    /** The date & time that the image was taken in unix timestamp (ms). */
    val takenTime: Long
    val size: Long
    val width: Int
    val height: Int
    val mime: String?
    val bucketName: String

    /** Used to identify album folders since `data` is deprecated. */
    val bucketId: Int

    fun getSizeStr(): String {
        return when {
            size < KB -> "$size Bytes"
            size < MB -> String.format("%.2f KB", size / KB)
            else -> String.format("%.2f MB", size / MB)
        }
    }
}

@JsonClass(generateAdapter = true)
@Parcelize
data class Image(
        override val uri: Uri,
        override val name: String,
        override val takenTime: Long,
        override val size: Long,
        override val width: Int,
        override val height: Int,
        override val mime: String?,
        override val bucketName: String,
        override val bucketId: Int,
        /** This field can only be used for display purposes because it is deprecated. */
        @Transient val file: String? = null
) : IImage, Parcelable