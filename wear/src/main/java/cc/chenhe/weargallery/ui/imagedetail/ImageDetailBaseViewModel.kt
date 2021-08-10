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
import androidx.lifecycle.*
import androidx.paging.LoadState
import androidx.paging.PagingData
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.uilts.context
import cc.chenhe.weargallery.uilts.fetchKeepScreenOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
abstract class ImageDetailBaseViewModel<T : Any>(application: Application) :
    AndroidViewModel(application) {

    val keepScreenOn = fetchKeepScreenOn(application)

    abstract val pagingImages: Flow<PagingData<T>>

    private val _title: MediatorLiveData<String?> = MediatorLiveData()
    val title: LiveData<String?> = _title

    private val _currentItem = MutableLiveData(0)
    val currentItem: LiveData<Int> = _currentItem

    private val _currentItemData: MediatorLiveData<T?> = MediatorLiveData()
    val currentItemData: LiveData<T?> = _currentItemData

    private var hideTitleViewJob: Job? = null
    private val _titleVisibility = MutableLiveData(true)
    val titleVisibility: LiveData<Boolean> = _titleVisibility

    private val _zoomButtonVisibility = MutableLiveData(false)
    val zoomButtonVisibility: LiveData<Boolean> = _zoomButtonVisibility

    /**
     * The value of this field is maintained by [ImageDetailBaseFr].
     * For other classes, it is read-only.
     */
    val initialLoadingState = MutableLiveData<LoadState>(LoadState.Loading)

    /**
     * The value of this field is maintained by [ImageDetailBaseFr].
     * For other classes, it is read-only.
     */
    private val _totalCount = MutableLiveData(0)

    /**
     * Subclass must call this method in constructor.
     */
    protected fun init() {
        _title.addSource(initialLoadingState) { loadState ->
            _title.value = if (loadState is LoadState.Loading) {
                context.getString(R.string.image_detail_load_ing)
            } else {
                context.getString(
                    R.string.image_detail_count,
                    (currentItem.value ?: 0) + 1,
                    _totalCount.value!!
                )
            }
        }
        _title.addSource(currentItem) { current ->
            if (initialLoadingState.value !is LoadState.Loading) {
                _title.value = context.getString(
                    R.string.image_detail_count,
                    current + 1,
                    _totalCount.value!!
                )
            }
        }
        _title.addSource(_totalCount) { totalCount ->
            if (initialLoadingState.value !is LoadState.Loading) {
                _title.value = context.getString(
                    R.string.image_detail_count,
                    (currentItem.value ?: 0) + 1,
                    totalCount
                )
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

    /**
     * Should only be called from [ImageDetailBaseFr].
     */
    internal fun setTotalCount(totalCount: Int) {
        if (totalCount != _totalCount.value) {
            _totalCount.value = totalCount
        }
    }

    internal fun setCurrentItem(position: Int = -1, data: T? = null) {
        if (position != -1 && position != _currentItem.value)
            _currentItem.value = position
        if (data != _currentItemData.value)
            _currentItemData.value = data
    }

    private fun MutableLiveData<Boolean>.setIfNot(newValue: Boolean) {
        if (value != newValue) {
            value = newValue
        }
    }
}