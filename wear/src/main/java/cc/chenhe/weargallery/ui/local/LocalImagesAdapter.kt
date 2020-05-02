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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageFolderGroup
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.common.util.fileName
import cc.chenhe.weargallery.databinding.RvItemLocalFolderBinding
import cc.chenhe.weargallery.databinding.RvItemLocalImageBinding
import cc.chenhe.weargallery.uilts.displayContentImage

private const val TYPE_IMAGE = 1
private const val TYPE_FOLDER = 2

/**
 * This adapter only accept a list of [Image] or [ImageFolderGroup]. [Any] is declared here to avoid extra wrapper.
 * Item view will adapt the type of given data.
 *
 * @throws IllegalArgumentException The type of given data is neither [Image] nor [ImageFolderGroup].
 */
class LocalImagesAdapter
    : BaseListAdapter<Any, BaseViewHolder>(LocalImagesDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Image -> TYPE_IMAGE
            is ImageFolderGroup -> TYPE_FOLDER
            else -> throw IllegalArgumentException("Unknown item type.")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> ImageVH(RvItemLocalImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_FOLDER -> FolderVH(RvItemLocalFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unknown item view type.")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (holder) {
            is ImageVH -> holder.bind(getItem(position) as Image)
            is FolderVH -> holder.bind(getItem(position) as ImageFolderGroup)
        }
    }

    private inner class ImageVH(private val binding: RvItemLocalImageBinding) : BaseViewHolder(binding.root) {
        fun bind(image: Image) {
            binding.itemImage.displayContentImage(image.uri)
        }
    }

    private inner class FolderVH(private val binding: RvItemLocalFolderBinding) : BaseViewHolder(binding.root) {
        fun bind(folder: ImageFolderGroup) {
            binding.itemImageCount.text = folder.children.size.toString()
            binding.folderName.text = folder.bucketName.fileName
            binding.itemImage.displayContentImage(folder.children.first().uri)
        }
    }

    fun getItemData(position: Int): Any? = currentList.getOrNull(position)
}

private class LocalImagesDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when (oldItem) {
            is Image -> {
                if (newItem is Image) oldItem.uri == newItem.uri else false
            }
            is ImageFolderGroup -> {
                if (newItem is ImageFolderGroup) oldItem.bucketName == newItem.bucketName else false
            }
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals") // misinformation
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when (oldItem) {
            is Image -> {
                if (newItem is Image) oldItem == oldItem else false
            }
            is ImageFolderGroup -> {
                if (newItem is ImageFolderGroup) oldItem == newItem else false
            }
            else -> false
        }
    }
}