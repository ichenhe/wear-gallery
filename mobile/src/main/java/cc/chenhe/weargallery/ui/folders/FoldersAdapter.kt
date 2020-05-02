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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.GlideApp
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.ImageFolderGroup
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder

private const val VIEW_TYPE_SMALL = 1 // linear
private const val VIEW_TYPE_BIG = 2 // grid

class FoldersAdapter(private val fragment: Fragment)
    : BaseListAdapter<ImageFolderGroup, BaseViewHolder>(FoldersDiffCallback()) {

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
        return if (layoutManager?.spanCount ?: 1 > 1) VIEW_TYPE_BIG else VIEW_TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == VIEW_TYPE_SMALL)
            SmallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_folders_small, parent, false))
        else
            BigViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_folders_big, parent, false))
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
        fun bind(data: ImageFolderGroup) {
            setText(R.id.folderTitle, itemView.context
                    .getString(R.string.folders_item_small_title, data.bucketName, data.children.size))
            setText(R.id.folderPath, data.path ?: data.bucketName)
            GlideApp.with(fragment).load(data.children[0].uri).into(getView(R.id.folderPreview))
        }
    }

    private inner class BigViewHolder(itemView: View) : BaseViewHolder(itemView) {
        fun bind(data: ImageFolderGroup) {
            setText(R.id.folderTitle, data.bucketName)
            setText(R.id.folderImagesCount, data.children.size.toString())
            GlideApp.with(fragment).load(data.children[0].uri).into(getView(R.id.folderPreview))
        }
    }
}

class FoldersDiffCallback : DiffUtil.ItemCallback<ImageFolderGroup>() {
    override fun areItemsTheSame(oldItem: ImageFolderGroup, newItem: ImageFolderGroup): Boolean {
        return oldItem.bucketName == newItem.bucketName
    }

    override fun areContentsTheSame(oldItem: ImageFolderGroup, newItem: ImageFolderGroup): Boolean {
        return oldItem == newItem
    }
}