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

package cc.chenhe.weargallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.ui.BaseViewHolder
import cc.chenhe.weargallery.databinding.FrLicensesBinding
import cc.chenhe.weargallery.databinding.RvItemLicenseBinding
import cc.chenhe.weargallery.ui.common.SwipeDismissFr
import cc.chenhe.weargallery.uilts.addQrCode
import me.chenhe.wearvision.dialog.AlertDialog

private const val MIT = "MIT License"
private const val APACHE_2 = "Apache Software License 2.0"

class LicensesFr : SwipeDismissFr() {

    private lateinit var binding: FrLicensesBinding

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )
            : ViewBinding {
        return FrLicensesBinding.inflate(inflater, container, false).also {
            binding = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val licenses = listOf(
            License("Android Open Source Project", "AOSP", APACHE_2, "https://source.android.com/"),
            License("Coil", "coil-kt", APACHE_2, "https://github.com/coil-kt/coil"),
            License(
                "DiskLruCache",
                "JakeWharton",
                APACHE_2,
                "https://github.com/JakeWharton/DiskLruCache"
            ),
            License("Koin", "Koin", APACHE_2, "https://github.com/InsertKoinIO/koin"),
            License("Kotlin", "kotlinlang", APACHE_2, "https://github.com/Kotlin/"),
            License(
                "material-intro",
                "heinrichreimer",
                MIT,
                "https://github.com/heinrichreimer/material-intro"
            ),
            License("Mars-xlog", "Tencent", "Custom", "https://github.com/Tencent/mars"),
            License("Moshi", "square", APACHE_2, "https://github.com/square/moshi"),
            License(
                "NanoHTTPD",
                "NanoHTTPD",
                "BSD 3-Clause",
                "https://github.com/NanoHttpd/nanohttpd"
            ),
            License("Sketch", "panpf", APACHE_2, "https://github.com/panpf/sketch"),
            License(
                "ViewPagerIndicator",
                "zhpanvip",
                APACHE_2,
                "https://github.com/zhpanvip/viewpagerindicator"
            ),
            License(
                "WatchFaceHelper",
                "Chenhe",
                APACHE_2,
                "https://github.com/ichenhe/WatchFaceHelper"
            ),
            License("Wear-Msger", "Chenhe", MIT, "https://github.com/ichenhe/Wear-Msger"),
            License("Wear-Vision", "Chenhe", APACHE_2, "https://github.com/ichenhe/Wear-Vision"),
        )

        binding.list.adapter = Adapter(licenses)
    }

    private inner class Adapter(private val list: List<License>) :
        RecyclerView.Adapter<BaseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return BaseViewHolder(
                RvItemLicenseBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ).root
            )
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            val l1 = list[position].name + " - " + list[position].author
            val l2 = list[position].type + "\n" + list[position].url
            holder.setText(android.R.id.text1, l1)
            holder.setText(android.R.id.text2, l2)

            holder.itemView.setOnClickListener {
                val data = list[holder.bindingAdapterPosition]
                AlertDialog(requireContext()).apply {
                    title = data.name
                    setMessage(R.string.dialog_scan_to_visit)
                    addQrCode(data.url)
                    setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
                }.show()
            }
        }

    }

    private data class License(
        val name: String,
        val author: String,
        val type: String,
        val url: String
    )
}