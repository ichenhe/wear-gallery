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
import cc.chenhe.weargallery.R
import me.chenhe.wearvision.dialog.AlertDialog
import me.chenhe.wearvision.preference.PreferenceSwipeDismissFragmentCompat

class TransferPreferenceFr : PreferenceSwipeDismissFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_transfer, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "preference_transfer_lan" ->
                findNavController().navigate(TransferPreferenceFrDirections.actionTransferPreferenceFrToWebServerFr())
            "preference_transfer_disclaimer" -> AlertDialog(requireContext()).apply {
                setTitle(R.string.preference_transfer_disclaimer)
                setMessage(R.string.preference_transfer_disclaimer_content)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm, null)
            }.show()

            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    override fun onDismissed() {
        findNavController().navigateUp()
    }
}