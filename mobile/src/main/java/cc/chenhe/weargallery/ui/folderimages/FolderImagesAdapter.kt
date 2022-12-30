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

package cc.chenhe.weargallery.ui.folderimages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import coil.load

class FolderImagesAdapter :
    BaseListAdapter<Image, FolderImagesAdapter.FolderImagesViewHolder>(FolderImagesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderImagesViewHolder {
        return FolderImagesViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.rv_item_images_img, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FolderImagesViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(getItem(position))
    }

    inner class FolderImagesViewHolder(itemView: View) : BaseViewHolder(itemView) {
        fun bind(data: Image) {
            getView<ImageView>(R.id.itemImageView).load(data.uri)
        }
    }
}

private class FolderImagesDiffCallback : DiffUtil.ItemCallback<Image>() {
    override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
        return oldItem == newItem
    }

}