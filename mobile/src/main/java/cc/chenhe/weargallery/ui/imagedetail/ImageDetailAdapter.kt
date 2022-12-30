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

import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.StatefulAdapter
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.utils.MIME_GIF
import coil.load
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.util.*

class ImageDetailAdapter(
    private val gestureDetector: GestureDetector
) : BaseListAdapter<Image, ImageDetailAdapter.PagerViewHolder>(ImageDetailDiffCallback()), StatefulAdapter {

    private val imageViews = WeakHashMap<Int, View>()

    private val onTouchListener = View.OnTouchListener { _: View, motionEvent: MotionEvent ->
        gestureDetector.onTouchEvent(motionEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        return PagerViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.pager_item_image_detail, parent, false))
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        imageViews[position] = holder.getView(R.id.imageDetailSubImageView)
        holder.bind(getItem(position))
    }

    override fun saveState(): Parcelable {
        return Bundle()
    }

    override fun restoreState(savedState: Parcelable) {
    }

    fun getItemData(position: Int): Image = currentList[position]

    inner class PagerViewHolder(itemView: View) : BaseViewHolder(itemView) {
        private var imageView: ImageView? = null

        private fun getImageView(): ImageView {
            imageView?.let { return it }
            imageView = itemView.findViewById<ViewStub>(R.id.imageDetailIvStub).inflate() as ImageView
            return imageView!!
        }

        fun bind(data: Image) {
            if (data.mime == MIME_GIF) {
                getView<SubsamplingScaleImageView>(R.id.imageDetailSubImageView).visibility = View.GONE
                getImageView().let {
                    it.visibility = View.VISIBLE
                    it.setOnTouchListener(onTouchListener)
                    it.load(data.uri)
                }
            } else {
                imageView?.let {
                    it.visibility = View.GONE
                    it.setImageBitmap(null)
                }
                getView<SubsamplingScaleImageView>(R.id.imageDetailSubImageView).let {
                    it.visibility = View.VISIBLE
                    it.setOnTouchListener(onTouchListener)
                    it.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                    it.setImage(ImageSource.uri(data.uri))
                }
            }
        }
    }
}

private class ImageDetailDiffCallback : DiffUtil.ItemCallback<Image>() {
    override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean = oldItem.uri == newItem.uri

    override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean = oldItem == newItem

}