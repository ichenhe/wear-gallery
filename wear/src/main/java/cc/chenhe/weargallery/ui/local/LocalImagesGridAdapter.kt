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

package cc.chenhe.weargallery.ui.local

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.databinding.RvItemLocalImageBinding
import cc.chenhe.weargallery.uilts.loadWithoutHW

class LocalImagesGridAdapter(context: Context) :
    LocalImagesBaseAdapter<Image>(context, LocalImagesGridDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LocalImagesBaseAdapter<Image>.SelectableVH {
        return ImageVH(
            RvItemLocalImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SelectableVH, position: Int) {
        super.onBindViewHolder(holder, position)
        (holder as ImageVH).bind(getItem(position) as Image)
    }

    private var gridImageSize = 0

    inner class ImageVH(private val binding: RvItemLocalImageBinding) :
        LocalImagesBaseAdapter<Image>.SelectableVH(binding.root) {
        override val scaleRoot: View
            get() = binding.itemImage

        override val checkbox: View
            get() = binding.checkbox

        fun bind(image: Image) {
            if (gridImageSize == 0 && binding.itemImage.width > 0) {
                gridImageSize = binding.itemImage.width
            }
            binding.itemImage.loadWithoutHW(image.uri) {
                crossfade(true)
                allowHardware(false)
                if (gridImageSize > 0) {
                    size(gridImageSize, gridImageSize)
                }
            }
        }

        override fun setChecked(checked: Boolean) {
            binding.checkbox.isChecked = checked
        }

        override fun isChecked(): Boolean = binding.checkbox.isChecked
    }

}

private class LocalImagesGridDiffCallback : DiffUtil.ItemCallback<Image>() {
    override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
        return oldItem == newItem
    }
}