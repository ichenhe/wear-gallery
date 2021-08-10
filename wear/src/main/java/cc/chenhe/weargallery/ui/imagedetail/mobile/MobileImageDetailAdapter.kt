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

package cc.chenhe.weargallery.ui.imagedetail.mobile

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.databinding.PagerItemImageDetailBinding
import cc.chenhe.weargallery.repository.RemoteImageRepository
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.panpf.sketch.SketchImageView
import me.panpf.sketch.decode.ImageAttrs
import me.panpf.sketch.request.CancelCause
import me.panpf.sketch.request.DisplayListener
import me.panpf.sketch.request.ErrorCause
import me.panpf.sketch.request.ImageFrom
import timber.log.Timber

private const val TAG = "ImageDetailAdapter"

class MobileImageDetailAdapter(
    private val fragment: MobileImageDetailFr,
    private val resp: RemoteImageRepository
) : ImageDetailBaseAdapter<RemoteImage, MobileImageDetailAdapter.DetailViewHolder>(DiffCallback()) {
    companion object {
        private class DiffCallback : DiffUtil.ItemCallback<RemoteImage>() {
            override fun areItemsTheSame(oldItem: RemoteImage, newItem: RemoteImage): Boolean =
                oldItem.uri == newItem.uri

            override fun areContentsTheSame(oldItem: RemoteImage, newItem: RemoteImage): Boolean {
                return oldItem == newItem && oldItem.localUri == newItem.localUri
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        return DetailViewHolder(
            PagerItemImageDetailBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(getItem(position), false)
    }

    override fun onBindViewHolder(
        holder: DetailViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            holder.bind(getItem(position), true)
        }
    }

    inner class DetailViewHolder(binding: PagerItemImageDetailBinding) :
        ImageDetailBaseViewHolder(binding) {

        init {
            binding.pagerSketchImage.isZoomEnabled = true
        }

        fun bind(data: RemoteImage?, pendingChanged: Boolean) {
            if (pending) {
                binding.pagerSketchImage.setTag(R.id.tag_image_detail_pending, true)
                binding.pagerSketchImage.setImageResource(R.drawable.bg_pic_default)
                return
            } else if (pendingChanged) {
                val tag = binding.pagerSketchImage.getTag(R.id.tag_image_detail_pending)
                if (tag == null || !(tag as Boolean)) {
                    // This item had been loaded before pending, no need to load again.
                    return
                }
            }

            binding.pagerSketchImage.setTag(R.id.tag_image_detail_pending, false)
            if (data == null) {
                binding.pagerSketchImage.setImageBitmap(null)
                return
            }
            if (data.localUri != null) {
                loadCachedHdImage(data)
            } else {
                loadImagePreview(data)
            }
        }

        private fun loadCachedHdImage(data: RemoteImage) {
            val localUri = data.localUri ?: return
            binding.pagerSketchImage.apply {
                displayListener = object : SimpleDisplayListener() {
                    override fun onError(cause: ErrorCause) {
                        // Failed to load cached HD images.
                        // The file may have been deleted so let's invalidate the cache.
                        Timber.tag(TAG)
                            .d("Failed to load cached HD picture, localUri=${data.localUri}, invalidate the cache")
                        fragment.deleteHdImage(data)
                        // No need to load preview here since RecyclerView will refresh after database is changed.
                    }
                }
                displayContentImage(localUri.toString())
                resetRotation()
            }
        }

        private fun loadImagePreview(data: RemoteImage) {
            binding.pagerSketchImage.setImageResource(R.drawable.bg_pic_default)
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                val cacheUri =
                    resp.loadImagePreview(fragment.requireContext(), data.uri) ?: return@launch
                if (binding.pagerSketchImage.getTag(R.id.tag_image_position) as Int == bindingAdapterPosition) {
                    withContext(Dispatchers.Main) {
                        binding.pagerSketchImage.displayImage(cacheUri.toString())
                        binding.pagerSketchImage.resetRotation()
                    }
                }
            }
        }

    }

    private fun SketchImageView.resetRotation() {
        if (zoomer?.rotateDegrees != 0) {
            zoomer?.rotateTo(0)
        }
    }

    private open class SimpleDisplayListener : DisplayListener {
        override fun onStarted() {}

        override fun onCanceled(cause: CancelCause) {}

        override fun onError(cause: ErrorCause) {}

        override fun onCompleted(
            drawable: Drawable,
            imageFrom: ImageFrom,
            imageAttrs: ImageAttrs
        ) {
        }
    }

}
