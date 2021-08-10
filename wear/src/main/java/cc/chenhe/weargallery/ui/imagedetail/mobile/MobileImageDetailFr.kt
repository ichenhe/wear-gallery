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

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.db.RemoteImageDao
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseAdapter
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseFr
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import cc.chenhe.weargallery.uilts.toast
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.*

class MobileImageDetailFr : ImageDetailBaseFr<RemoteImage>() {
    companion object {
        private const val TAG = "MobileImageDetailFr"
    }

    private val args: MobileImageDetailFrArgs by navArgs()
    private val model: MobileImageDetailViewModel by viewModel { parametersOf(args.bucketId) }
    private val remoteImageDao: RemoteImageDao by inject()

    private var pendingUris: Collection<Uri>? = null
    private val deleteRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode != AppCompatActivity.RESULT_OK) {
                Timber.tag(TAG).d("${pendingUris?.size ?: 0} Image deletion request is rejected.")
                return@registerForActivityResult
            }
            Timber.tag(TAG)
                .d("Image deletion is approved, try to clear ${pendingUris?.size ?: 0}$ fields.")
            ProcessLifecycleOwner.get().lifecycleScope.launch {
                pendingUris?.also { uris -> remoteImageDao.clearLocalUri(uris) }
            }
        }

    private lateinit var adapter: MobileImageDetailAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.deleteRequestEvent.observe(viewLifecycleOwner) { pending ->
            pending?.let {
                pendingUris = it.uris
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build())
            }
        }

        model.currentItemData.observe(viewLifecycleOwner) { currentItemData ->
            // Display mime type
            if (currentItemData == null || currentItemData.localUri != null) {
                binding.imageMimeType.visibility = View.GONE
                return@observe
            }
            val mime =
                currentItemData.mime?.split("/")?.getOrNull(1)?.uppercase(Locale.getDefault())
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

    override fun createAdapter(): ImageDetailBaseAdapter<RemoteImage, *> {
        return MobileImageDetailAdapter(this, get()).also {
            it.itemImageViewClickListener = { _ -> model.toggleWidgetsVisibility() }
            adapter = it
        }
    }

    override fun getViewModel(): ImageDetailBaseViewModel<RemoteImage> = model

    override fun getCachedTotalCount(): Int = args.totalCount

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
        adapter.refresh()
    }
}