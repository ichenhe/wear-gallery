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

package cc.chenhe.weargallery.ui.folders

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseListAdapter
import cc.chenhe.weargallery.common.ui.SimpleItemDecoration
import cc.chenhe.weargallery.databinding.FrFoldersBinding
import cc.chenhe.weargallery.ui.common.BaseFr
import cc.chenhe.weargallery.ui.common.CollapseHeaderLayout
import cc.chenhe.weargallery.ui.main.PagerFrDirections
import cc.chenhe.weargallery.ui.main.SharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FoldersFr : BaseFr(), Toolbar.OnMenuItemClickListener {

    private val sharedModel by sharedViewModel<SharedViewModel>()
    private val model by viewModel<FoldersViewModel>()

    private lateinit var binding: FrFoldersBinding
    private lateinit var adapter: FoldersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FrFoldersBinding.inflate(inflater, container, false)
        binding.header.toolbar.apply {
            inflateMenu(R.menu.folders)
            setOnMenuItemClickListener(this@FoldersFr)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (binding.header.root as CollapseHeaderLayout).setTitle(R.string.nav_menu_folders)

        // RecyclerView
        adapter = FoldersAdapter(this)
        binding.foldersRecyclerView.addItemDecoration(SimpleItemDecoration(requireContext(),
                R.dimen.folders_grid_padding))
        binding.foldersRecyclerView.layoutManager = GridLayoutManager(requireContext(),
                getColumns(model.listStyle.value!!))
        binding.foldersRecyclerView.adapter = adapter

        adapter.itemClickListener = object : BaseListAdapter.SimpleItemClickListener() {
            override fun onItemClick(view: View, position: Int) {
                val action = PagerFrDirections.actionPagerFrToFolderImagesFr(adapter.currentList[position].bucketName)
                findNavController().navigate(action)
            }
        }

        // data
        model.listStyle.observe(viewLifecycleOwner) {
            (binding.foldersRecyclerView.layoutManager as GridLayoutManager).spanCount = getColumns(it)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)

            binding.header.toolbar.menu?.findItem(R.id.menu_folders_style)?.setIcon(
                    if (it) R.drawable.ic_view_list else R.drawable.ic_view_grid)
        }

        sharedModel.folderImages.observe(viewLifecycleOwner) {
            binding.header.subtitleTextView.text = requireContext().resources
                    .getQuantityString(R.plurals.folders_subtitle, it.size, it.size)
            adapter.submitList(it)
        }
    }

    private fun getColumns(isGrid: Boolean): Int {
        return if (isGrid) {
            if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5
        } else {
            1
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_folders_style -> {
                model.toggleListStyle()
            }
            R.id.menu_settings -> {
                findNavController().navigate(R.id.preferenceFr)
            }
            else -> return false
        }
        return true
    }
}