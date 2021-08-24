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
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.util.fileName
import cc.chenhe.weargallery.databinding.RvItemLocalFolderBinding
import cc.chenhe.weargallery.uilts.loadWithoutHW

class LocalImagesFolderAdapter(context: Context) :
    LocalImagesBaseAdapter<ImageFolder>(context, LocalImagesFolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableVH {
        return FolderVH(
            RvItemLocalFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SelectableVH, position: Int) {
        super.onBindViewHolder(holder, position)
        (holder as FolderVH).bind(getItem(position) as ImageFolder)
    }

    private inner class FolderVH(private val binding: RvItemLocalFolderBinding) :
        SelectableVH(binding.root) {
        override val scaleRoot: View
            get() = binding.content

        override val checkbox: View
            get() = binding.checkbox

        fun bind(folder: ImageFolder) {
            binding.itemImageCount.text = folder.imgNum.toString()
            binding.folderName.text = folder.name.fileName
            binding.itemImage.loadWithoutHW(folder.preview.uri) {
                crossfade(true)
            }
        }

        override fun setChecked(checked: Boolean) {
            binding.checkbox.isChecked = checked
        }

        override fun isChecked(): Boolean = binding.checkbox.isChecked
    }

}

private class LocalImagesFolderDiffCallback : DiffUtil.ItemCallback<ImageFolder>() {
    override fun areItemsTheSame(oldItem: ImageFolder, newItem: ImageFolder): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ImageFolder, newItem: ImageFolder): Boolean {
        return oldItem == newItem
    }
}