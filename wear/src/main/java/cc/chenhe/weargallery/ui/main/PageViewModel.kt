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
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import cc.chenhe.weargallery.ui.explore.ExploreFr
import cc.chenhe.weargallery.ui.local.LocalImagesFr
import cc.chenhe.weargallery.ui.mobile.MobileImagesFr
import cc.chenhe.weargallery.uilts.fetchQueryShowPhoneImages
import cc.chenhe.weargallery.uilts.fetchShowPhoneImages

class PageViewModel(application: Application) : AndroidViewModel(application) {

    enum class Item(val id: Int) {
        Local(1), Phone(2), Explore(3), WhetherShowPhone(4)
    }

    private val showMobileImages = fetchShowPhoneImages(application)
    private val queryShowPhoneImages = fetchQueryShowPhoneImages(application)
    val items: MediatorLiveData<List<Item>> = MediatorLiveData()

    val size get() = items.value?.size ?: 0

    init {
        items.addSource(showMobileImages) { show ->
            queryShowPhoneImages.value?.also { query ->
                items.value = createFragmentList(show, query)
            }
        }
        items.addSource(queryShowPhoneImages) { query ->
            showMobileImages.value?.also { show ->
                items.value = createFragmentList(show, query)
            }
        }
    }

    private fun createFragmentList(
        showPhoneImages: Boolean,
        queryShowPhoneImages: Boolean
    ): List<Item> {
        if (queryShowPhoneImages)
            return listOf(Item.Local, Item.WhetherShowPhone, Item.Explore)
        return if (showPhoneImages)
            listOf(Item.Local, Item.Phone, Item.Explore)
        else
            listOf(Item.Local, Item.Explore)
    }

    fun createFragment(position: Int): Fragment? {
        return when (items.value?.getOrNull(position)) {
            Item.Local -> LocalImagesFr()
            Item.Phone -> MobileImagesFr()
            Item.Explore -> ExploreFr()
            Item.WhetherShowPhone -> WhetherShoePhoneImageFr()
            null -> null
        }
    }
}