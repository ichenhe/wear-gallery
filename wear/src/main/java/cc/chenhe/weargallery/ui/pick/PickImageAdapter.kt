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

package cc.chenhe.weargallery.ui.pick

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.databinding.RvItemPickImageBinding
import cc.chenhe.weargallery.uilts.loadWithoutHW

class PickImageAdapter : BaseListAdapter<Image, PickImageAdapter.PickViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickViewHolder {
        return PickViewHolder(
            RvItemPickImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PickViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(getItem(position))
    }

    private var gridImageSize = 0

    inner class PickViewHolder(private val binding: RvItemPickImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(image: Image) {
            if (gridImageSize == 0 && binding.itemImage.width > 0) {
                gridImageSize = binding.itemImage.width
            }
            binding.itemImage.loadWithoutHW(image.uri) {
                crossfade(true)
                if (gridImageSize > 0) {
                    size(gridImageSize, gridImageSize)
                }
            }
        }
    }

    fun getItemData(position: Int): Image = getItem(position)
}

private class DiffCallback : DiffUtil.ItemCallback<Image>() {
    override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean =
        oldItem.uri == newItem.uri

    override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean = oldItem == newItem

}