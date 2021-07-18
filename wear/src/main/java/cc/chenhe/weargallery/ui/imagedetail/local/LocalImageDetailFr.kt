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
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseAdapter
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseFr
import cc.chenhe.weargallery.ui.imagedetail.ImageDetailBaseViewModel
import cc.chenhe.weargallery.ui.main.SharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocalImageDetailFr : ImageDetailBaseFr() {

    companion object {
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

    private val sharedModel: SharedViewModel by sharedViewModel()
    private val model: LocalImageDetailViewModel by viewModel()

    private val args: LocalImageDetailFrArgs by navArgs()

    private lateinit var adapter: LocalImageDetailAdapter

    private var shouldInitCurrentPosition = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageDetailPager.adapter = adapter

        if (args.sourceType as Source == Source.IMAGES) {
            model.currentItem.value = sharedModel.currentPosition
            model.addImageDataSource(sharedModel.localImages)
            model.currentItem.observe(viewLifecycleOwner) { currentItem ->
                sharedModel.currentPosition = currentItem
            }
        } else if (args.sourceType as Source == Source.FOLDER) {
            val args = LocalImageDetailFrArgs.fromBundle(requireArguments())
            model.addFolderDataSource(sharedModel.localFolderImages, args.bucketId)
        }

        model.images.observe(viewLifecycleOwner) { images ->
            if (images is Success && (images.data?.isEmpty() == true)) {
                findNavController().navigateUp()
            } else {
                adapter.submitList(images.data) {
                    if (args.sourceType as Source == Source.IMAGES && shouldInitCurrentPosition) {
                        // Jump to the clicked position
                        binding.imageDetailPager.setCurrentItem(sharedModel.currentPosition, false)
                        shouldInitCurrentPosition = false
                    }
                }
            }
        }
    }

    override fun createAdapter(): ImageDetailBaseAdapter<*, *> {
        return LocalImageDetailAdapter().also { adapter = it }
    }

    override fun getViewModel(): ImageDetailBaseViewModel<*> = model

    override fun onLoadHd() {
        throw NotImplementedError() // should never called in local list
    }

    override fun onDelete() {
        model.currentItemData.value?.let { currentImage ->
            sharedModel.deleteLocalImage(currentImage.uri)
        }
    }

    override fun retry() {
        throw NotImplementedError() // should never called in local list
    }

}