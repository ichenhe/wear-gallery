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

package cc.chenhe.weargallery.ui.common

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.common.bean.GroupEntity
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.ui.common.GroupListAdapter.Companion.DEFAULT_VIEW_TYPE

/**
 * An extended [BaseListAdapter] that can deal with group data.
 *
 * Normally you should use functions this class provided instead of [RecyclerView.Adapter]'s original methods.
 * For example replace [submitList] with [submitData]. Do not call original functions unless you know what you are
 * doing.
 *
 * The data submitted by [submitData] will be flatted to a one-dimensional list and passed to [submitList].
 * [index] will be updated after the new data are committed.
 *
 * Multiple ViewType is supported. The type of group and the type of child are independent, so Both [getGroupViewType]
 * and [getChildViewType] should be implemented separately, they return [DEFAULT_VIEW_TYPE] by default. Finally the
 * value that subclass given will be mixed with group flag into an Int value and returned to [getItemViewType]. Please
 * use [getRealViewType] to extract your custom ViewType from original value.
 */
abstract class GroupListAdapter<G : GroupEntity<C>, C : Any, VH : RecyclerView.ViewHolder>(
        differCallback: GroupDifferCallback<G, C>
) : BaseListAdapter<Any, VH>(differCallback) {

    companion object {
        private const val TYPE_GROUP = 0
        private const val TYPE_CHILD = 1
        const val DEFAULT_VIEW_TYPE: Short = 0
    }

    /**
     * An indexed array representing the position of the group in the flatted array. (ASC order)
     *
     * For example: index[1]=3 indicates that the 2th group item's position is 3.
     * (the first group item locate at pos 0 and it has 2 children)
     *
     * It helps to calculate which group an item belongs to and switch between native position and group/child index.
     *
     * **Warning:** Do NOT edit it in subclass, read only.
     */
    private var index = intArrayOf()

    /**
     * Use higher 16 bits for group flag and lower 16 bits as true view type.
     *
     * Do NOT override this method, implement [getGroupViewType], [getChildViewType] instead.
     */
    override fun getItemViewType(position: Int): Int {
        val group = getGroupIndex(position)
        return if (index.contains(position)) {
            // It is an group item
            TYPE_GROUP.shl(16) or getGroupViewType(group).toInt()
        } else {
            TYPE_CHILD.shl(16) or getChildViewType(group, getChildIndex(position, group)).toInt()
        }
    }

    /**
     * Do NOT override this method, implement [onCreateGroupViewHolder], [onCreateChildViewHolder] instead.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return if (isGroup(viewType)) {
            onCreateGroupViewHolder(parent, getRealViewType(viewType))
        } else {
            onCreateChildViewHolder(parent, getRealViewType(viewType))
        }
    }

    /**
     * Do NOT override this method, implement [onBindGroupViewHolder], [onBindChildViewHolder] instead.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        super.onBindViewHolder(holder, position)
        if (isGroupItem(position)) {
            onBindGroupViewHolder(holder, getGroupIndex(position), position)
        } else {
            val g = getGroupIndex(position)
            onBindChildViewHolder(holder, g, getChildIndex(position, g), position)
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        }
        if (isGroupItem(position)) {
            onBindGroupViewHolder(holder, getGroupIndex(position), position, payloads)
        } else {
            val g = getGroupIndex(position)
            onBindChildViewHolder(holder, g, getChildIndex(position, g), position, payloads)
        }
    }

    private fun refreshIndex(list: List<GroupEntity<C>>) {
        index = IntArray(list.size)
        for (i in list.indices) {
            if (i == 0) {
                index[i] = 0
            } else {
                index[i] = index[i - 1] + list[i - 1].children.size + 1
            }
        }
    }

    // ---------------------------------------------
    // Abstracts
    // ---------------------------------------------

    open fun getGroupViewType(groupIndex: Int): Short {
        return DEFAULT_VIEW_TYPE
    }

    open fun getChildViewType(groupIndex: Int, childIndex: Int): Short {
        return DEFAULT_VIEW_TYPE
    }

    open fun onDataCommitted() {}

    abstract fun onCreateGroupViewHolder(parent: ViewGroup, groupViewType: Short): VH

    abstract fun onCreateChildViewHolder(parent: ViewGroup, childViewType: Short): VH

    abstract fun onBindGroupViewHolder(holder: VH, groupIndex: Int, flattedPosition: Int)

    abstract fun onBindChildViewHolder(holder: VH, groupIndex: Int, childIndex: Int, flattedPosition: Int)

    /**
     * Methods [onBindViewHolder] without [payloads] parameter will have been invoked if [payloads] is view_empty.
     * Please do not invoke it again unless you want it been called even if [payloads] is not view_empty.
     */
    open fun onBindGroupViewHolder(holder: VH, groupIndex: Int, flattedPosition: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            onBindGroupViewHolder(holder, groupIndex, flattedPosition)
        }
    }

    /**
     * Methods [onBindViewHolder] without [payloads] parameter will have been invoked if [payloads] is view_empty.
     * Please do not invoke it again unless you want it been called even if [payloads] is not view_empty.
     */
    open fun onBindChildViewHolder(holder: VH, groupIndex: Int, childIndex: Int, flattedPosition: Int,
                                   payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            onBindChildViewHolder(holder, groupIndex, childIndex, flattedPosition)
        }
    }

    // ---------------------------------------------
    // APIs
    // ---------------------------------------------

    /**
     * Calculate which group the given position belongs to.
     *
     * @param position Flatted position (RecyclerView's original index)
     * @return The group index that given item belongs to.
     */
    fun getGroupIndex(position: Int): Int {
        var p = index.size / 2
        while (true) {
            if (index[p] <= position) {
                if (p == index.size - 1 || position < index[p + 1]) {
                    return p
                } else {
                    p += 1
                }
            } else {
                p -= 1
            }
        }
    }

    /**
     * Calculate the index within its belonged group.
     *
     * @param position Flatted position (RecyclerView's original index)
     * @param groupIndex The group index which the given position belongs to. It will calculate itself if given `-1`.
     * @return The child index within its belonged group.
     */
    fun getChildIndex(position: Int, groupIndex: Int = -1): Int {
        return if (groupIndex >= 0) {
            position - index[groupIndex] - 1
        } else {
            position - index[getGroupIndex(position)] - 1
        }
    }

    /**
     * @return The flatted position in RecyclerView.
     */
    fun getGroupPosition(groupIndex: Int): Int = index[groupIndex]

    /**
     * Extract real view type and filter out group flag.
     *
     * @param viewType Mixed view type with group flag.
     */
    fun getRealViewType(viewType: Int): Short = (viewType and 0xFFFF).toShort()

    /**
     * @param position Flatted position (RecyclerView's original index)
     */
    fun isGroupItem(position: Int): Boolean = getItem(position) is GroupEntity<*>

    /**
     * @param position Flatted position (RecyclerView's original index)
     */
    fun isChildItem(position: Int): Boolean = !isGroupItem(position)

    /**
     * @param mixedViewType Mixed view type with group flag.
     */
    fun isGroup(mixedViewType: Int): Boolean = mixedViewType.shr(16) == TYPE_GROUP

    /**
     * @param mixedViewType Mixed view type with group flag.
     */
    fun isChild(mixedViewType: Int): Boolean = mixedViewType.shr(16) == TYPE_CHILD

    fun getGroupItem(groupIndex: Int): G {
        @Suppress("UNCHECKED_CAST")
        return getItem(index[groupIndex]) as G
    }

    fun getChildItem(groupIndex: Int, childIndex: Int): C {
        @Suppress("UNCHECKED_CAST")
        return getItem(index[groupIndex] + childIndex + 1) as C
    }

    fun submitData(list: List<GroupEntity<C>>) {
        val l = mutableListOf<Any>()
        list.forEach { group ->
            l.add(group)
            group.children.forEach { ch ->
                l.add(ch)
            }
        }
        submitList(l) {
            refreshIndex(list)
            onDataCommitted()
        }
    }

    fun getGroupCount(): Int = index.size

    fun getChildCount(): Int = itemCount - index.size

}

abstract class GroupDifferCallback<G : GroupEntity<C>, C> : DiffUtil.ItemCallback<Any>() {

    abstract fun areGroupTheSame(oldGroup: G, newGroup: G): Boolean
    abstract fun areChildTheSame(oldChild: C, newChild: C): Boolean

    abstract fun areGroupContentsTheSame(oldGroup: G, newGroup: G): Boolean
    abstract fun areChildContentsTheSame(oldChild: C, newChild: C): Boolean

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem is GroupEntity<*>) {
            if (newItem is GroupEntity<*>) {
                @Suppress("UNCHECKED_CAST")
                return areGroupTheSame(oldItem as G, newItem as G)
            } else {
                return false
            }
        } else {
            if (newItem is GroupEntity<*>) {
                return false
            } else {
                @Suppress("UNCHECKED_CAST")
                return areChildTheSame(oldItem as C, newItem as C)
            }
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem is GroupEntity<*>) {
            if (newItem is GroupEntity<*>) {
                @Suppress("UNCHECKED_CAST")
                return areGroupContentsTheSame(oldItem as G, newItem as G)
            } else {
                return false
            }
        } else {
            if (newItem is GroupEntity<*>) {
                return false
            } else {
                @Suppress("UNCHECKED_CAST")
                return areChildContentsTheSame(oldItem as C, newItem as C)
            }
        }
    }

}
