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

package cc.chenhe.weargallery.ui.imagedetail

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Loading
import cc.chenhe.weargallery.common.bean.Resource
import cc.chenhe.weargallery.uilts.fetchKeepScreenOn
import cc.chenhe.weargallery.uilts.isNullOrEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HIDE_TITLE_VIEW_DELAY = 2000L

/**
 * A Base [AndroidViewModel] for image detail fragment that provides basic UI components data source. (e.g. title,
 * zoom button's visibility) You should always use this class with [ImageDetailBaseFr] which will respond to some data
 * changes and update the UI. (e.g. [title], [titleVisibility], [zoomButtonVisibility])
 *
 * The implement must call [init] in it's `init` block or constructor.
 *
 * @param T Type of picture entity.
 */
abstract class ImageDetailBaseViewModel<T>(application: Application) : AndroidViewModel(application) {

    val keepScreenOn = fetchKeepScreenOn(application)

    abstract val images: LiveData<Resource<List<T>>>

    private val _title: MediatorLiveData<String?> = MediatorLiveData()
    val title: LiveData<String?> = _title

    val currentItem = MutableLiveData(0)

    private val _currentItemData: MediatorLiveData<T?> = MediatorLiveData()
    val currentItemData: LiveData<T?> = _currentItemData

    private var hideTitleViewJob: Job? = null
    private val _titleVisibility = MutableLiveData(true)
    val titleVisibility: LiveData<Boolean> = _titleVisibility

    private val _zoomButtonVisibility = MutableLiveData(false)
    val zoomButtonVisibility: LiveData<Boolean> = _zoomButtonVisibility


    protected fun init() {
        _currentItemData.addSource(images) {
            _currentItemData.value = images.value?.data?.getOrNull(currentItem.value!!)
        }
        _currentItemData.addSource(currentItem) {
            _currentItemData.value = images.value?.data?.getOrNull(currentItem.value!!)
        }

        _title.addSource(images) {
            val data = it.data
            if (data.isNullOrEmpty()) {
                if (it is Loading) {
                    _title.value = (getApplication() as Context).getString(R.string.image_detail_load_ing)
                } else {
                    _title.value = ""
                }
            } else {
                _title.value = (getApplication() as Context).getString(R.string.image_detail_count,
                        (currentItem.value ?: 0) + 1, data.size)
            }
        }
        _title.addSource(currentItem) { current ->
            images.value?.data?.let { data ->
                _title.value = (getApplication() as Context).getString(R.string.image_detail_count, current + 1, data.size)
            }
        }
    }

    /**
     * Restart the countdown to hide widgets.
     *
     * View controller should observe [titleVisibility] and [zoomButtonVisibility] to response the change.
     *
     * @param showTitle Whether reset the title view's visibility to `true`
     */
    fun resetWidgetsVisibilityCountdown(showTitle: Boolean) {
        hideTitleViewJob?.cancel()
        if (showTitle) {
            _titleVisibility.setIfNot(true)
        }
        hideTitleViewJob = viewModelScope.launch(Dispatchers.Main) {
            delay(HIDE_TITLE_VIEW_DELAY)
            _titleVisibility.setIfNot(false)
            _zoomButtonVisibility.setIfNot(false)
        }
    }

    fun stopWidgetsVisibilityCountdown() {
        hideTitleViewJob?.cancel()
        hideTitleViewJob = null
    }

    /**
     * If any widgets is not shown, then shown them all and start the countdown via [resetWidgetsVisibilityCountdown].
     * Otherwise hide all widgets.
     */
    fun toggleWidgetsVisibility() {
        if (_titleVisibility.value == false || _zoomButtonVisibility.value == false) {
            // If any component is not shown, then shown them all.
            resetWidgetsVisibilityCountdown(true)
            _zoomButtonVisibility.setIfNot(true)
            return
        } else {
            // All components are shown, let's hide them.
            hideTitleViewJob?.cancel() // We do not need to schedule to hide them since we do it now.
            _titleVisibility.setIfNot(false)
            _zoomButtonVisibility.setIfNot(false)
        }
    }

    private fun MutableLiveData<Boolean>.setIfNot(newValue: Boolean) {
        if (value != newValue) {
            value = newValue
        }
    }
}