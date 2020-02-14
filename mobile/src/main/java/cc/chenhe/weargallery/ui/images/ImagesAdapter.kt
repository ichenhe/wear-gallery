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

package cc.chenhe.weargallery.ui.images

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import androidx.fragment.app.Fragment
import cc.chenhe.weargallery.GlideApp
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageDateGroup
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.common.util.isSameDay
import cc.chenhe.weargallery.common.view.CircleCheckBox
import cc.chenhe.weargallery.common.view.MaskImageView
import cc.chenhe.weargallery.ui.common.GroupDifferCallback
import cc.chenhe.weargallery.ui.common.GroupListAdapter
import java.text.SimpleDateFormat
import java.util.*

/**
 * @param selected Collection of selected images. Used to restore selection status.
 */
internal class ImagesAdapter(private val fragment: Fragment, private val selected: Set<Image>?, private val currentPosition: Int)
    : GroupListAdapter<ImageDateGroup, Image, ImagesAdapter.StubViewHolder>(ImagesDiffCallback()) {

    var onSelectChangedCallback: ((selectedChildCount: Int) -> Unit)? = null

    private var selectedMode = false

    private val selectedGroups = mutableSetOf<Int>()
    private val selectedImages = mutableSetOf<Image>()

    // Cache some data for formatting date
    private val currentYear by lazy { Calendar.getInstance().get(Calendar.YEAR) }
    private val nonyearDateFormat by lazy {
        SimpleDateFormat(fragment.getString(R.string.date_format_nonyear), Locale.getDefault())
    }
    private val yearDateFormat by lazy {
        SimpleDateFormat(fragment.getString(R.string.date_format_year), Locale.getDefault())
    }

    init {
        selected?.let {
            selectedImages.addAll(it)
        }
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, groupViewType: Short): StubViewHolder {
        return StubViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.rv_item_images_title, parent, false))
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, childViewType: Short): StubViewHolder {
        return StubViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.rv_item_images_img, parent, false))
    }

    override fun onBindGroupViewHolder(holder: StubViewHolder, groupIndex: Int, flattedPosition: Int) {
        holder.setText(R.id.itemImageTitleTextView, formatTime(getGroupItem(groupIndex).date))
    }

    override fun onBindChildViewHolder(holder: StubViewHolder, groupIndex: Int, childIndex: Int, flattedPosition: Int) {
        val data = getChildItem(groupIndex, childIndex)
        holder.getView<ImageView>(R.id.itemImageView).let {
            it.transitionName = data.uri.toString()

            GlideApp.with(fragment).load(data.uri).into(it)
        }
    }

    override fun onBindGroupViewHolder(holder: StubViewHolder, groupIndex: Int, flattedPosition: Int,
                                       payloads: MutableList<Any>) {
        holder.setGroupSelectionMode(isInSelectMode())
        holder.setGroupChecked(selectedGroups.contains(flattedPosition))
    }

    override fun onBindChildViewHolder(holder: StubViewHolder, groupIndex: Int, childIndex: Int, flattedPosition: Int,
                                       payloads: MutableList<Any>) {
        if (isInSelectMode()) {
            holder.setImageSelectionMode(true)
            if (selectedImages.contains(getItem(flattedPosition))) {
                holder.getView<MaskImageView>(R.id.itemImageView).isChecked = true
                holder.setImageChecked(true)
            } else {
                holder.getView<MaskImageView>(R.id.itemImageView).isChecked = false
                holder.setImageChecked(false)
            }
        } else {
            holder.getView<MaskImageView>(R.id.itemImageView).isChecked = false
            holder.setImageSelectionMode(false)
        }
    }


    override fun onItemClick(view: View, position: Int): Boolean {
        if (isInSelectMode()) {
            if (isChildItem(position)) {
                toggle(position)
            } else {
                setGroupSelected(getGroupIndex(position), !selectedGroups.contains(position))
            }
        }
        return false
    }

    private fun isWholeGroupSelected(groupIndex: Int): Boolean {
        val size = getGroupItem(groupIndex).children.size
        if (size == 0) {
            return false
        }
        getGroupItem(groupIndex).children.forEach {
            if (!selectedImages.contains(it)) {
                return false
            }
        }
        return true
    }

    private fun refreshGroupSelectState(groupIndex: Int) {
        if (isWholeGroupSelected(groupIndex)) {
            selectedGroups.add(getGroupPosition(groupIndex))
        } else {
            selectedGroups.remove(getGroupPosition(groupIndex))
        }
        notifyItemChanged(getGroupPosition(groupIndex), true)
    }

    override fun onDataCommitted() {
        super.onDataCommitted()
        if (isInSelectMode()) {
            getGroupCount().let {
                if (it > 0) {
                    for (i in 0 until it) {
                        refreshGroupSelectState(i)
                    }
                }
            }
            onSelectChangedCallback?.invoke(selectedImages.size)
        }
    }

    private fun formatTime(time: Long): String {
        if (isSameDay(System.currentTimeMillis(), time)) {
            return fragment.getString(R.string.today)
        }
        val date = Date(time)
        @Suppress("DEPRECATION")
        return if (date.year + 1900 == currentYear) {
            nonyearDateFormat.format(Date(time))
        } else {
            yearDateFormat.format(Date(time))
        }
    }

    // -------------------------------------
    // APIs
    // -------------------------------------

    fun getImageItem(position: Int) {
        getItem(position) as Image
    }

    fun getSelected(): Set<Image> {
        return selectedImages
    }

    fun enterSelectMode() {
        selectedMode = true
        notifyItemRangeChanged(0, itemCount, true)
    }

    /**
     * Exit selection mode, selection state will be cleared.
     */
    fun exitSelectMode() {
        selectedMode = false
        selectedImages.clear()
        selectedGroups.clear()
        notifyItemRangeChanged(0, itemCount, true)
    }

    fun isInSelectMode() = selectedMode

    fun setSelectAll(isSelected: Boolean) {
        if (itemCount == 0) {
            return
        }
        if (isSelected) {
            for (i in 0 until itemCount) {
                if (isGroupItem(i)) {
                    selectedGroups.add(i)
                } else {
                    selectedImages.add(getItem(i) as Image)
                }
            }
        } else {
            selectedGroups.clear()
            selectedImages.clear()
        }
        notifyItemRangeChanged(0, itemCount, true)
        onSelectChangedCallback?.invoke(selectedImages.size)
    }

    fun setGroupSelected(groupIndex: Int, isSelected: Boolean) {
        val size = getGroupItem(groupIndex).children.size
        if (size == 0) {
            return
        }
        for (i in getGroupPosition(groupIndex) + 1..getGroupPosition(groupIndex) + size) {
            setSelected(i, isSelected, refreshItem = false, refreshGroup = false)
        }
        notifyItemRangeChanged(getGroupPosition(groupIndex) + 1, getGroupPosition(groupIndex) + size, true)
        refreshGroupSelectState(groupIndex)
    }

    fun setSelected(position: Int, isSelected: Boolean, refreshItem: Boolean = true, refreshGroup: Boolean = true) {
        if (!isChildItem(position)) {
            return
        }
        if (isSelected) {
            selectedImages.add(getItem(position) as Image)
        } else {
            selectedImages.remove(getItem(position))
        }
        if (refreshItem) {
            notifyItemChanged(position, true)
        }
        if (refreshGroup) {
            refreshGroupSelectState(getGroupIndex(position))
        }
        onSelectChangedCallback?.invoke(selectedImages.size)
    }

    fun toggle(position: Int) {
        if (selectedImages.contains(getItem(position))) {
            setSelected(position, false, refreshItem = true)
        } else {
            setSelected(position, true, refreshItem = true)
        }
    }

    fun selectRange(start: Int, end: Int, isSelected: Boolean) {
        for (i in start..end) {
            setSelected(i, isSelected, refreshItem = false)
        }
        notifyItemRangeChanged(start, end - start + 1, true)
    }

    /**
     * Calculate the index in the pure image list (without group item).
     *
     * @param position Flatted position (RecyclerView's original index)
     */
    fun getImageListIndex(position: Int): Int {
        val g = getGroupIndex(position)
        val c = getChildIndex(position, g)

        var r = 0
        for (i in 0 until g) {
            r += getGroupItem(i).children.size
        }
        r += c
        return r
    }

    internal class StubViewHolder(itemView: View) : BaseViewHolder(itemView) {
        private lateinit var checkBox: CircleCheckBox

        private fun getImageCheckBox(): CircleCheckBox {
            if (::checkBox.isInitialized) {
                return checkBox
            }
            checkBox = itemView.findViewById<ViewStub>(R.id.imagesCheckBoxStub).inflate() as CircleCheckBox
            return checkBox
        }

        fun setGroupSelectionMode(inSelectMode: Boolean) {
            getView<View>(R.id.itemImageGroupCheckBox).visibility = if (inSelectMode) View.VISIBLE else View.GONE
        }

        fun setImageSelectionMode(inSelectMode: Boolean) {
            if (!inSelectMode && !::checkBox.isInitialized) {
                return
            }
            getImageCheckBox().visibility = if (inSelectMode) View.VISIBLE else View.GONE
            if (!inSelectMode) {
                setImageChecked(false)
            }
        }

        fun setGroupChecked(checked: Boolean) {
            itemView.findViewById<CircleCheckBox>(R.id.itemImageGroupCheckBox).isChecked = checked
        }

        fun setImageChecked(checked: Boolean) {
            if (!checked && !::checkBox.isInitialized) {
                return
            }
            getImageCheckBox().isChecked = checked
        }
    }

}

private class ImagesDiffCallback : GroupDifferCallback<ImageDateGroup, Image>() {
    override fun areGroupTheSame(oldGroup: ImageDateGroup, newGroup: ImageDateGroup): Boolean = oldGroup == newGroup

    override fun areChildTheSame(oldChild: Image, newChild: Image): Boolean = oldChild.uri == newChild.uri

    override fun areGroupContentsTheSame(oldGroup: ImageDateGroup, newGroup: ImageDateGroup): Boolean = oldGroup == newGroup

    override fun areChildContentsTheSame(oldChild: Image, newChild: Image): Boolean = oldChild == newChild
}
