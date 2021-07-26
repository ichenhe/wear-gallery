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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.*
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.databinding.FrLocalImagesBinding
import cc.chenhe.weargallery.ui.imagedetail.local.LocalImageDetailFr
import cc.chenhe.weargallery.ui.main.PagerFrDirections
import cc.chenhe.weargallery.ui.main.SharedViewModel
import cc.chenhe.weargallery.uilts.loge
import cc.chenhe.weargallery.uilts.shouldShowEmptyLayout
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocalImagesFr : Fragment() {

    companion object {
        private const val TAG = "LocalImagesFr"
    }

    private val sharedViewModel: SharedViewModel by sharedViewModel()
    private val model: LocalImagesViewModel by viewModel()

    private lateinit var binding: FrLocalImagesBinding
    private lateinit var adapter: LocalImagesAdapter

    private var imagesObserver: Observer<Success<List<Image>>>? = null
    private var foldersObserver: Observer<in Resource<out List<ImageFolder>>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrLocalImagesBinding.inflate(inflater, container, false).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.sharedModel = sharedViewModel
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadImages()

        binding.localImageHeader.setOnClickListener {
            model.toggleListMode()
        }
    }

    private fun loadImages() {
        adapter = LocalImagesAdapter()
        adapter.itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
            override fun onItemClick(view: View, position: Int) {
                sharedViewModel.currentPosition = position

                // Jump to detail fragment
                val action = if (model.folderMode.value!!) {
                    val item = adapter.getItemData(position)
                    if (item is ImageFolder) {
                        PagerFrDirections.actionPagerFrToLocalImageDetailFr(
                            LocalImageDetailFr.Source.FOLDER,
                            item.id
                        )
                    } else {
                        loge(TAG, "Now it's in folder mode but item is not a ImageFolder.")
                        return
                    }
                } else {
                    PagerFrDirections.actionPagerFrToLocalImageDetailFr(
                        LocalImageDetailFr.Source.IMAGES,
                        LocalImageDetailFr.BUCKET_ID_NA
                    )
                }
                findNavController().navigate(action)
            }
        }

        binding.imagesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@LocalImagesFr.adapter
        }

        model.folderMode.observe(viewLifecycleOwner) { folderMode ->
            binding.listGridType.setImageResource(if (folderMode) R.drawable.ic_view_list else R.drawable.ic_view_grid)
            (binding.imagesRecyclerView.layoutManager as GridLayoutManager).spanCount =
                if (folderMode) 1 else 2
            registerImagesObserver(folderMode)
        }
    }

    private fun registerImagesObserver(isFolderMode: Boolean) {
        if (isFolderMode) {
            imagesObserver?.let { sharedViewModel.localImages.removeObserver(it) }
            foldersObserver = (foldersObserver ?: Observer {
                adapter.submitList(it.data)
                if (shouldShowEmptyLayout(it)) {
                    binding.emptyLayout.viewStub?.inflate()
                }
            }).also {
                model.localFolders.observe(viewLifecycleOwner, it)
            }
        } else {
            foldersObserver?.let { model.localFolders.removeObserver(it) }
            imagesObserver = (imagesObserver ?: Observer {
                adapter.submitList(it.data)
                if (shouldShowEmptyLayout(it)) {
                    binding.emptyLayout.viewStub?.inflate()
                }
            }).also {
                sharedViewModel.localImages.observe(viewLifecycleOwner, it)
            }
        }
    }

}