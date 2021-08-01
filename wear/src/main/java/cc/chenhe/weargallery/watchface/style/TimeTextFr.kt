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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.core.view.children
import androidx.viewbinding.ViewBinding
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.databinding.WfFrTimeTextBinding
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.uilts.toast
import com.google.android.material.chip.Chip
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

private const val TAG = "TimeTextFr"

class TimeTextFr : SwipeDismissFr() {

    private lateinit var binding: WfFrTimeTextBinding
    private val model by viewModel<TimeTextFormatViewModel>()

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ViewBinding {
        return WfFrTimeTextBinding.inflate(inflater, container, false).also {
            binding = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPreference()
        binding.tagGroup.children.forEach { v ->
            if (v is Chip) {
                v.setOnClickListener(onTagClickListener)
            }
        }
        binding.reset.setOnClickListener {
            model.resetPreference()
            loadPreference()
            toast(R.string.wf_preference_format_reset_done)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.savePreference()
    }

    private fun loadPreference() {
        binding.checkedGroup.removeAllViews()
        model.loadPreference()
        model.tags.forEach { tag ->
            val resId = model.tagsNameIds[tag]
            if (resId != null) {
                addTagChip(getString(resId), false)
            } else {
                Timber.tag(TAG).w("Can not find tag's display name res id. tag=$tag")
            }
        }
    }

    private fun addTagChip(displayName: CharSequence, scrollToEnd: Boolean) {
        val chip: Chip = LayoutInflater.from(binding.checkedGroup.context).inflate(
            R.layout.wf_view_time_text_chip,
            binding.checkedGroup, false
        ) as Chip
        chip.text = displayName
        chip.setOnCloseIconClickListener(onChipCloseListener)
        binding.checkedGroup.addView(chip)
        if (scrollToEnd) {
            binding.checkedGroupContainer.post {
                binding.checkedGroupContainer.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
            }
        }
    }

    private val onChipCloseListener = View.OnClickListener { v ->
        if (v !is Chip) {
            return@OnClickListener
        }
        val childIndex = binding.checkedGroup.indexOfChild(v)
        model.removeTag(childIndex)
        binding.checkedGroup.removeView(v)
    }

    private val onTagClickListener = View.OnClickListener { v ->
        if (v is Chip) {
            // add tag
            val tag = model.tagsMap[v.id] ?: return@OnClickListener
            model.addTag(tag)
            addTagChip(v.text, true)
        }
    }

}