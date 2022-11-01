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

package cc.chenhe.weargallery.ui.imagedetail

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Error
import cc.chenhe.weargallery.common.bean.Loading
import cc.chenhe.weargallery.common.bean.Success
import cc.chenhe.weargallery.databinding.FrImageDetailBinding
import cc.chenhe.weargallery.ui.common.RetryCallback
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.view.LongPressImageView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * A fragment with basic UI components and responding to basic operations to display image details. This class has
 * implemented controls for the title, zoom buttons, and menus. You should always use this class with
 * [ImageDetailBaseViewModel] and [ImageDetailBaseAdapter] together to provides the necessary data sources and
 * operational implementations.
 */
abstract class ImageDetailBaseFr<T : Any> : SwipeDismissFr(), View.OnClickListener,
    LongPressImageView.OnLongPressListener, RetryCallback {

    protected lateinit var binding: FrImageDetailBinding
    private lateinit var adapter: ImageDetailBaseAdapter<T, *>
    private lateinit var menuBehavior: ImageDetailOperationBehavior

    private var fadeAnimationDuration: Int = 0

    @CallSuper
    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ViewBinding {
        return FrImageDetailBinding.inflate(inflater, container, false).also {
            binding = it
            it.lifecycleOwner = viewLifecycleOwner
            it.onClickListener = this
            it.onLongPressListener = this
            it.retryCallback = this
            it.model = getViewModel()
        }
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fadeAnimationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)
        menuBehavior = ((binding.operationMenu.root.layoutParams as CoordinatorLayout.LayoutParams)
            .behavior) as ImageDetailOperationBehavior

        binding.imageDetailPager.apply {
            registerOnPageChangeCallback(onPageChangeCallback)
            // disable viewpager2's over scroll mode
            getChildAt(0)?.let { c0 ->
                if (c0 is RecyclerView) {
                    c0.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                }
            }
        }

        val model = getViewModel()
        adapter = createAdapter().apply {
            itemImageViewClickListener = { _ -> model.toggleWidgetsVisibility() }
        }

        model.titleVisibility.observe(viewLifecycleOwner) { titleVisibility ->
            if (titleVisibility) {
                binding.imageDetailTitleLayout.fadeIn()
            } else {
                binding.imageDetailTitleLayout.fadeOut()
            }
        }

        model.zoomButtonVisibility.observe(viewLifecycleOwner) { zoomVisibility ->
            if (zoomVisibility) {
                binding.imageDetailZoomIn.fadeIn()
                binding.imageDetailZoomOut.fadeIn()
            } else {
                binding.imageDetailZoomIn.fadeOut()
                binding.imageDetailZoomOut.fadeOut()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.pagingImages.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        adapter.addLoadStateListener { combinedLoadStates ->
            if (combinedLoadStates.refresh != model.initialLoadingState.value) {
                model.initialLoadingState.value = combinedLoadStates.refresh
            }

            // Refresh the visibility of fragment-level state indicator
            val isEmpty = adapter.itemCount == 0
            if (combinedLoadStates.refresh is LoadState.NotLoading) {
                // A workaround that is compatible with legacy layout structure.
                binding.res = Success(if (isEmpty) emptyList() else listOf(Unit))
                if (isEmpty) {
                    binding.emptyLayout.viewStub?.inflate()
                }
            } else if (combinedLoadStates.refresh is LoadState.Loading) {
                binding.res = Loading(if (isEmpty) null else listOf(Unit))
                // only show the loading layout if there are no cache data
                if (isEmpty) {
                    binding.loadingLayout.viewStub?.inflate()
                }
            } else {
                binding.res = Error(0, "", if (isEmpty) null else listOf(Unit))
                if (isEmpty) {
                    binding.retryLayout.viewStub?.inflate()
                }
            }

            // update current data
            if (combinedLoadStates.refresh !is LoadState.Loading
                && combinedLoadStates.append !is LoadState.Loading
                && combinedLoadStates.prepend !is LoadState.Loading
            ) {
                model.setCurrentItem(data = adapter.getItemData(model.currentItem.value!!))
            }


            // refresh total count
            if (combinedLoadStates.append.endOfPaginationReached) {
                model.setTotalCount(adapter.itemCount)
            } else {
                model.setTotalCount(max(adapter.itemCount, getCachedTotalCount()))
            }
        }

        model.resetWidgetsVisibilityCountdown(true)

        binding.imageDetailPager.adapter = adapter.withLoadStateFooter(ImageDetailLoadStateAdapter {
            adapter.retry()
        })
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {

        private var lastSelectTime = 0L
        private var pendingJob: Job? = null

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager2.SCROLL_STATE_DRAGGING || state == ViewPager2.SCROLL_STATE_SETTLING) {
                getViewModel().resetWidgetsVisibilityCountdown(true)
            }
        }

        override fun onPageSelected(position: Int) {
            getViewModel().setCurrentItem(position, adapter.getItemData(position))
            val t = SystemClock.uptimeMillis()
            if (t - lastSelectTime <= 700) {
                setPending()
                pendingJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    adapter.pending = false
                }
            }
            lastSelectTime = t
        }

        private fun setPending() {
            pendingJob?.cancel()
            adapter.pending = true
        }
    }

    abstract fun getCachedTotalCount(): Int

    abstract fun getViewModel(): ImageDetailBaseViewModel<T>

    /**
     * Called in [onViewCreated].
     */
    abstract fun createAdapter(): ImageDetailBaseAdapter<T, *>

    /**
     * Called when the load HD button is clicked.
     */
    abstract fun onLoadHd()

    /**
     * Called when the delete button is clicked.
     */
    abstract fun onDelete()


    @CallSuper
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imageOperationTurnLeft -> {
                // Do not use -90 :)
                adapter.rotateBy(binding.imageDetailPager.currentItem, 270)
            }
            R.id.imageOperationTurnRight -> {
                adapter.rotateBy(binding.imageDetailPager.currentItem, 90)
            }
            R.id.imageOperationHd -> {
                onLoadHd()
                menuBehavior.setMenuState(ImageDetailOperationBehavior.STATE_HIDDEN)
            }
            R.id.imageOperationDelete -> {
                onDelete()
                menuBehavior.setMenuState(ImageDetailOperationBehavior.STATE_HIDDEN)
            }
            R.id.imageDetailZoomIn -> {
                getViewModel().resetWidgetsVisibilityCountdown(false)
                adapter.zoom(
                    binding.imageDetailPager.currentItem,
                    zoomIn = true,
                    consecutive = false
                )
            }
            R.id.imageDetailZoomOut -> {
                getViewModel().resetWidgetsVisibilityCountdown(false)
                adapter.zoom(
                    binding.imageDetailPager.currentItem,
                    zoomIn = false,
                    consecutive = false
                )
            }
        }
    }

    @CallSuper
    override fun onLongPress(view: LongPressImageView) {
        getViewModel().stopWidgetsVisibilityCountdown()
        when (view.id) {
            R.id.imageDetailZoomIn -> {
                adapter.zoom(
                    binding.imageDetailPager.currentItem,
                    zoomIn = true,
                    consecutive = true
                )
            }
            R.id.imageDetailZoomOut -> {
                adapter.zoom(
                    binding.imageDetailPager.currentItem,
                    zoomIn = false,
                    consecutive = true
                )
            }
        }
    }

    @CallSuper
    override fun onRelease(view: LongPressImageView) {
        getViewModel().resetWidgetsVisibilityCountdown(false)
    }

    private fun View.fadeIn() {
        animate()
            .alpha(1f)
            .setDuration(fadeAnimationDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    visibility = View.VISIBLE
                }
            })
    }

    private fun View.fadeOut() {
        val view = this
        animate()
            .alpha(0f)
            .setDuration(fadeAnimationDuration.toLong())
            .withEndAction {
                view.visibility = View.GONE
            }
    }
}