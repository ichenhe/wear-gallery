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

package cc.chenhe.weargallery.ui.images

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.SimpleItemDecoration
import cc.chenhe.weargallery.databinding.FrImagesBinding
import cc.chenhe.weargallery.ui.common.DragSelectProcessor
import cc.chenhe.weargallery.ui.common.DragSelectTouchListener
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailFr
import cc.chenhe.weargallery.ui.legacy.PagerFrDirections
import cc.chenhe.weargallery.ui.legacy.SharedViewModel
import cc.chenhe.weargallery.ui.sendimages.SendImagesAty
import cc.chenhe.weargallery.utils.calculateImageColumnCount
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class ImagesFr : Fragment(), Toolbar.OnMenuItemClickListener {

    private val sharedModel: SharedViewModel by sharedViewModel()
    private val model: ImagesViewModel by viewModel()

    private lateinit var binding: FrImagesBinding
    private lateinit var adapter: ImagesAdapter

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (model.inSelectionMode.value!!) {
                model.inSelectionMode.value = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrImagesBinding.inflate(inflater, container, false)
        binding.header.toolbar.apply {
            inflateMenu(R.menu.images)
            setOnMenuItemClickListener(this@ImagesFr)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        setupRecyclerView()

        model.columnWidth.observe(viewLifecycleOwner) { itemWidth ->
            if (binding.imagesRecyclerView.isLaidOut) {
                setSpanCount(calculateImageColumnCount(binding.imagesRecyclerView.width, itemWidth))
            }
        }

        model.inSelectionMode.observe(viewLifecycleOwner) {
            onBackPressedCallback.isEnabled = it
            binding.header.toolbar.menu.findItem(R.id.menu_images_check_all)?.isVisible = it
            binding.header.toolbar.menu.findItem(R.id.menu_send)?.isVisible = it
            if (it) {
                binding.header.root.setTitle(R.string.images_selected_none)
                adapter.enterSelectMode()
            } else {
                binding.header.root.setTitle(R.string.nav_menu_images)
                adapter.exitSelectMode()
            }
        }

        sharedModel.groupImages.observe(viewLifecycleOwner) {
            adapter.submitData(it)
            val num = it.sumOf { item -> item.children.size }
            binding.header.subtitleTextView.text =
                resources.getQuantityString(R.plurals.images_subtitle, num, num)
        }
    }

    private fun setSpanCount(count: Int) {
        val lm = binding.imagesRecyclerView.layoutManager as? GridLayoutManager
        if (lm == null) {
            val manager = GridLayoutManager(requireContext(), count)
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter.isGroupItem(position)) manager.spanCount else 1
                }
            }
            binding.imagesRecyclerView.layoutManager = manager

        } else {
            if (lm.spanCount != count) {
                lm.spanCount = count
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        // Zoom
        val scaleGestureDetector = ScaleGestureDetector(requireContext(), object :
            ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                when {
                    abs(detector.scaleFactor) < 0.1 -> return
                    detector.scaleFactor > 1 -> model.minusColumn()
                    else -> model.addColumn()
                }
            }
        })
        binding.imagesRecyclerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }

        binding.imagesRecyclerView.addItemDecoration(
            SimpleItemDecoration(
                requireContext(),
                R.dimen.images_grid_padding
            )
        )
        adapter = ImagesAdapter(
            this,
            model.getSelectionStatus(),
            sharedModel.getGroupBasedCurrentPosition()
        )
        binding.imagesRecyclerView.adapter = adapter
        binding.imagesRecyclerView.post {
            setSpanCount(
                calculateImageColumnCount(
                    binding.imagesRecyclerView.width,
                    model.columnWidth.value!!
                )
            )
        }

        // drag selection
        val dragSelectProcessor = DragSelectProcessor(DragSelectProcessor.Mode.Simple, object :
            DragSelectProcessor.SimpleSelectionHandler() {
            override fun updateSelection(
                start: Int,
                end: Int,
                isSelected: Boolean,
                calledFromOnStart: Boolean
            ) {
                adapter.selectRange(start, end, isSelected)
            }
        })
        val dragSelectTouchListener =
            DragSelectTouchListener().withSelectListener(dragSelectProcessor)

        adapter.itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {

            override fun onItemClick(view: View, position: Int) {
                super.onItemClick(view, position)
                if (!adapter.isInSelectMode() && adapter.isChildItem(position)) {
                    sharedModel.currentPosition = adapter.getImageListIndex(position)
                    // Nav to detail fragment
                    val imageView = view.findViewById<ImageView>(R.id.itemImageView)
                    val action =
                        PagerFrDirections.actionPagerFrToImageDetailFr(
                            ImageDetailFr.Source.IMAGES,
                            shareAnimationName = imageView.transitionName,
                            -1
                        )
                    val extras = FragmentNavigatorExtras(
                        imageView to imageView.transitionName
                    )
                    findNavController().navigate(action, extras)
                }
            }

            override fun onItemLongClick(view: View, position: Int): Boolean {
                model.inSelectionMode.value = true
                dragSelectTouchListener.startDragSelection(position)
                return true
            }
        }
        adapter.onSelectChangedCallback = { count ->
            if (count == 0) {
                binding.header
                binding.header.root.setTitle(getString(R.string.images_selected_none))
            } else {
                binding.header.root.setTitle(
                    getString(
                        R.string.images_selected_num,
                        count
                    )
                )
            }
            model.saveSelectionStatus(adapter.getSelected())
        }
        binding.imagesRecyclerView.addOnItemTouchListener(dragSelectTouchListener)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_images_check_all -> {
                adapter.setSelectAll(adapter.getSelected().size != adapter.getChildCount())
            }
            R.id.menu_send -> {
                val selected = adapter.getSelected()

                val intent = Intent(requireContext(), SendImagesAty::class.java).apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList(selected.map { it.uri })
                    )
                }
                startActivity(intent)
                model.inSelectionMode.value = false
            }
            else -> return false
        }
        return true
    }

}