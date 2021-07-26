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

package cc.chenhe.weargallery.ui.folderimages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.SimpleItemDecoration
import cc.chenhe.weargallery.databinding.FrFolderImagesBinding
import cc.chenhe.weargallery.ui.legacy.SharedViewModel
import cc.chenhe.weargallery.utils.requireCompatAty
import cc.chenhe.weargallery.utils.setupToolbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FolderImagesFr : Fragment() {

    private lateinit var binding: FrFolderImagesBinding

    private val sharedModel by sharedViewModel<SharedViewModel>()
    private val args: FolderImagesFrArgs by navArgs()
    private val model by viewModel<FolderImagesViewModel> { parametersOf(args.bucketId) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrFolderImagesBinding.inflate(inflater, container, false)
        // toolbar
        setupToolbar(binding.header.toolbar)
        binding.header.root.setTitle(args.bucketName)
        requireCompatAty().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.header.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // RecyclerView
        binding.imagesRecyclerView.addItemDecoration(
            SimpleItemDecoration(
                requireContext(),
                R.dimen.images_grid_padding
            )
        )
        binding.imagesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        val adapter = FolderImagesAdapter(this)
        binding.imagesRecyclerView.adapter = adapter
        adapter.itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
            override fun onItemClick(view: View, position: Int) {
                sharedModel.currentPosition = position
                val action = FolderImagesFrDirections.actionFolderImagesFrToImageDetailFr(
                    shareAnimationName = "",
                    bucketId = args.bucketId,
                )
                findNavController().navigate(action)
            }
        }

        // Data
        model.images.observe(viewLifecycleOwner) { images ->
            if (images != null) {
                binding.header.subtitleTextView.text = resources.getQuantityString(
                    R.plurals.images_subtitle,
                    images.size, images.size
                )
                adapter.submitList(images)
            }
        }
    }
}