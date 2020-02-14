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

package cc.chenhe.weargallery.ui.images

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.utils.addImageColumnCount
import cc.chenhe.weargallery.utils.fetchImageColumnWidth
import cc.chenhe.weargallery.utils.minusImageColumnCount

class ImagesViewModel(application: Application) : AndroidViewModel(application) {

    val columnWidth = fetchImageColumnWidth(application)

    val inSelectionMode = MutableLiveData(false)

    /**
     * Used to save and restore selection status.
     */
    private var selectedImages: Set<Image>? = null

    fun saveSelectionStatus(selected: Set<Image>) {
        selectedImages = selected
    }

    fun getSelectionStatus(): Set<Image>? = selectedImages

    fun addColumn() {
        addImageColumnCount(getApplication())
    }

    fun minusColumn() {
        minusImageColumnCount(getApplication())
    }

}