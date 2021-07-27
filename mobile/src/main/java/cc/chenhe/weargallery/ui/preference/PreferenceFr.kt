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

package cc.chenhe.weargallery.ui.preference

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.common.util.getVersionName
import cc.chenhe.weargallery.ui.common.CollapseHeaderLayout
import cc.chenhe.weargallery.utils.UPDATE_URL
import cc.chenhe.weargallery.utils.requireCompatAty
import cc.chenhe.weargallery.utils.setupToolbar

private const val ALIPAY =
    "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/tsx12672qtk37hufsxfkub7"

class PreferenceFr : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar).let {
            setupToolbar(it)
            it.setNavigationOnClickListener { findNavController().navigateUp() }
        }
        requireCompatAty().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        view.findViewById<CollapseHeaderLayout>(R.id.header).setTitle(R.string.pref_title)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // version
        findPreference<Preference>("check_update")?.summary = getString(
            R.string.pref_version,
            getVersionName(requireContext()), getVersionCode(requireContext())
        )
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "check_update" -> {
                openMarket()
            }
            "about" -> {
                findNavController().navigate(PreferenceFrDirections.actionPreferenceFrToAboutFr())
            }
            "donate" -> {
                startAliPay()
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    private fun openMarket() {
        try {
            val uri = Uri.parse("market://details?id=" + requireContext().packageName)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireContext().startActivity(intent)
        } catch (e: Exception) {
            openWithBrowser(UPDATE_URL)
        }
    }

    private fun openWithBrowser(@Suppress("SameParameterValue") url: String) {
        try {
            requireContext().startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                    requireContext().getString(R.string.links_chooser_browser)
                )
            )
        } catch (ignored: ActivityNotFoundException) {
            Toast.makeText(context, R.string.update_no_web_view, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAliPay() {
        try {
            val uri = Uri.parse(ALIPAY)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            Toast.makeText(requireContext(), R.string.pref_donate_alipay_error, Toast.LENGTH_SHORT)
                .show()
        }
    }
}