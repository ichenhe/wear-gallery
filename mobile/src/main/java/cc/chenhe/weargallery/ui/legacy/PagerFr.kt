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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.databinding.FrPagerBinding
import cc.chenhe.weargallery.ui.folders.FoldersFr
import cc.chenhe.weargallery.ui.images.ImagesFr
import com.google.android.material.navigation.NavigationBarView

private const val INDEX_IMAGES = 0
private const val INDEX_FOLDERS = 1


class PagerFr : Fragment(), NavigationBarView.OnItemSelectedListener {

    private lateinit var binding: FrPagerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pager.isUserInputEnabled = false
        binding.mainBottomNav.setOnItemSelectedListener(this)

        loadFragments()
    }

    private fun loadFragments() {
        binding.pager.adapter = PagerAdapter()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_images -> binding.pager.setCurrentItem(INDEX_IMAGES, false)
            R.id.nav_folders -> binding.pager.setCurrentItem(INDEX_FOLDERS, false)
        }
        return true
    }

    private inner class PagerAdapter : FragmentStateAdapter(this) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                INDEX_IMAGES -> ImagesFr()
                INDEX_FOLDERS -> FoldersFr()
                else -> throw IllegalArgumentException("Unexpected main pager index.")
            }
        }
    }
}