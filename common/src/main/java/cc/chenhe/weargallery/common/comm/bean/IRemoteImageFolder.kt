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

package cc.chenhe.weargallery.common.comm.bean

import android.net.Uri

/**
 * An entity representing a image folder on the remote.
 * We don't use [cc.chenhe.weargallery.common.bean.ImageFolderGroup] to speed up transfers and save memory.
 *
 * Unless otherwise specified, all fields represent attributes on the remote device.
 */
interface IRemoteImageFolder {
    val bucketId: Int
    val bucketName: String
    val imageCount: Int

    /** The first image's uri on remote device. */
    val previewUri: Uri
    val latestTime: Long
}