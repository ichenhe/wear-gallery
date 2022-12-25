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

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cc.chenhe.weargallery.BuildConfig
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.*
import cc.chenhe.weargallery.service.ForegroundService
import cc.chenhe.weargallery.ui.common.CollapseHeaderLayout
import cc.chenhe.weargallery.utils.fetchForegroundService
import cc.chenhe.weargallery.utils.requireCompatAty
import cc.chenhe.weargallery.utils.setupToolbar

class PreferenceFr : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.toolbar).let {
            setupToolbar(it)
            it.setNavigationOnClickListener { findNavController().navigateUp() }
        }
        requireCompatAty().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        view.findViewById<CollapseHeaderLayout>(R.id.header).setTitle(R.string.pref_title)

        fetchForegroundService(
            requireContext(),
            init = false
        ).observe(viewLifecycleOwner) { foregroundService ->
            if (foregroundService)
                ForegroundService.start(requireContext())
            else
                ForegroundService.stop(requireContext())

        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // version
        findPreference<Preference>("check_update")?.summary = getString(
            R.string.pref_version,
            getVersionName(requireContext()), getVersionCode(requireContext())
        )
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "check_update" -> {
                if (BuildConfig.IS_GP) {
                    openMarket(requireContext()) {
                        openWithBrowser(requireContext(), GITHUB)
                    }
                } else {
                    openWithBrowser(requireContext(), GITHUB)
                }
            }
            "about" -> {
                findNavController().navigate(PreferenceFrDirections.actionPreferenceFrToAboutFr())
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }
}