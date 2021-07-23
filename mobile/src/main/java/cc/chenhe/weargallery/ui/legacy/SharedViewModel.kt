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

package cc.chenhe.weargallery.ui.legacy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageDateGroup
import cc.chenhe.weargallery.common.bean.ImageFolderGroup
import cc.chenhe.weargallery.common.util.ImageUtil

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    val images: LiveData<List<Image>> = liveData { emit(ImageUtil.queryImages(application)) }

    /**
     * Index of the currently displayed image for shared element animation.
     */
    var currentPosition = -1

    val groupImages: LiveData<List<ImageDateGroup>> = images.switchMap {
        liveData {
            emit(ImageUtil.groupImagesByDate(it))
        }
    }

    val folderImages: LiveData<List<ImageFolderGroup>> = images.switchMap {
        liveData {
            emit(ImageUtil.groupImagesByFolder(it))
        }
    }

    fun getGroupBasedCurrentPosition(): Int {
        if (currentPosition < 0) {
            return currentPosition
        }
        var groups = 0
        var items = 0
        groupImages.value?.forEach {
            groups++
            if (items + it.children.size > currentPosition) {
                return groups + currentPosition
            } else {
                items += it.children.size
            }
        }
        return 0
    }
}