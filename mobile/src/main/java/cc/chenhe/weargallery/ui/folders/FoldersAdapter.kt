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

package cc.chenhe.weargallery.ui.folders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import coil.load

private const val VIEW_TYPE_SMALL = 1 // linear
private const val VIEW_TYPE_BIG = 2 // grid

class FoldersAdapter :
    BaseListAdapter<ImageFolder, BaseViewHolder>(FoldersDiffCallback()) {

    private var layoutManager: GridLayoutManager? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.layoutManager?.let {
            if (it is GridLayoutManager)
                layoutManager = it
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        layoutManager = null
    }

    override fun getItemViewType(position: Int): Int {
        return if ((layoutManager?.spanCount ?: 1) > 1) VIEW_TYPE_BIG else VIEW_TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == VIEW_TYPE_SMALL)
            SmallViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rv_item_folders_small, parent, false)
            )
        else
            BigViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rv_item_folders_big, parent, false)
            )
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val data = getItem(position)
        when (holder) {
            is SmallViewHolder -> holder.bind(data)
            is BigViewHolder -> holder.bind(data)
        }
    }

    private inner class SmallViewHolder(itemView: View) : BaseViewHolder(itemView) {
        fun bind(data: ImageFolder) {
            setText(
                R.id.folderTitle, itemView.context.getString(
                    R.string.folders_item_small_title, data.name, data.imgNum
                )
            )
            setText(R.id.folderPath, data.path)
            getView<ImageView>(R.id.folderPreview).load(data.preview.uri)
        }
    }

    private inner class BigViewHolder(itemView: View) : BaseViewHolder(itemView) {
        fun bind(data: ImageFolder) {
            setText(R.id.folderTitle, data.name)
            setText(R.id.folderImagesCount, data.imgNum.toString())
            getView<ImageView>(R.id.folderPreview).load(data.preview.uri)
        }
    }
}

class FoldersDiffCallback : DiffUtil.ItemCallback<ImageFolder>() {
    override fun areItemsTheSame(oldItem: ImageFolder, newItem: ImageFolder): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ImageFolder, newItem: ImageFolder): Boolean {
        return oldItem == newItem
    }
}