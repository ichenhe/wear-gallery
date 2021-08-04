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

package cc.chenhe.weargallery.ui.mobile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.databinding.RvItemMobileFolderBinding
import cc.chenhe.weargallery.repository.RemoteImageRepository
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MobileImageAdapter(
    private val fragment: Fragment,
    private val repository: RemoteImageRepository
) : BaseListAdapter<RemoteImageFolder, MobileImageAdapter.FolderViewHolder>(MobileImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        return FolderViewHolder(
            RvItemMobileFolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.binding.itemImage.setTag(R.id.tag_image_position, position)
        holder.bind(getItem(position))
    }

    fun getItemData(position: Int) = getItem(position)!!

    inner class FolderViewHolder(val binding: RvItemMobileFolderBinding) :
        BaseViewHolder(binding.root) {
        fun bind(data: RemoteImageFolder) {
            binding.folderName.text = data.bucketName
            binding.itemImageCount.text = data.imageCount.toString()

            fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                repository.loadImagePreview(fragment.requireContext(), data.previewUri)
                    ?.let { cacheUri ->
                        if (binding.itemImage.getTag(R.id.tag_image_position) as Int == bindingAdapterPosition) {
                            binding.itemImage.load(cacheUri) {
                                crossfade(true)
                            }
                        }
                    }
            }
        }
    }
}

private class MobileImageDiffCallback : DiffUtil.ItemCallback<RemoteImageFolder>() {
    override fun areItemsTheSame(oldItem: RemoteImageFolder, newItem: RemoteImageFolder) =
        oldItem.bucketId == newItem.bucketId

    override fun areContentsTheSame(oldItem: RemoteImageFolder, newItem: RemoteImageFolder) =
        oldItem == newItem
}