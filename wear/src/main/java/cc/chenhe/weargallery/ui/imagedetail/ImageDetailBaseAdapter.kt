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

package cc.chenhe.weargallery.ui.imagedetail

import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.databinding.PagerItemImageDetailBinding
import cc.chenhe.weargallery.uilts.IMAGE_ZOOM_CONSECUTIVE_SCALE
import cc.chenhe.weargallery.uilts.IMAGE_ZOOM_SINGLE_SCALE
import cc.chenhe.weargallery.uilts.logd
import me.panpf.sketch.SketchImageView
import java.util.*
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ImageDetailBaseAdapter"

abstract class ImageDetailBaseAdapter<T, VH : ImageDetailBaseAdapter.ImageDetailBaseViewHolder>(
        diffCallback: DiffUtil.ItemCallback<T>) : BaseListAdapter<T, VH>(diffCallback) {

    private val imageViews = WeakHashMap<Int, SketchImageView>()  // <position, view>
    private val itemImageViewOnClickListener = View.OnClickListener { itemImageViewClickListener?.invoke(it) }

    var itemImageViewClickListener: ((view: View) -> Unit)? = null

    /**
     * No actual image should be loaded if this flag is set. [notifyItemRangeChanged] is automatically called with a
     * [Boolean] payload when this flag is cleared.
     */
    var pending = false
        set(value) {
            if (field && !value) {
                field = value
                logd(TAG, "Clear pending flag, call notifyItemRangeChanged() with payload.")
                notifyItemRangeChanged(0, itemCount, true)
            } else {
                field = value
            }
        }

    @CallSuper
    override fun onBindViewHolder(holder: VH, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.binding.pagerSketchImage.onClickListener = itemImageViewOnClickListener
        imageViews[position] = holder.binding.pagerSketchImage
        holder.binding.pagerSketchImage.apply {
            setImageBitmap(null)
            setTag(R.id.tag_image_position, position)
        }
    }

    open class ImageDetailBaseViewHolder(val binding: PagerItemImageDetailBinding)
        : BaseViewHolder(binding.root) {
        init {
            binding.pagerSketchImage.apply {
                isZoomEnabled = true
                setClickPlayGifEnabled(R.drawable.ic_gif_play)
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    // API
    // ----------------------------------------------------------------------------------------

    /**
     * Try to rotate current image.
     *
     * @param degrees Can only be 90°, 180°, 270°, 360°.
     */
    fun rotateBy(position: Int, degrees: Int) {
        imageViews[position]?.zoomer?.rotateBy(degrees)
    }

    /**
     * @param position The target item's position.
     * @param zoomIn `true` means zoom in, else zoom out.
     * @param consecutive `true` means a single operation, else means a series invoked is expected.
     */
    fun zoom(position: Int, zoomIn: Boolean, consecutive: Boolean) {
        imageViews[position]?.zoomer?.let { zoomer ->
            val scale = if (consecutive) IMAGE_ZOOM_CONSECUTIVE_SCALE else IMAGE_ZOOM_SINGLE_SCALE
            var target = if (zoomIn) {
                zoomer.zoomScale * scale
            } else {
                zoomer.zoomScale / scale
            }
            target = max(zoomer.minZoomScale, min(target, zoomer.maxZoomScale))
            zoomer.zoom(target, !consecutive)
        }
    }

    fun getItemData(position: Int): T? = currentList.getOrNull(position)
}