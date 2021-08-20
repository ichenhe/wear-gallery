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

package cc.chenhe.weargallery.ui.imagedetail.local

import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseAdapter
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseFr
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import cc.chenhe.weargallery.ui.main.SharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class LocalImageDetailFr : ImageDetailBaseFr<Image>() {

    companion object {
        private const val TAG = "LocalImageDetailFr"

        /**
         * Used as a placeholder in grid mode.
         */
        const val BUCKET_ID_NA = -1
    }

    @Keep
    enum class Source {
        IMAGES,
        FOLDER
    }

    private val args: LocalImageDetailFrArgs by navArgs()
    private val sharedModel: SharedViewModel by sharedViewModel()
    private val model: LocalImageDetailViewModel by viewModel {
        parametersOf(args.sourceType, args.bucketId)
    }


    private lateinit var adapter: LocalImageDetailAdapter

    // used to jump to the initial position
    private var loadStateListener: ((CombinedLoadStates) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.sourceType as Source == Source.IMAGES) {
            model.setCurrentItem(
                sharedModel.currentPosition,
                adapter.getItemData(sharedModel.currentPosition)
            )
            model.currentItem.observe(viewLifecycleOwner) { currentItem ->
                sharedModel.currentPosition = currentItem
            }

            loadStateListener = { states: CombinedLoadStates ->
                if (states.refresh is LoadState.NotLoading) {
                    loadStateListener?.also { adapter.removeLoadStateListener(it) }
                    loadStateListener = null
                    // Jump to the clicked position
                    binding.imageDetailPager.setCurrentItem(sharedModel.currentPosition, false)
                }
            }
            adapter.addLoadStateListener(requireNotNull(loadStateListener))
        }

        adapter.addLoadStateListener { states: CombinedLoadStates ->
            if (states.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                Timber.tag(TAG).i("No picture, close detail fragment.")
                findNavController().navigateUp()
            }
        }
    }

    override fun createAdapter(): ImageDetailBaseAdapter<Image, *> {
        return LocalImageDetailAdapter().also { adapter = it }
    }

    override fun getViewModel(): ImageDetailBaseViewModel<Image> = model

    override fun getCachedTotalCount(): Int = args.totalCount

    override fun onLoadHd() {
        throw NotImplementedError() // should never called in local list
    }

    override fun onDelete() {
        model.currentItemData.value?.let { currentImage ->
            sharedModel.deleteLocalImage(currentImage.uri)
            // We don't know whether the deletion is success. So don't refresh UI here.
        }
    }

    override fun retry() {
        throw NotImplementedError() // should never called in local list
    }

}