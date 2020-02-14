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
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.uilts.PREFERENCE_KEEP_SCREEN_ON
import cc.chenhe.weargallery.wearvision.preference.PreferenceSwipeDismissFragmentCompat

class DisplayPreferenceFr : PreferenceSwipeDismissFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_display, rootKey)

        requireNotNull(findPreference<SwitchPreferenceCompat>(PREFERENCE_KEEP_SCREEN_ON))
                .summaryProvider = Preference.SummaryProvider<SwitchPreferenceCompat> { preference ->
            if (preference.isChecked) {
                getString(R.string.preference_always_on_summary)
            } else {
                null
            }
        }

    }

    override fun onDismissed() {
        findNavController().navigateUp()
    }
}