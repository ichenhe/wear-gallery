package cc.chenhe.weargallery.ui.local

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder

abstract class LocalImagesBaseAdapter<T>(context: Context, diffCallback: DiffUtil.ItemCallback<T>) :
    BaseListAdapter<T, LocalImagesBaseAdapter<T>.SelectableVH>(diffCallback) {

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

    private var _checkedItem: MutableSet<T>? = null
    val checkedItem: Set<T>? get() = _checkedItem

    val inSelectionMode: Boolean get() = _checkedItem != null

    var selectionModeChangedListener: ((inSelectionMode: Boolean) -> Unit)? = null

    var checkedItemChangedListener: ((checked: Set<T>) -> Unit)? = null

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

    fun getItemData(position: Int): T? = currentList.getOrNull(position)


    /** For touch feedback without vibration permission. */
    private var recyclerView: View? = null

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

    @CallSuper
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
    }

    /**
     * Process partial refresh. Mainly changes related to selection.
     */
    @CallSuper
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
                checkbox.fadeIn()
            else
                checkbox.show()
        }

        fun hideCheckBox(animation: Boolean) {
            if (checkbox.visibility == View.GONE)
                return
            if (animation)
                checkbox.fadeOut()
            else
                checkbox.hide()
        }
    }

    private fun View.show() {
        alpha = 1f
        visibility = View.VISIBLE
    }

    private fun View.fadeIn() {
        alpha = 0f
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(shortAnimDuration.toLong()).setListener(null).start()
    }

    private fun View.hide() {
        visibility = View.GONE
    }

    private fun View.fadeOut() {
        animate().alpha(0f).setDuration(shortAnimDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    visibility = View.GONE
                }
            })
            .start()
    }
}
