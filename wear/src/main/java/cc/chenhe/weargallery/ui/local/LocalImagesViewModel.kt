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

package cc.chenhe.weargallery.ui.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cc.chenhe.weargallery.uilts.fetchFolderMode
import cc.chenhe.weargallery.uilts.folderMode

class LocalImagesViewModel(application: Application) : AndroidViewModel(application) {

    val folderMode: LiveData<Boolean> = fetchFolderMode(application)

    fun toggleListMode() {
        folderMode(getApplication(), !(folderMode.value ?: false))
    }
}