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

package cc.chenhe.weargallery.ui.imagedetail.local

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.databinding.PagerItemImageDetailBinding
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseAdapter

class LocalImageDetailAdapter
    : ImageDetailBaseAdapter<Image, ImageDetailBaseAdapter.ImageDetailBaseViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageDetailBaseViewHolder {
        val binding = PagerItemImageDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageDetailBaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageDetailBaseViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val tag = holder.binding.pagerSketchImage.getTag(R.id.tag_image_detail_pending)
            if (tag != null && (tag as Boolean)) {
                // Need to resume loading
                onBindViewHolder(holder, position)
            }
        }
    }

    override fun onBindViewHolder(holder: ImageDetailBaseViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (pending) {
            holder.binding.pagerSketchImage.apply {
                setTag(R.id.tag_image_detail_pending, true)
                setImageResource(R.drawable.bg_pic_default)
            }
            return
        }
        val data = getItem(position)
        holder.binding.pagerSketchImage.apply {
            setTag(R.id.tag_image_detail_pending, false)
            displayContentImage(data.uri.toString())
        }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<Image>() {
    override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean = oldItem.uri == newItem.uri

    override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean = oldItem == newItem
}