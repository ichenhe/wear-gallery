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

package cc.chenhe.weargallery.common.util

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A generic class that can observe the change of ContentProvider and trans them to a [MutableLiveData]. So that we can
 * easily observe the Uri without dealing with trivial life cycles.
 */
abstract class ContentProviderLiveData<T>(
    context: Context,
    private val uri: Uri
) : MutableLiveData<T>() {
    protected val ctx = context.applicationContext!!
    private lateinit var observer: ContentObserver
    private val fetchRunner = ControlledRunner<T>()

    override fun onActive() {
        refreshData()
        if (!::observer.isInitialized) {
            observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    onChange(selfChange, null)
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    refreshData()
                }
            }
        }
        ctx.contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun onInactive() {
        ctx.contentResolver.unregisterContentObserver(observer)
    }

    private fun refreshData() {
        getCoroutineScope().launch {
            val newData = fetchRunner.cancelPreviousThenRun { getContentProviderValue() }
            postValue(newData)
        }
    }

    /**
     * Implement this method to provide a [CoroutineScope] to launch a coroutine in [ContentObserver.onChange].
     *
     * The default implement is a simple [GlobalScope] that means may leak jobs.
     */
    open fun getCoroutineScope(): CoroutineScope {
        return ProcessLifecycleOwner.get().lifecycleScope
    }

    /**
     * Implement this method to fetch data from content provider which will be post to LiveData. This method is called
     * every time the [ContentObserver.onChange] is triggered.
     */
    abstract suspend fun getContentProviderValue(): T
}