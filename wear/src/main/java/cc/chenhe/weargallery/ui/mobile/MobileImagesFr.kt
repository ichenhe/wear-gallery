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

package cc.chenhe.weargallery.ui.mobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.util.GITHUB_RELEASE
import cc.chenhe.weargallery.databinding.FrMobileImagesBinding
import cc.chenhe.weargallery.databinding.ViewRetryBinding
import cc.chenhe.weargallery.ui.common.RetryCallback
import cc.chenhe.weargallery.ui.main.PagerFrDirections
import cc.chenhe.weargallery.ui.main.SharedViewModel
import cc.chenhe.weargallery.uilts.addQrCode
import cc.chenhe.weargallery.uilts.shouldShowEmptyLayout
import cc.chenhe.weargallery.uilts.shouldShowLoadingLayout
import cc.chenhe.weargallery.uilts.shouldShowRetryLayout
import me.chenhe.wearvision.dialog.AlertDialog
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MobileImagesFr : Fragment(), RetryCallback {

    private lateinit var binding: FrMobileImagesBinding
    private val sharedViewModel: SharedViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrMobileImagesBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            sharedModel = sharedViewModel
            retryCallback = this@MobileImagesFr
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MobileImageAdapter(this, get())
        binding.imagesRecyclerView.adapter = adapter

        adapter.itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
            override fun onItemClick(view: View, position: Int) {
                val itemData = adapter.getItemData(position)
                val action = PagerFrDirections.actionPagerFrToMobileImageDetailFr(
                    itemData.bucketId,
                    itemData.imageCount,
                )
                findNavController().navigate(action)
            }
        }

        sharedViewModel.remoteFolders.observe(viewLifecycleOwner) {
            it.data?.let { data -> adapter.submitList(data) }
            when {
                shouldShowRetryLayout(it) -> {
                    binding.retryLayout.viewStub?.inflate()
                    (binding.retryLayout.binding as ViewRetryBinding).retryHelp.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            AlertDialog(requireContext()).apply {
                                setTitle(R.string.mobile_load_gallery_err_tip)
                                setMessage(R.string.mobile_load_gallery_err_content)
                                addQrCode(GITHUB_RELEASE)
                                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
                            }.show()
                        }
                    }
                }
                shouldShowEmptyLayout(it) -> {
                    binding.emptyLayout.viewStub?.inflate()
                }
                shouldShowLoadingLayout(it) -> {
                    binding.loadingLayout.viewStub?.inflate()
                }
            }
        }
    }

    override fun retry() {
        sharedViewModel.retryFetchRemoteImageFolders()
    }
}