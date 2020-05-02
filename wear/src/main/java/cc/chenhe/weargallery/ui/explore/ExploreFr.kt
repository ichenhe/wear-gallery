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

import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.watchface.PreferenceAty
import cc.chenhe.weargallery.wearvision.preference.PreferenceFragmentCompat

class ExploreFr : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_main, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "preference_display" -> findNavController().navigate(R.id.displayPreferenceFr)
            "preference_transfer" -> findNavController().navigate(R.id.transferPreferenceFr)
            "preference_watchface" -> startActivity(Intent(requireContext(), PreferenceAty::class.java))
            "preference_about" -> findNavController().navigate(R.id.aboutFr)
            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }
}