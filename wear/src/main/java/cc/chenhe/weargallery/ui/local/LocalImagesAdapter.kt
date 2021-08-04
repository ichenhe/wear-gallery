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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.common.util.fileName
import cc.chenhe.weargallery.databinding.RvItemLocalFolderBinding
import cc.chenhe.weargallery.databinding.RvItemLocalImageBinding
import coil.load

private const val TYPE_IMAGE = 1
private const val TYPE_FOLDER = 2

/**
 * This adapter only accept a list of [Image] or [ImageFolder]. [Any] is declared here to avoid extra wrapper.
 * Item view will adapt the type of given data.
 *
 * @throws IllegalArgumentException The type of given data is neither [Image] nor [ImageFolder].
 */
class LocalImagesAdapter(context: Context) :
    BaseListAdapter<Any, LocalImagesAdapter.SelectableVH>(LocalImagesDiffCallback()) {

    companion object {
        /** Start the process of entering selection mode. */
        private const val MSG_START_ENTER_SELECTION_MODE = 1

        /** The process of entering selection mode is interrupted. */
        private const val MSG_CANCEL_ENTER_SELECTION_MODE = 2

        /**
         * Indicate that there is enough time to trigger the selection mode, but the user hasn't
         * released his finger yet.
         *
         * We should trigger a vibration to remind the user.
         */
        private const val MSG_ENTER_SELECTION_MODE = 3

        /** Selection mode is triggered and user has released his finger. */
        private const val MSG_UP_ENTER_SELECTION_MODE = 4

        private const val SCALE_RATIO = 0.9f

        private const val PAYLOAD_START_ENTER_SELECTION_MODE = 1
        private const val PAYLOAD_CHANGE_SELECTION_MODE = 2
        private const val PAYLOAD_CHANGE_CHECK_STATE = 3
        private const val PAYLOAD_SHOW_CHECK_BOX = 4
    }

    private val longAnimDuration =
        context.resources.getInteger(android.R.integer.config_longAnimTime)
    private val shortAnimDuration =
        context.resources.getInteger(android.R.integer.config_shortAnimTime)
    private val longClickTimeout = ViewConfiguration.getLongPressTimeout()

    /**
     * For touch feedback without vibration permission.
     */
    private var recyclerView: View? = null


    private var _checkedItem: MutableSet<Any>? = null

    val checkedItem: Set<Any>? get() = _checkedItem

    val inSelectionMode: Boolean get() = _checkedItem != null

    var selectionModeChangedListener: ((inSelectionMode: Boolean) -> Unit)? = null

    var checkedItemChangedListener: ((checked: Set<Any>) -> Unit)? = null

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_START_ENTER_SELECTION_MODE) {
                // There will be a long animation before enter selection mode really.
                // Once the process is interrupted, everything will rollback to normal status.
                notifyItemRangeChanged(0, itemCount, PAYLOAD_START_ENTER_SELECTION_MODE)
                sendEmptyMessageDelayed(MSG_ENTER_SELECTION_MODE, longAnimDuration.toLong())
            } else if (msg.what == MSG_CANCEL_ENTER_SELECTION_MODE) {
                removeMessages(MSG_ENTER_SELECTION_MODE)
                notifyItemRangeChanged(0, itemCount, PAYLOAD_CHANGE_SELECTION_MODE)
            } else if (msg.what == MSG_ENTER_SELECTION_MODE) {
                _checkedItem = mutableSetOf() // enter selection mode, reset checked items
                selectionModeChangedListener?.invoke(true)
                recyclerView?.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            } else if (msg.what == MSG_UP_ENTER_SELECTION_MODE) {
                val position = msg.obj as Int
                requireNotNull(_checkedItem).add(getItem(position))
                notifyItemRangeChanged(0, itemCount, PAYLOAD_SHOW_CHECK_BOX)
                notifyItemChanged(position, PAYLOAD_CHANGE_CHECK_STATE)
                checkedItemChangedListener?.invoke(requireNotNull(_checkedItem))
            }
        }
    }

    fun quitSelectionMode() {
        _checkedItem = null
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CHANGE_SELECTION_MODE)
        selectionModeChangedListener?.invoke(false)
    }

    fun enterSelectionMode() {
        _checkedItem = mutableSetOf()
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CHANGE_SELECTION_MODE)
        selectionModeChangedListener?.invoke(true)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Image -> TYPE_IMAGE
            is ImageFolder -> TYPE_FOLDER
            else -> throw IllegalArgumentException("Unknown item type.")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableVH {
        return when (viewType) {
            TYPE_IMAGE -> ImageVH(
                RvItemLocalImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            TYPE_FOLDER -> FolderVH(
                RvItemLocalFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("Unknown item view type.")
        }
    }

    override fun onBindViewHolder(holder: SelectableVH, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.itemView.setOnTouchListener(object : View.OnTouchListener {
            var downTime = 0L
            var trigger = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        trigger = false
                        if (inSelectionMode)
                            return false
                        handler.sendEmptyMessageDelayed(
                            MSG_START_ENTER_SELECTION_MODE,
                            longClickTimeout.toLong()
                        )
                        downTime = SystemClock.uptimeMillis()
                        trigger = true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (inSelectionMode && !trigger
                            || handler.hasMessages(MSG_START_ENTER_SELECTION_MODE)
                        ) {
                            // already in selection mode or
                            // not trigger 'long click'  ->  consider as click
                            handler.removeMessages(MSG_START_ENTER_SELECTION_MODE)
                            return false
                        }
                        val upTime = SystemClock.uptimeMillis()
                        if (upTime - downTime < longClickTimeout + longAnimDuration) {
                            // animation of enter selection mode is interrupted
                            handler.sendEmptyMessage(MSG_CANCEL_ENTER_SELECTION_MODE)
                            v.isPressed = false // fix ripple effect
                            return true
                        }

                        // activate selection mode
                        val msg = handler.obtainMessage(
                            MSG_UP_ENTER_SELECTION_MODE,
                            holder.bindingAdapterPosition
                        )
                        handler.sendMessage(msg)
                        v.isPressed = false
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        holder.itemView.cancelLongPress()
                        handler.removeMessages(MSG_START_ENTER_SELECTION_MODE)
                        if (SystemClock.uptimeMillis() - downTime < longClickTimeout + longAnimDuration) {
                            // animation of enter selection mode is interrupted
                            handler.sendEmptyMessage(MSG_CANCEL_ENTER_SELECTION_MODE)
                            return true
                        }
                    }
                }
                return false
            }
        })

        if (inSelectionMode) {
            holder.scaleRoot.apply {
                scaleX = SCALE_RATIO
                scaleY = SCALE_RATIO
            }
            holder.setChecked(_checkedItem?.contains(getItem(position)) ?: false)
            holder.showCheckBox(false)
        } else {
            holder.scaleRoot.apply {
                scaleX = 1f
                scaleY = 1f
            }
            holder.hideCheckBox(false)
        }

        when (holder) {
            is ImageVH -> holder.bind(getItem(position) as Image)
            is FolderVH -> holder.bind(getItem(position) as ImageFolder)
        }
    }

    /**
     * Process partial refresh. Mainly changes related to selection.
     */
    override fun onBindViewHolder(
        holder: SelectableVH,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        var needStartEnterSelectionMode = false
        var needChangeSelectionMode = false
        var needChangeCheckState = false
        var needShowCheckBox = false // a action, not status
        payloads.forEach {
            when (it as Int) {
                PAYLOAD_START_ENTER_SELECTION_MODE -> needStartEnterSelectionMode = true
                PAYLOAD_CHANGE_SELECTION_MODE -> needChangeSelectionMode = true
                PAYLOAD_CHANGE_CHECK_STATE -> needChangeCheckState = true
                PAYLOAD_SHOW_CHECK_BOX -> needShowCheckBox = true
            }
        }
        if (needStartEnterSelectionMode && !needChangeSelectionMode) {
            holder.setChecked(false)
            holder.scaleRoot.animate().scaleX(SCALE_RATIO).scaleY(SCALE_RATIO)
                .setDuration(longAnimDuration.toLong())
                .start()
        }
        if (needChangeCheckState) {
            holder.setChecked(_checkedItem?.contains(getItem(position)) ?: false)
        }
        if (needChangeSelectionMode) {
            if (inSelectionMode) {
                holder.setChecked(_checkedItem?.contains(getItem(position)) ?: false)
                holder.scaleRoot.animate().scaleX(SCALE_RATIO).scaleY(SCALE_RATIO)
                    .setDuration(longAnimDuration.toLong())
                    .start()
            } else {
                holder.hideCheckBox(true)
                holder.scaleRoot.animate().scaleX(1f).scaleY(1f)
                    .setDuration(shortAnimDuration.toLong())
                    .start()
            }
        }
        if (needShowCheckBox) {
            holder.showCheckBox(true)
        }
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        if (!inSelectionMode) {
            return false // pass event to outer listener
        }
        val data = getItem(position)
        if (requireNotNull(_checkedItem).contains(data)) {
            requireNotNull(_checkedItem).remove(data)
        } else {
            requireNotNull(_checkedItem).add(data)
        }
        notifyItemChanged(position, PAYLOAD_CHANGE_CHECK_STATE)
        checkedItemChangedListener?.invoke(requireNotNull(_checkedItem))
        return true
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        handler.removeCallbacksAndMessages(null) // Defensive cleanup
        super.onDetachedFromRecyclerView(recyclerView)
    }

    // -----------------------------------------------------------------------------------
    // ViewHolder
    // -----------------------------------------------------------------------------------

    abstract inner class SelectableVH(itemView: View) : BaseViewHolder(itemView) {
        /**
         * The root view which the scale animation should be applied.
         */
        abstract val scaleRoot: View

        abstract val checkbox: View

        abstract fun setChecked(checked: Boolean)

        abstract fun isChecked(): Boolean

        fun showCheckBox(animation: Boolean) {
            if (checkbox.visibility == View.VISIBLE && checkbox.alpha == 1f)
                return
            if (animation)
                checkbox.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(shortAnimDuration.toLong())
                        .setListener(null)
                        .start()
                }
            else
                checkbox.apply {
                    alpha = 1f
                    visibility = View.VISIBLE
                }
        }

        fun hideCheckBox(animation: Boolean) {
            if (checkbox.visibility == View.GONE)
                return
            if (animation)
                checkbox.animate().alpha(0f).setDuration(shortAnimDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            checkbox.visibility = View.GONE
                        }
                    })
                    .start()
            else
                checkbox.visibility = View.GONE
        }
    }

    private var gridImageSize = 0

    private inner class ImageVH(private val binding: RvItemLocalImageBinding) :
        SelectableVH(binding.root) {
        override val scaleRoot: View
            get() = binding.itemImage

        override val checkbox: View
            get() = binding.checkbox

        fun bind(image: Image) {
            if (gridImageSize == 0 && binding.itemImage.width > 0) {
                gridImageSize = binding.itemImage.width
            }
            binding.itemImage.load(image.uri) {
                crossfade(true)
                if (gridImageSize > 0) {
                    size(gridImageSize, gridImageSize)
                }
            }
        }

        override fun setChecked(checked: Boolean) {
            binding.checkbox.isChecked = checked
        }

        override fun isChecked(): Boolean = binding.checkbox.isChecked
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
            binding.itemImage.load(folder.preview.uri) {
                crossfade(true)
            }
        }

        override fun setChecked(checked: Boolean) {
            binding.checkbox.isChecked = checked
        }

        override fun isChecked(): Boolean = binding.checkbox.isChecked
    }

    fun getItemData(position: Int): Any? = currentList.getOrNull(position)
}

private class LocalImagesDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when (oldItem) {
            is Image -> {
                if (newItem is Image) oldItem.uri == newItem.uri else false
            }
            is ImageFolder -> {
                if (newItem is ImageFolder) oldItem.id == newItem.id else false
            }
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals") // misinformation
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem is Image && newItem is Image)
            return oldItem == newItem
        return if (oldItem is ImageFolder && newItem is ImageFolder)
            oldItem == newItem
        else false
    }
}