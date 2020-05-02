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

package cc.chenhe.weargallery.ui.sendimages

import android.app.Application
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.util.ImageUtil.queryImages
import cc.chenhe.weargallery.utils.fetchImageColumnWidth
import cc.chenhe.weargallery.utils.logw

private const val TAG = "SendImagesViewModel"

class SendImagesViewModel(application: Application, intent: Intent) : AndroidViewModel(application) {

    val columnWidth = fetchImageColumnWidth(application)

    private val _targetFolder = MutableLiveData<String?>(null)
    val targetFolder: LiveData<String?> = _targetFolder

    val images: LiveData<List<Image>?> = liveData {
        val ids = extractIds(intent)
        if (ids.isNullOrEmpty()) {
            emit(null)
        } else {
            emit(queryImages(application, ids = extractIds(intent)))
        }
    }

    private fun extractIds(intent: Intent): List<Long>? {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            else -> {
                logw(TAG, "Unknown intent action = ${intent.action}.")
                null
            }
        }
        return uris?.map { ContentUris.parseId(it) }
    }

    /**
     * @return Whether the new path is valid and has been set.
     */
    fun setTargetFolder(s: String?): Boolean {
        if (!isFolderPathValid(s)) {
            return false
        }
        if (s == null || s.isEmpty() || s.isBlank()) {
            _targetFolder.value = null
        } else {
            _targetFolder.value = s
        }
        return true
    }

    private fun isFolderPathValid(s: String?): Boolean {
        return s.isNullOrEmpty() || s.matches(Regex("[^\\\\<>*?|\"]+")) && s.isNotBlank()
    }

}