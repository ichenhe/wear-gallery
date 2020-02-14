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

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import androidx.lifecycle.observe
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseAdapter
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseFr
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import cc.chenhe.weargallery.uilts.toast
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

/**
 * Code used with [IntentSender] to request user permission to delete an image with scoped storage.
 */
private const val DELETE_PERMISSION_REQUEST = 1

class MobileImageDetailFr : ImageDetailBaseFr() {

    private val model: MobileImageDetailViewModel by viewModel()

    private lateinit var adapter: MobileImageDetailAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = MobileImageDetailFrArgs.fromBundle(requireArguments())
        model.setBucketId(args.bucketId)

        binding.imageDetailPager.adapter = adapter

        model.images.observe(viewLifecycleOwner) { images ->
            adapter.submitList(images.data)
        }

        model.permissionNeededForDelete.observe(viewLifecycleOwner) { intentSender ->
            intentSender?.let {
                startIntentSenderForResult(intentSender, DELETE_PERMISSION_REQUEST, null, 0, 0, 0, null)
            }
        }

        model.currentItemData.observe(viewLifecycleOwner) { currentItemData ->
            // Display mime type
            if (currentItemData == null || currentItemData.localUri != null) {
                binding.imageMimeType.visibility = View.GONE
                return@observe
            }
            val mime = currentItemData.mime.split("/").getOrNull(1)?.toUpperCase(Locale.getDefault())
            if (mime == null || mime != "GIF") {
                // only display gif mime type
                binding.imageMimeType.visibility = View.GONE
            } else {
                binding.imageMimeType.apply {
                    text = mime
                    visibility = View.VISIBLE
                }
            }
        }
    }

    override fun createAdapter(): ImageDetailBaseAdapter<*, *> {
        return MobileImageDetailAdapter(this, get()).also {
            it.itemImageViewClickListener = { _ -> model.toggleWidgetsVisibility() }
            adapter = it
        }
    }

    override fun getViewModel(): ImageDetailBaseViewModel<*> = model

    override fun onLoadHd() {
        adapter.getItemData(binding.imageDetailPager.currentItem)?.let {
            toast(R.string.image_detail_load_hd_ing)
            model.loadHd(it)
        }
    }

    override fun onDelete() {
        deleteHdImage(adapter.getItemData(binding.imageDetailPager.currentItem)!!)
    }

    fun deleteHdImage(remoteImage: RemoteImage) {
        model.deleteHd(remoteImage)
    }

    override fun retry() {
        model.retry()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            model.deletePendingImage()
        }
    }
}