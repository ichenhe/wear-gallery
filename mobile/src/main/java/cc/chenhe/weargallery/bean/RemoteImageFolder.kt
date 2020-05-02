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
import cc.chenhe.weargallery.common.comm.bean.IRemoteImageFolder
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteImageFolder(
        override val bucketId: Int,
        override val bucketName: String,
        override val imageCount: Int,
        override val previewUri: Uri,
        override val latestTime: Long
) : IRemoteImageFolder