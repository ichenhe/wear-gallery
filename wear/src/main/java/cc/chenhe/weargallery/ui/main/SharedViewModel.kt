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

package cc.chenhe.weargallery.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.common.comm.PATH_REQ_VERSION
import cc.chenhe.weargallery.common.comm.bean.VersionResp
import cc.chenhe.weargallery.common.util.ImageLiveData
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.repository.RemoteImageRepository
import cc.chenhe.weargallery.uilts.MIN_PAIRED_VERSION
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.chenhe.lib.wearmsger.BothWayHub
import timber.log.Timber

class SharedViewModel(
    application: Application,
    private val imageRepo: RemoteImageRepository,
    private val moshi: Moshi,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SharedViewModel"
    }

    private val _localImages = ImageScopeLiveData().map { Success(it) }
    val localImages: LiveData<Success<List<Image>>> = _localImages

    /**
     * Where the user clicks to jump to the details fragment. This is used to display the correct detail data in grid
     * mode, and must be updated before jump to the details page.
     *
     * In addition, it may also be used to scroll the list to the appropriate position when returning. To achieve this,
     * the detail fragment also needs to update this value.
     *
     * PS: For folder mode, we pass `bucketId` as a parameter to indicate which bucket should be shown in details page.
     * Because only in this way can we accurately determine whether the user deletes the last picture of a specific
     * bucket.
     */
    var currentPosition = -1

    private val _fetchRemoteFolders = MutableLiveData(true)
    val remoteFolders = _fetchRemoteFolders.switchMap {
        imageRepo.loadImageFolder(application)
    }


    /** @see deleteRequestEvent */
    private val _deleteRequestEvent = MutableLiveData<ImageRepository.Pending?>(null)

    /**
     * If value change to non-null, a request is needed to make.
     *
     * **Note: This represents a event, not a state, so it's value is meaningless unless it just changed.**
     */
    val deleteRequestEvent: LiveData<ImageRepository.Pending?> = _deleteRequestEvent

    /**
     * Whether the version of wear gallery between the mobile phone and the watch incompatible.
     *
     * `null` means unknown. If the watch does not respond to the version request, it will still
     * be null.
     */
    private val _isVersionConflict = MutableLiveData<Boolean?>(null)
    val isVersionConflict: LiveData<Boolean?> = _isVersionConflict
    var mobileVersion: String? = null

    private inner class ImageScopeLiveData : ImageLiveData(getApplication()) {
        override fun getCoroutineScope(): CoroutineScope = viewModelScope
    }

    fun retryFetchRemoteImageFolders() {
        _fetchRemoteFolders.value = true
    }

    /**
     * @see deleteLocalImages
     */
    fun deleteLocalImage(localUri: Uri) {
        deleteLocalImages(listOf(localUri))
    }

    /**
     * Delete given images. UI should observe [deleteRequestEvent] to get result.
     *
     * Initial value is `null`. If user's confirmation is requested, value will change to non-null.
     */
    fun deleteLocalImages(localUris: Collection<Uri>) {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val pending = imageRepo.deleteLocalImage(getApplication(), localUris)
            if (pending != null) {
                _deleteRequestEvent.postValue(pending)
            }
        }
    }

    /**
     * @see deleteLocalImages
     */
    fun deleteLocalImageFolders(folders: Collection<ImageFolder>) {
        val ids = folders.map { it.id }
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val pending = imageRepo.deleteLocalImageFolders(getApplication(), ids)
            if (pending != null) {
                _deleteRequestEvent.postValue(pending)
            }
        }
    }

    /**
     * Check whether the version of the paired application meets the requirements.
     *
     * Observe [isVersionConflict] for the result.
     */
    fun checkMobileVersion() {
        viewModelScope.launch(Dispatchers.Main) {
            Timber.tag(TAG).d("Send version request.")
            val resp = BothWayHub.requestForMessage(getApplication(), null, PATH_REQ_VERSION, "")
            if (!resp.isSuccess()) {
                Timber.tag(TAG).w("Failed to get version request response: %s", resp.result)
                _isVersionConflict.value = null
                mobileVersion = null
                return@launch
            }
            val ver = withContext(Dispatchers.IO) {
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    moshi.adapter(VersionResp::class.java).fromJson(String(resp.data!!))
                } catch (e: JsonDataException) {
                    Timber.tag(TAG).w(e, "Failed to parse version request response")
                    null
                } catch (e: NullPointerException) {
                    Timber.tag(TAG).w(e, "Failed to parse version request response: null")
                    null
                }
            } ?: kotlin.run {
                _isVersionConflict.value = null
                mobileVersion = null
                return@launch
            }
            mobileVersion = ver.name
            _isVersionConflict.value = getVersionCode(getApplication()) < ver.minPairedVersion
                    || ver.code < MIN_PAIRED_VERSION
        }
    }
}