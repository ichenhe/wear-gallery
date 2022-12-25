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

package cc.chenhe.weargallery.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.GITHUB
import cc.chenhe.weargallery.common.util.TELEGRAM
import cc.chenhe.weargallery.common.util.getVersionName
import cc.chenhe.weargallery.common.util.openMarket
import cc.chenhe.weargallery.databinding.FrAboutBinding
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.uilts.addQrCode
import me.chenhe.wearvision.dialog.AlertDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class AboutFr : SwipeDismissFr() {

    private lateinit var binding: FrAboutBinding
    private val model: AboutViewModel by viewModel()

    private var newVersion: Int = 0

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ViewBinding {
        return FrAboutBinding.inflate(inflater, container, false).also {
            binding = it
            it.lifecycleOwner = viewLifecycleOwner
            it.model = model
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvVersion.text = getVersionName(requireContext())

        binding.licenses.setOnClickListener {
            findNavController().navigate(AboutFrDirections.actionAboutFrToLicensesFr())
        }

        binding.thanks.setOnClickListener {
            AlertDialog(requireContext()).apply {
                setTitle(R.string.about_thank)
                setMessage(R.string.about_thank_content)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
            }.show()
        }

        binding.github.setOnClickListener {
            AlertDialog(requireContext()).apply {
                title = "GitHub"
                setMessage(R.string.dialog_scan_to_visit)
                addQrCode(GITHUB)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
            }.show()
        }

        binding.telegram.setOnClickListener {
            AlertDialog(requireContext()).apply {
                title = "Telegram"
                setMessage(R.string.dialog_scan_to_visit)
                addQrCode(TELEGRAM)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
            }.show()
        }

        model.newVersion.observe(viewLifecycleOwner) {
            this.newVersion = it
        }

        binding.update.setOnClickListener {
            when (newVersion) {
                1 -> openMarket(requireContext()) { showUrl(model.url) }
                2, 3 -> model.checkUpdate()
            }
        }
        model.checkUpdate()
    }

    private fun showUrl(url: String) {
        context?.let {
            AlertDialog(it).apply {
                title = ""
                setMessage(R.string.dialog_scan_to_visit)
                addQrCode(url)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
            }.show()
        }
    }
}