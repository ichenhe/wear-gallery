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
import androidx.recyclerview.widget.LinearLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.*
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.databinding.FrLocalImagesBinding
import cc.chenhe.weargallery.ui.imagedetail.local.LocalImageDetailFr
import cc.chenhe.weargallery.ui.main.PagerFrDirections
import cc.chenhe.weargallery.ui.main.SharedViewModel
import cc.chenhe.weargallery.uilts.shouldShowEmptyLayout
import me.chenhe.wearvision.dialog.AlertDialog
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocalImagesFr : Fragment() {

    private val sharedViewModel: SharedViewModel by sharedViewModel()
    private val model: LocalImagesViewModel by viewModel()

    private lateinit var binding: FrLocalImagesBinding

    private var imagesObserver: Observer<Success<List<Image>>>? = null
    private var foldersObserver: Observer<in Resource<out List<ImageFolder>>>? = null

    /** Get the current adapter that related to list. */
    private val adapter: LocalImagesBaseAdapter<*>?
        get() = binding.imagesRecyclerView.adapter as? LocalImagesBaseAdapter<*>

    // Here is null-safe because the live data has initial value.
    private val inFolderView: Boolean get() = model.folderMode.value!!

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

        model.folderMode.observe(viewLifecycleOwner) { folderMode ->
            resetRecyclerView(folderMode, binding.imagesRecyclerView.adapter != null)
        }

        model.inSelectionMode.observe(viewLifecycleOwner) {
            val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            if (it) {
                binding.title.text =
                    getString(R.string.local_title_selection, adapter?.checkedItem?.size ?: 0)
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
                binding.listGridType.setImageResource(
                    if (inFolderView) R.drawable.ic_view_list else R.drawable.ic_view_grid
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

        binding.localImageHeader.setOnClickListener {
            if (adapter?.inSelectionMode == true) {
                requireNotNull(adapter).quitSelectionMode()
            } else {
                model.toggleListMode()
            }
        }

        binding.delete.setOnClickListener {
            val inFolderView = requireNotNull(adapter) is LocalImagesFolderAdapter
            val checked = requireNotNull(adapter).checkedItem
            if (checked.isNullOrEmpty())
                return@setOnClickListener
            if (!inFolderView) {
                // delete images
                val data = checked.map { (it as Image).uri }
                AlertDialog(requireContext()).apply {
                    setTitle(R.string.confirm)
                    message = resources.getQuantityString(
                        R.plurals.local_delete_images_confirm,
                        data.size,
                        data.size
                    )
                    setNegativeButtonIcon(R.drawable.ic_dialog_close, null)
                    setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { _, _ ->
                        sharedViewModel.deleteLocalImages(data)
                        adapter?.quitSelectionMode()
                        dismiss()
                    }
                }.show()
            } else {
                // delete folders
                val data = checked.map { it as ImageFolder }
                AlertDialog(requireContext()).apply {
                    setTitle(R.string.confirm)
                    message = resources.getQuantityString(
                        R.plurals.local_delete_image_folders_confirm,
                        data.size,
                        data.size
                    )
                    setNegativeButtonIcon(R.drawable.ic_dialog_close, null)
                    setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { _, _ ->
                        sharedViewModel.deleteLocalImageFolders(data)
                        adapter?.quitSelectionMode()
                        dismiss()
                    }
                }.show()
            }
        }
    }

    private fun resetRecyclerView(folderMode: Boolean, animation: Boolean) {
        binding.listGridType.setImageResource(
            if (folderMode) R.drawable.ic_view_list else R.drawable.ic_view_grid
        )
        if (!animation) {
            resetAdapterAndObserver(folderMode)
            return
        }
        val duration =
            requireContext().resources.getInteger(android.R.integer.config_shortAnimTime)
        binding.imagesRecyclerView.animate().setDuration(duration.toLong()).alpha(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    binding.imagesRecyclerView.adapter = null
                    resetAdapterAndObserver(folderMode)
                    binding.imagesRecyclerView.animate().setDuration(duration.toLong())
                        .alpha(1f).setListener(null).start()
                }
            })
            .start()
    }

    /**
     * Create a new adapter that matches the given view type and set it to the list view.
     * Meanwhile create a observer if needed and start observe the corresponding data source.
     * And remove unnecessary observer.
     *
     * This method should only be called in [resetRecyclerView].
     */
    private fun resetAdapterAndObserver(folderMode: Boolean) {
        val newAdapter = createAdapter(folderMode)
        if (newAdapter is LocalImagesFolderAdapter) {
            // folder mode
            binding.imagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            imagesObserver?.also { sharedViewModel.localImages.removeObserver(it) }
        } else if (newAdapter is LocalImagesGridAdapter) {
            // grid mode
            binding.imagesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

            foldersObserver?.also { model.localFolders.removeObserver(it) }
        }

        binding.imagesRecyclerView.adapter = newAdapter
        if (newAdapter is LocalImagesFolderAdapter) {
            foldersObserver = (foldersObserver ?: Observer {
                val currentAdapter = binding.imagesRecyclerView.adapter ?: return@Observer
                if (currentAdapter is LocalImagesFolderAdapter) {
                    currentAdapter.submitList(it.data)
                    if (shouldShowEmptyLayout(it)) {
                        binding.emptyLayout.viewStub?.inflate()
                    }
                }
            }).also {
                model.localFolders.observe(viewLifecycleOwner, it)
            }
        } else if (newAdapter is LocalImagesGridAdapter) {
            imagesObserver = (imagesObserver ?: Observer {
                val currentAdapter = binding.imagesRecyclerView.adapter ?: return@Observer
                if (currentAdapter is LocalImagesGridAdapter) {
                    currentAdapter.submitList(it.data)
                    if (shouldShowEmptyLayout(it)) {
                        binding.emptyLayout.viewStub?.inflate()
                    }
                }
            }).also {
                sharedViewModel.localImages.observe(viewLifecycleOwner, it)
            }
        }
    }

    /**
     * Create a new adapter that matches the given view type.
     */
    private fun createAdapter(folderMode: Boolean): LocalImagesBaseAdapter<*> {
        val adapter: LocalImagesBaseAdapter<*>
        if (folderMode) {
            adapter = LocalImagesFolderAdapter(requireContext()).apply {
                itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
                    override fun onItemClick(view: View, position: Int) {
                        sharedViewModel.currentPosition = position
                        val itemData = requireNotNull(getItemData(position))
                        val action = PagerFrDirections.actionPagerFrToLocalImageDetailFr(
                            LocalImageDetailFr.Source.FOLDER,
                            itemData.id,
                            itemData.imgNum
                        )
                        findNavController().navigate(action)
                    }
                }
            }
        } else {
            adapter = LocalImagesGridAdapter(requireContext()).apply {
                itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
                    override fun onItemClick(view: View, position: Int) {
                        sharedViewModel.currentPosition = position
                        val action = PagerFrDirections.actionPagerFrToLocalImageDetailFr(
                            LocalImageDetailFr.Source.IMAGES,
                            LocalImageDetailFr.BUCKET_ID_NA,
                            itemCount,
                        )
                        findNavController().navigate(action)
                    }
                }
            }
        }
        adapter.selectionModeChangedListener = { inSelectionMode ->
            model.setSelectionMode(inSelectionMode) // save current selection mode
        }

        adapter.checkedItemChangedListener = { checked ->
            binding.title.text = getString(R.string.local_title_selection, checked.size)
        }

        // restore selection mode if view is destroyed
        // so this is a one-time operation, no need to observe
        if (model.inSelectionMode.value!! != adapter.inSelectionMode) {
            if (model.inSelectionMode.value!!)
                adapter.enterSelectionMode()
            else
                adapter.quitSelectionMode()
        }

        return adapter
    }

    override fun onDestroyView() {
        binding.imagesRecyclerView.adapter = null  // trigger adapter's onDetachedFromRecyclerView
        super.onDestroyView()
    }

}