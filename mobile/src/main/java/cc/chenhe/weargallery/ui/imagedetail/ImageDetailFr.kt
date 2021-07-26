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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.util.filePath
import cc.chenhe.weargallery.databinding.FrImageDetailBinding
import cc.chenhe.weargallery.ui.legacy.SharedViewModel
import cc.chenhe.weargallery.utils.getTitleTextView
import cc.chenhe.weargallery.utils.requireCompatAty
import cc.chenhe.weargallery.utils.resetStatusBarTextColor
import cc.chenhe.weargallery.utils.setupToolbar
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*


class ImageDetailFr : Fragment() {

    @Keep
    enum class Source {
        IMAGES,
        FOLDER
    }

    private lateinit var binding: FrImageDetailBinding
    private lateinit var panelBehavior: ImageDetailPanelBehavior
    private val args: ImageDetailFrArgs by navArgs()
    private val sharedModel: SharedViewModel by sharedViewModel()
    private val modle: ImageDetailViewModel by viewModel { parametersOf(args.bucketId) }

    private lateinit var imageGestureDetector: GestureDetector
    private lateinit var adapter: ImageDetailAdapter

    private var originalNavigationBarColor = 0

    private val dateFormat by lazy {
        SimpleDateFormat(getString(R.string.date_format_full), Locale.getDefault())
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            sharedModel.currentPosition = position
            updateDetailPanel(adapter.getItemData(position))
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            performBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FrImageDetailBinding.inflate(inflater, container, false)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        setupSystemUI(binding.root)
        // toolbar
        setupToolbar(binding.imageDetailToolBar)
        requireCompatAty().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.imageDetailToolBar.apply {
            getTitleTextView()?.visibility = View.INVISIBLE
            title = getString(R.string.image_detail_title)
            setNavigationOnClickListener { performBack() }
        }

        panelBehavior = ((binding.imageDetailPanel.layoutParams as CoordinatorLayout.LayoutParams)
            .behavior as ImageDetailPanelBehavior)
        panelBehavior.onStateChangeListener = { state ->
            if (state == ImageDetailPanelBehavior.STATE_EXPANDED) {
                binding.imageDetailToolBar.getTitleTextView()?.visibility = View.VISIBLE
                setSystemBarVisibility(requireView(), true)
                requireActivity().window.statusBarColor = Color.BLACK
            } else {
                binding.imageDetailToolBar.getTitleTextView()?.visibility = View.INVISIBLE
                requireActivity().window.statusBarColor =
                    ContextCompat.getColor(requireContext(), R.color.imageDetailBarBg)
            }
        }
        return binding.root
    }

    private fun setupSystemUI(rootView: View) {
        originalNavigationBarColor = requireActivity().window.navigationBarColor
        requireActivity().window.navigationBarColor = Color.TRANSPARENT
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.imageDetailBarBg)
        WindowInsetsControllerCompat(requireActivity().window, rootView)
            .isAppearanceLightStatusBars = false
        setSystemBarVisibility(rootView, true)
        var isNavigationBarVisible = true

        imageGestureDetector =
            GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    setSystemBarVisibility(rootView, !isNavigationBarVisible)
                    return true
                }
            })

        rootView.setOnApplyWindowInsetsListener { _, insets ->
            isNavigationBarVisible = WindowInsetsCompat.toWindowInsetsCompat(insets, binding.root)
                .isVisible(WindowInsetsCompat.Type.navigationBars())
            binding.imageDetailToolBar.visibility =
                if (isNavigationBarVisible) View.VISIBLE else View.INVISIBLE
            insets
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ImageDetailAdapter(this, args.shareAnimationName, imageGestureDetector)
        binding.imageDetailPager.adapter = adapter
        binding.imageDetailPager.registerOnPageChangeCallback(onPageChangeCallback)

        // load data
        if (args.source as Source == Source.IMAGES) {
            sharedModel.images.observe(viewLifecycleOwner) {
                adapter.submitList(it) {
                    binding.imageDetailPager.setCurrentItem(sharedModel.currentPosition, false)
                }
            }
        } else if (args.source as Source == Source.FOLDER) {
            modle.images.observe(viewLifecycleOwner) { images ->
                adapter.submitList(images) {
                    binding.imageDetailPager.setCurrentItem(sharedModel.currentPosition, false)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDetailPanel(image: Image) {
        binding.imageDetailDate.text = dateFormat.format(Date(image.takenTime))
        binding.imageDetailFileName.text = image.name
        binding.imageDetailFilePath.text = image.file?.filePath ?: image.bucketName
        binding.imageDetailInfo.text = "${image.getSizeStr()}    ${image.width}×${image.height}"
    }

    private fun performBack() {
        if (panelBehavior.getState() != ImageDetailPanelBehavior.STATE_HIDDEN) {
            panelBehavior.setState(ImageDetailPanelBehavior.STATE_HIDDEN)
        } else {
            findNavController().navigateUp()
        }
    }

    private fun setSystemBarVisibility(rootView: View, visible: Boolean) {
        if (visible)
            WindowInsetsControllerCompat(requireActivity().window, rootView).apply {
                show(WindowInsetsCompat.Type.systemBars())
            }
        else
            WindowInsetsControllerCompat(requireActivity().window, rootView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
    }

    override fun onDestroyView() {
        setSystemBarVisibility(requireView(), true)
        requireActivity().window.apply {
            navigationBarColor = originalNavigationBarColor
            statusBarColor = Color.TRANSPARENT
        }
        requireActivity().resetStatusBarTextColor(binding.root)
        binding.imageDetailPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }

}