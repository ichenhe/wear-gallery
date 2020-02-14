/*
 * Copyright (c) 2020 Chenhe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.chenhe.weargallery.wearvision.util

import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.annotation.IntDef
import androidx.collection.ArraySet
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "TrackSelectionAdapterWrapper"

/**
 * The wrapped adapter can't not set OnClickListener on item root view.
 */
class TrackSelectionAdapterWrapper<VH : RecyclerView.ViewHolder>(
        private val mAdapter: RecyclerView.Adapter<VH>
) : RecyclerView.Adapter<VH>() {

    companion object {
        const val CHOICE_MODE_NONE = 0
        const val CHOICE_MODE_SINGLE = 1
        const val CHOICE_MODE_MULTIPLE = 2

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(CHOICE_MODE_NONE, CHOICE_MODE_SINGLE, CHOICE_MODE_MULTIPLE)
        annotation class ChoiceMode
    }

    interface OnItemClickListener {
        fun onItemClick(holder: RecyclerView.ViewHolder, position: Int)
    }

    /**
     * Controls if/how the user may choose/check items in the list.
     *
     * The value should be one of [CHOICE_MODE_NONE], [CHOICE_MODE_SINGLE], or [CHOICE_MODE_MULTIPLE].
     */
    @get:ChoiceMode
    @setparam:ChoiceMode
    var choiceMode = CHOICE_MODE_NONE
        set(value) {
            field = value
            if (value != CHOICE_MODE_NONE) {
                mCheckStates.clear()
            }
            notifyDataSetChanged()
        }

    var onItemClickListener: OnItemClickListener? = null

    init {
        setHasStableIds(mAdapter.hasStableIds())
    }

    /**
     * Maintain state of which positions are currently checked.
     */
    private var mCheckStates = ArraySet<Int>()

    override fun getItemViewType(position: Int): Int = mAdapter.getItemViewType(position)

    override fun getItemId(position: Int): Long = mAdapter.getItemId(position)

    override fun getItemCount(): Int = mAdapter.itemCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vh: VH = mAdapter.onCreateViewHolder(parent, viewType)
        vh.itemView.setOnClickListener {
            performItemClick(vh, vh.layoutPosition)
        }
        return vh
    }

    private fun performItemClick(holder: VH, position: Int) {
        if (choiceMode == CHOICE_MODE_MULTIPLE) {
            setItemChecked(position, !isItemChecked(position))
            notifyItemChanged(position, true)
        } else if (choiceMode == CHOICE_MODE_SINGLE) {
            val checkedPosition: Int = getCheckedItemPosition()
            mCheckStates.clear()
            setItemChecked(position, true)
            if (checkedPosition != position) {
                if (checkedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(checkedPosition, true)
                }
                notifyItemChanged(position, true)
            }
        }
        onItemClickListener?.onItemClick(holder, position)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        mAdapter.onBindViewHolder(holder, position)
    }

    /**
     * Called only when the check state changed.
     */
    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        }
        if (choiceMode != CHOICE_MODE_NONE) {
            updateCheckState(holder.itemView, position)
        }
    }

    private fun updateCheckState(itemView: View, position: Int) {
        if (itemView is Checkable) {
            (itemView as Checkable).isChecked = isItemChecked(position)
        } else {
            itemView.isActivated = isItemChecked(position)
        }
    }

    /**
     * Sets the checked state of the specified position without any restrict or updating adapter.
     */
    private fun setItemChecked(position: Int, checked: Boolean) {
        if (checked) {
            mCheckStates.add(position)
        } else {
            mCheckStates.remove(position)
        }
    }

    // -------------------------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------------------------

    /**
     * Sets the checked state of the specified position. In multiple check mode, this method will guarantee the
     * uniqueness of the choice.
     *
     * This method is only valid if the choice mode has been set to [CHOICE_MODE_SINGLE] or [CHOICE_MODE_MULTIPLE].
     */
    fun setItemChecked(position: Int, checked: Boolean, updateView: Boolean) {
        when (choiceMode) {
            CHOICE_MODE_SINGLE -> {
                val checkedPosition = getCheckedItemPosition()
                mCheckStates.clear()
                setItemChecked(position, true)
                if (updateView) {
                    if (checkedPosition != RecyclerView.NO_POSITION && checkedPosition != position) {
                        notifyItemChanged(checkedPosition, true)
                    }
                    notifyItemChanged(position, true)
                }
            }
            CHOICE_MODE_MULTIPLE -> {
                setItemChecked(position, checked)
                if (updateView) {
                    notifyItemChanged(position, true)
                }
            }
            else -> {
                logw(TAG, "Choice mode is none or unknown, ignore this request.")
                return
            }
        }
    }

    /**
     * Clear the checked state, all items are reset to unchecked.
     *
     * This method is only valid if the choice mode has been set to [CHOICE_MODE_SINGLE] or [CHOICE_MODE_MULTIPLE].
     */
    fun clearCheck(updateView: Boolean) {
        when (choiceMode) {
            CHOICE_MODE_SINGLE -> {
                val checkedPosition = getCheckedItemPosition()
                mCheckStates.clear()
                if (updateView && checkedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(checkedPosition, true)
                }
            }
            CHOICE_MODE_MULTIPLE -> {
                mCheckStates.clear()
                if (updateView) {
                    notifyItemRangeChanged(0, itemCount, true)
                }
            }
            else -> {
                logw(TAG, "Choice mode is none or unknown, ignore this request.")
                return
            }
        }
    }

    /**
     * Returns the checked state of the specified position. The result is only valid if the choice mode has been set
     * to [CHOICE_MODE_SINGLE] or [CHOICE_MODE_MULTIPLE].
     *
     * @param position The item whose checked state to return.
     * @return The item's checked state or `false` if choice mode is invalid.
     */
    fun isItemChecked(position: Int): Boolean {
        return if (choiceMode != CHOICE_MODE_NONE) {
            mCheckStates.contains(position)
        } else false
    }

    /**
     * Returns the currently checked item. The result is only valid if the choice mode has been set to
     * [CHOICE_MODE_SINGLE].
     *
     * @return The position of the currently checked item or [RecyclerView.NO_POSITION] if nothing is selected.
     */
    fun getCheckedItemPosition(): Int {
        return if (choiceMode == CHOICE_MODE_SINGLE && mCheckStates.size == 1) {
            mCheckStates.valueAt(0)!!
        } else RecyclerView.NO_POSITION
    }

    /**
     * Returns the set of checked items in the list.
     *
     * @return A new array which contains the position of each checked item in the list.
     */
    fun getCheckedItemPositions(): IntArray {
        return mCheckStates.toTypedArray().toIntArray()
    }
}