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
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.RemoteImageFolder
import cc.chenhe.weargallery.common.bean.Error
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.util.LIVE_PREVIEW_HELP_URL
import cc.chenhe.weargallery.common.util.getVersionName
import cc.chenhe.weargallery.databinding.FrMobileImagesBinding
import cc.chenhe.weargallery.databinding.ViewRetryBinding
import cc.chenhe.weargallery.ui.common.RetryCallback
import cc.chenhe.weargallery.ui.main.PagerFrDirections
import cc.chenhe.weargallery.ui.main.SharedViewModel
import cc.chenhe.weargallery.uilts.addQrCode
import cc.chenhe.weargallery.uilts.shouldShowEmptyLayout
import cc.chenhe.weargallery.uilts.shouldShowRetryLayout
import me.chenhe.wearvision.dialog.AlertDialog
import me.chenhe.wearvision.util.enableRsbSupport
import me.chenhe.wearvision.util.postRequestFocus
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
        binding.imagesRecyclerView.enableRsbSupport()

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

        sharedViewModel.isVersionConflict.observe(viewLifecycleOwner) { isConflict: Boolean? ->
            if (isConflict != true)
                return@observe
            if (binding.retryLayout.binding?.root?.isVisible == true) {
                (binding.retryLayout.binding as? ViewRetryBinding)?.apply {
                    retryMessage.setText(R.string.mobile_version_conflict)
                    retryHelp.setOnClickListener {
                        AlertDialog(requireContext()).apply {
                            setTitle(R.string.mobile_load_gallery_err_tip)
                            message = getString(
                                R.string.mobile_version_conflict_content,
                                getVersionName(requireContext()), sharedViewModel.mobileVersion
                            )
                            addQrCode(LIVE_PREVIEW_HELP_URL)
                            setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
                        }.show()
                    }
                }
            } else {
                context?.also {
                    Toast.makeText(it, R.string.mobile_version_conflict, Toast.LENGTH_SHORT).show()
                }
            }
        }

        sharedViewModel.remoteFolders.observe(viewLifecycleOwner) {
            it.data?.let { data -> adapter.submitList(data) }
            if (it is Error) {
                sharedViewModel.checkMobileVersion()
            }
            when {
                shouldShowRetryLayout(Error<List<RemoteImageFolder>>(0, "")) -> {
                    binding.retryLayout.viewStub?.inflate()
                    (binding.retryLayout.binding as ViewRetryBinding).retryHelp.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            AlertDialog(requireContext()).apply {
                                setTitle(R.string.mobile_load_gallery_err_tip)
                                setMessage(R.string.mobile_load_gallery_err_content)
                                addQrCode(LIVE_PREVIEW_HELP_URL)
                                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
                            }.show()
                        }
                    }
                }
                shouldShowEmptyLayout(it) -> {
                    binding.emptyLayout.viewStub?.inflate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.imagesRecyclerView.postRequestFocus()
    }

    override fun retry() {
        sharedViewModel.retryFetchRemoteImageFolders()
    }
}