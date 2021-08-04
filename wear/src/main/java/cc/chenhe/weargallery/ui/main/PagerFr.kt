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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cc.chenhe.weargallery.databinding.FrPagerBinding
import cc.chenhe.weargallery.ui.common.PagerIndicatorCounter
import org.koin.androidx.viewmodel.ext.android.viewModel

class PagerFr : Fragment() {

    private lateinit var binding: FrPagerBinding
    private val model: PageViewModel by viewModel()

    private lateinit var adapter: PagerAdapter
    private lateinit var indicatorCounter: PagerIndicatorCounter


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
        indicatorCounter = PagerIndicatorCounter(requireContext(), lifecycleScope) { visible ->
            if (visible) indicatorCounter.fadeIn(binding.indicator)
            else indicatorCounter.fadeOut(binding.indicator)
        }

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    indicatorCounter.resetVisibilityCountdown()
                } else {
                    indicatorCounter.pin()
                }
            }
        })

        loadFragments()

        indicatorCounter.resetVisibilityCountdown(1500L)

    }


    private fun loadFragments() {
        model.items.observe(viewLifecycleOwner) { items ->
            if (binding.pager.adapter == null) {
                adapter = PagerAdapter().apply { setItems(items) }
                binding.pager.adapter = adapter
            } else {
                adapter.setItems(items)
            }
            binding.indicator.setupWithViewPager(binding.pager)
        }
    }

    private inner class PagerAdapter : FragmentStateAdapter(this) {
        private val items: ArrayList<PageViewModel.Item> = arrayListOf()

        override fun getItemCount(): Int = items.size

        override fun createFragment(position: Int): Fragment {
            return model.createFragment(position)
                ?: throw IllegalArgumentException("Unexpected main pager index.")
        }

        override fun getItemId(position: Int): Long {
            return items[position].id.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return items.any { it.id.toLong() == itemId }
        }

        fun setItems(newItems: List<PageViewModel.Item>) {
            val diff = DiffUtil.calculateDiff(PagerDiffUtilCallback(items, newItems))
            items.clear()
            items.addAll(newItems)
            diff.dispatchUpdatesTo(this)
        }
    }

    private class PagerDiffUtilCallback(
        private val oldList: List<PageViewModel.Item>,
        private val newList: List<PageViewModel.Item>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}