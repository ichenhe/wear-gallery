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

package cc.chenhe.weargallery.watchface.style

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.databinding.WfFrTimeTextColorBinding
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.uilts.toast
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.viewModel


private const val TYPE_COLOR = 1
private const val TYPE_TEXT = 2

private const val INDEX_CURRENT_COLOR_CHIP = 0
private const val INDEX_CURRENT_COLOR_TEXT = 1
private const val COLOR_LIST_OFFSET = 2

class TimeTextColorFr : SwipeDismissFr() {

    private lateinit var binding: WfFrTimeTextColorBinding
    private val model by viewModel<TimeTextColorViewModel>()

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )
            : ViewBinding {
        return WfFrTimeTextColorBinding.inflate(inflater, container, false).also {
            binding = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.layoutManager = GridLayoutManager(requireContext(), 3).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (position == 1) 2 else 1
            }
        }
        binding.list.adapter = ColorAdapter()

        model.color.observe(viewLifecycleOwner) { color ->
            if (color != null) {
                binding.list.adapter?.notifyItemRangeChanged(INDEX_CURRENT_COLOR_CHIP, 2, true)
            }
        }
    }

    override fun onDestroy() {
        model.saveColor()
        super.onDestroy()
    }

    private inner class ColorAdapter : RecyclerView.Adapter<BaseViewHolder>() {

        private val colorsHex = arrayOf(
            "#FFFFFF", "#000000", "#55565A",
            "#E13025", "#FF9743", "#FDD249",
            "#5EB27E", "#5D84E1", "#9475D2"
        )

        override fun getItemViewType(position: Int): Int {
            return if (position == INDEX_CURRENT_COLOR_TEXT) {
                TYPE_TEXT
            } else {
                TYPE_COLOR
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                TYPE_COLOR -> {
                    val lp = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val chip = Chip(parent.context).also { chip ->
                        chip.isCheckedIconVisible = false
                        chip.layoutParams = lp
                    }
                    BaseViewHolder(chip)
                }
                TYPE_TEXT -> {
                    BaseViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.wf_rv_item_color, parent, false)
                    )
                }
                else -> throw  IllegalArgumentException("Unknown view type.")
            }
        }

        override fun getItemCount(): Int = colorsHex.size + COLOR_LIST_OFFSET

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            when (position) {
                INDEX_CURRENT_COLOR_CHIP -> {
                    (holder.itemView as Chip).apply {
                        setChipBackgroundColor(model.color.value ?: Color.BLACK)
                        setOnClickListener(null)
                    }
                }
                INDEX_CURRENT_COLOR_TEXT -> {
                    holder.itemView.setOnClickListener(null)
                    val et: EditText = holder.getView(R.id.colorHex)
                    val s = model.color.value?.let { String.format("%06X", 0xFFFFFF and it) }
                    et.setText(s)
                    et.setOnEditorActionListener { textView, i, _ ->
                        if (i == EditorInfo.IME_ACTION_DONE) {
                            try {
                                val newColor = Color.parseColor(
                                    "#" + textView.text.toString().replace(" ", "")
                                )
                                model.setColor(newColor)
                            } catch (e: Exception) {
                                toast(R.string.wf_preference_color_error)
                            }
                            (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                                ?.hideSoftInputFromWindow(textView.windowToken, 0)
                            true
                        } else {
                            false
                        }
                    }
                }
                else -> {
                    bindColorListView(holder, position - COLOR_LIST_OFFSET)
                }
            }
        }

        private fun bindColorListView(holder: BaseViewHolder, listPosition: Int) {
            val color = Color.parseColor(colorsHex[listPosition])
            val chip = holder.itemView as Chip
            chip.setChipBackgroundColor(color)
            holder.itemView.setOnClickListener {
                val i: Int = holder.bindingAdapterPosition - COLOR_LIST_OFFSET
                if (i in colorsHex.indices) {
                    model.setColor(Color.parseColor(colorsHex[i]))
                }
            }
        }
    }

    /**
     * Set chip background color to the given single color for all states. If the contrast with black is less than a
     * certain value, a border will be automatically set.
     */
    private fun Chip.setChipBackgroundColor(@ColorInt color: Int) {
        val colorList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(color))
        this.chipBackgroundColor = colorList
        if (ColorUtils.calculateContrast(color, Color.BLACK) < 2) {
            this.setChipStrokeWidthResource(R.dimen.wf_time_text_color_dark_stroke_width)
        } else {
            this.chipStrokeWidth = 0f
        }
    }

}