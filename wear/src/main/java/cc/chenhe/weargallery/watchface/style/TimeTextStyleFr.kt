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

package cc.chenhe.weargallery.watchface.style

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.databinding.WfFrTimeTextStyleBinding
import cc.chenhe.weargallery.ui.common.PagerIndicatorCounter
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.uilts.setWatchFaceTextSize
import cc.chenhe.weargallery.uilts.setWatchFaceTimePosition
import cc.chenhe.weargallery.view.DigitalWatchFaceView
import cc.chenhe.weargallery.view.LongPressImageView
import org.koin.android.ext.android.get
import timber.log.Timber

private const val VIEW_TYPE_POSITION = 1
private const val VIEW_TYPE_SIZE = 2

private const val TAG = "TimeTextStyleFr"

class TimeTextStyleFr : SwipeDismissFr() {

    companion object {
        const val DIRECTION_UP = 1
        const val DIRECTION_DOWN = 2
        const val DIRECTION_LEFT = 3
        const val DIRECTION_RIGHT = 4
    }

    private lateinit var binding: WfFrTimeTextStyleBinding
    private lateinit var indicatorCounter: PagerIndicatorCounter


    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )
            : ViewBinding {
        return WfFrTimeTextStyleBinding.inflate(inflater, container, false).also {
            binding = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        indicatorCounter = PagerIndicatorCounter(requireContext(), lifecycleScope) { visible ->
            if (visible) indicatorCounter.fadeIn(binding.indicator)
            else indicatorCounter.fadeOut(binding.indicator)
        }
        binding.operationPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    indicatorCounter.resetVisibilityCountdown()
                } else {
                    indicatorCounter.pin()
                }
            }
        })
        binding.operationPager.adapter = PageAdapter()
        indicatorCounter.resetVisibilityCountdown(1500L)
        binding.indicator.setupWithViewPager(binding.operationPager)
    }

    fun move(direction: Int, continuous: Boolean) {
        val step = if (continuous) 3f else 1f
        when (direction) {
            DIRECTION_UP -> binding.watchFaceView.increaseTimeY(-step)
            DIRECTION_DOWN -> binding.watchFaceView.increaseTimeY(step)
            DIRECTION_LEFT -> binding.watchFaceView.increaseTimeX(-step)
            DIRECTION_RIGHT -> binding.watchFaceView.increaseTimeX(step)
        }
    }

    /**
     * @param direction `true` means zoom in, else zoom out.
     */
    fun zoom(direction: Boolean, continuous: Boolean) {
        val step = if (continuous) 1f else 1f
        if (direction) {
            binding.watchFaceView.increaseTimeTextSize(step)
        } else {
            binding.watchFaceView.increaseTimeTextSize(-step)
        }
    }

    override fun onStop() {
        super.onStop()
        val x = binding.watchFaceView.timeX
        val y = binding.watchFaceView.timeY
        if (x != DigitalWatchFaceView.NO_VALUE && y != DigitalWatchFaceView.NO_VALUE) {
            Timber.tag(TAG).d("Save watch face time position. x=$x, y=$y")
            setWatchFaceTimePosition(get(), x, y)
        }
        val textSize = binding.watchFaceView.timeTextSize
        if (textSize != DigitalWatchFaceView.NO_VALUE) {
            Timber.tag(TAG).d("Save watch face time text size=$textSize.")
            setWatchFaceTextSize(get(), textSize)
        }
    }

    private inner class PageAdapter : RecyclerView.Adapter<BaseViewHolder>() {

        private val onClickListener = View.OnClickListener { view ->
            when (view.id) {
                R.id.up -> move(DIRECTION_UP, false)
                R.id.down -> move(DIRECTION_DOWN, false)
                R.id.left -> move(DIRECTION_LEFT, false)
                R.id.right -> move(DIRECTION_RIGHT, false)
                R.id.zoomOut -> zoom(direction = false, continuous = false)
                R.id.zoomIn -> zoom(direction = true, continuous = false)
            }
        }

        private val onLongPressListener = object : LongPressImageView.OnLongPressListener {
            override fun onLongPress(view: LongPressImageView) {
                when (view.id) {
                    R.id.up -> move(DIRECTION_UP, true)
                    R.id.down -> move(DIRECTION_DOWN, true)
                    R.id.left -> move(DIRECTION_LEFT, true)
                    R.id.right -> move(DIRECTION_RIGHT, true)
                    R.id.zoomOut -> zoom(direction = false, continuous = true)
                    R.id.zoomIn -> zoom(direction = true, continuous = true)
                }
            }

            override fun onRelease(view: LongPressImageView) {
            }
        }

        override fun getItemViewType(position: Int): Int = when (position) {
            0 -> VIEW_TYPE_POSITION
            1 -> VIEW_TYPE_SIZE
            else -> throw IllegalArgumentException("Unknown position: $position")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                VIEW_TYPE_POSITION -> createPositionItem(parent)
                VIEW_TYPE_SIZE -> createSizeItem(parent)
                else -> throw IllegalArgumentException("Unknown view type: $viewType")
            }
        }

        private fun createPositionItem(parent: ViewGroup): BaseViewHolder {
            val vh = BaseViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.wf_menu_time_text_position, parent, false)
            )
            vh.getView<LongPressImageView>(R.id.up).setListener()
            vh.getView<LongPressImageView>(R.id.down).setListener()
            vh.getView<LongPressImageView>(R.id.left).setListener()
            vh.getView<LongPressImageView>(R.id.right).setListener()
            return vh
        }

        private fun createSizeItem(parent: ViewGroup): BaseViewHolder {
            val vh = BaseViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.wf_menu_time_text_size, parent, false)
            )
            vh.getView<LongPressImageView>(R.id.zoomOut).setListener()
            vh.getView<LongPressImageView>(R.id.zoomIn).setListener()
            return vh
        }

        private fun LongPressImageView.setListener() {
            this.onLongPressListener = this@PageAdapter.onLongPressListener
            this.setOnClickListener(this@PageAdapter.onClickListener)
        }

        override fun getItemCount(): Int = 2

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {

        }
    }

}