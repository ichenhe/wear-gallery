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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
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
import me.chenhe.wearvision.dialog.AlertDialog
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
        ((binding.delete.layoutParams as CoordinatorLayout.LayoutParams).behavior as FabBehavior)
            .scope = viewLifecycleOwner.lifecycleScope

        loadImages()

        model.inSelectionMode.observe(viewLifecycleOwner) {
            val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            if (it) {
                binding.title.text =
                    getString(R.string.local_title_selection, adapter.checkedItem?.size ?: 0)
                binding.listGridType.setImageResource(R.drawable.ic_view_cancel)
                // show delete button
                binding.delete.apply {
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().scaleX(1f).scaleY(1f).setDuration(duration)
                        .setListener(null).start()
                }
            } else {
                binding.title.setText(R.string.drawer_local_gallery)
                val folderMode = model.folderMode.value
                if (folderMode != null)
                    binding.listGridType.setImageResource(
                        if (folderMode) R.drawable.ic_view_list else R.drawable.ic_view_grid
                    )
                // hide delete button
                binding.delete.apply {
                    animate().scaleX(0f).scaleY(0f).setDuration(duration)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                visibility = View.GONE
                            }
                        })
                }
            }
        }

        adapter.checkedItemChangedListener = { checked ->
            binding.title.text = getString(R.string.local_title_selection, checked.size)
        }

        binding.localImageHeader.setOnClickListener {
            if (adapter.inSelectionMode) {
                adapter.quitSelectionMode()
            } else {
                model.toggleListMode()
            }
        }

        // restore selection mode if view is destroyed
        // so this is a one-time operation, no need to observe
        if (model.inSelectionMode.value!! != adapter.inSelectionMode) {
            if (model.inSelectionMode.value!!)
                adapter.enterSelectionMode()
            else
                adapter.quitSelectionMode()
        }

        binding.delete.setOnClickListener {
            val checked = adapter.checkedItem
            if (checked.isNullOrEmpty())
                return@setOnClickListener
            if (model.folderMode.value != true) {
                // delete images
                val data = checked.map { (it as Image).uri }
                AlertDialog(requireContext()).apply {
                    setTitle(R.string.confirm)
                    message = getString(R.string.local_delete_images_confirm, data.size)
                    setNegativeButtonIcon(R.drawable.ic_dialog_close, null)
                    setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { _, _ ->
                        sharedViewModel.deleteLocalImages(data)
                        adapter.quitSelectionMode()
                        dismiss()
                    }
                }.show()
            } else {
                // delete folders
                val data = checked.map { it as ImageFolder }
                AlertDialog(requireContext()).apply {
                    setTitle(R.string.confirm)
                    message = getString(R.string.local_delete_image_folders_confirm, data.size)
                    setNegativeButtonIcon(R.drawable.ic_dialog_close, null)
                    setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { _, _ ->
                        sharedViewModel.deleteLocalImageFolders(data)
                        adapter.quitSelectionMode()
                        dismiss()
                    }
                }.show()
            }
        }
    }

    private fun loadImages() {
        adapter = LocalImagesAdapter(requireContext())

        adapter.selectionModeChangedListener = { inSelectionMode ->
            model.setSelectionMode(inSelectionMode) // save current selection mode
        }

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
            if (model.inSelectionMode.value != true)
                binding.listGridType.setImageResource(
                    if (folderMode) R.drawable.ic_view_list else R.drawable.ic_view_grid
                )
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