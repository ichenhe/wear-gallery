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

package cc.chenhe.weargallery.common.ui

import android.content.Context
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.DimenRes
import androidx.annotation.IdRes
import androidx.core.util.containsKey
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.common.ui.BaseListAdapter.SimpleItemClickListener
import timber.log.Timber
import kotlin.math.abs

/**
 * This class is a convenience wrapper for item click listening.
 *
 * Usage: set [itemClickListener] with [SimpleItemClickListener] to get item click callback.
 */
abstract class BaseListAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback) {

    var itemClickListener: ItemClickListener? = null

    @CallSuper
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.setOnClickListener { v: View ->
            val pos = holder.absoluteAdapterPosition
            if (!onItemClick(v, pos)) {
                itemClickListener?.onItemClick(v, pos)
            }
        }
        holder.itemView.setOnLongClickListener { v: View ->
            val pos = holder.absoluteAdapterPosition
            onItemLongClick(v, pos) || itemClickListener?.onItemLongClick(v, pos) ?: false
        }
    }

    /**
     * Called before [ItemClickListener.onItemClick].
     *
     * @return Whether consume this event. If true [ItemClickListener.onItemClick] will not be called.
     */
    open fun onItemClick(view: View, position: Int): Boolean {
        return false
    }

    /**
     * Called before [ItemClickListener.onItemLongClick].
     *
     * @return Whether consume this event. If true [ItemClickListener.onItemLongClick] will not be called.
     */
    open fun onItemLongClick(view: View, position: Int): Boolean {
        return false
    }

    interface ItemClickListener {

        fun onItemClick(view: View, position: Int)

        /**
         * @return true if the callback consumed the long click, false otherwise.
         */
        fun onItemLongClick(view: View, position: Int): Boolean
    }

    /**
     * An implementation of [ItemClickListener] that has view_empty method bodies and default return values.
     */
    open class SimpleItemClickListener : ItemClickListener {
        override fun onItemClick(view: View, position: Int) {
        }

        override fun onItemLongClick(view: View, position: Int): Boolean {
            return false
        }
    }

}

open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val viewCache: SparseArray<View> = SparseArray()

    @Suppress("UNCHECKED_CAST")
    fun <T : View> getView(@IdRes id: Int): T {
        return if (viewCache.containsKey(id)) {
            viewCache[id] as T
        } else {
            itemView.findViewById<T>(id).also { viewCache.put(id, it) }
        }
    }

    fun setText(@IdRes id: Int, text: CharSequence?) {
        getView<TextView>(id).text = text
    }
}

/**
 * A simple [RecyclerView.ItemDecoration] providing blank space.
 *
 * @param space Space in pixels.
 */
class SimpleItemDecoration(space: Int) : RecyclerView.ItemDecoration() {

    constructor(
        context: Context,
        @DimenRes dimenRes: Int
    ) : this(context.resources.getDimensionPixelSize(dimenRes))

    init {
        if (abs(space) > 10000) {
            // This fucking problem wasted me two hours. （╯‵□′）╯︵┴─┴
            Timber.tag("SimpleItemDecoration").e(
                "Abnormal space: $space. You may have missed a Context parameter to use dimen resource."
            )
        }
    }

    private val halfSpace = space / 2

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = halfSpace
        outRect.bottom = halfSpace
        outRect.left = halfSpace
        outRect.right = halfSpace
    }
}