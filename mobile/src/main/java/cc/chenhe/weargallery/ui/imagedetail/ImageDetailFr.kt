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
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.util.filePath
import cc.chenhe.weargallery.databinding.FrImageDetailBinding
import cc.chenhe.weargallery.ui.common.BaseFr
import cc.chenhe.weargallery.ui.common.requireCompatAty
import cc.chenhe.weargallery.ui.common.setupToolbar
import cc.chenhe.weargallery.ui.main.MainAty
import cc.chenhe.weargallery.ui.main.SharedViewModel
import cc.chenhe.weargallery.utils.getTitleTextView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.text.SimpleDateFormat
import java.util.*


class ImageDetailFr : BaseFr() {

    @Keep
    enum class Source {
        IMAGES,
        FOLDER
    }

    private lateinit var binding: FrImageDetailBinding
    private lateinit var panelBehavior: ImageDetailPanelBehavior
    private val sharedModel: SharedViewModel by sharedViewModel()

    private lateinit var imageGestureDetector: GestureDetector
    private lateinit var adapter: ImageDetailAdapter

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
        setupSystemUI()
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
                showSystemUi()
                setStatusBarColor(Color.BLACK)
            } else {
                binding.imageDetailToolBar.getTitleTextView()?.visibility = View.INVISIBLE
                setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.imageDetailBarBg))
            }
        }
        return binding.root
    }

    private fun setupSystemUI() {
        showSystemUi()
        setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.imageDetailBarBg))
        val aty = requireActivity() as MainAty
        imageGestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if (aty.isSystemUIVisible()) {
                    hideSystemUI()
                } else {
                    showSystemUi()
                }
                return true
            }
        })

        setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // visible
                binding.imageDetailToolBar.visibility = View.VISIBLE
            } else {
                // invisible
                binding.imageDetailToolBar.visibility = View.INVISIBLE
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = ImageDetailFrArgs.fromBundle(requireArguments())

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
            sharedModel.folderImages.observe(viewLifecycleOwner) { folders ->
                folders.forEach {
                    if (it.bucketName == args.sourceAttr) {
                        adapter.submitList(it.children) {
                            binding.imageDetailPager.setCurrentItem(
                                sharedModel.currentPosition,
                                false
                            )
                        }
                        return@observe
                    }
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

    private fun showSystemUi() {
        requireActivity().window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun hideSystemUI() {
        requireActivity().window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onDestroyView() {
        (requireActivity() as MainAty).resetSystemUi()
        resetStatusBarColor()
        binding.imageDetailPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }

}