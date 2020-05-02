package cc.chenhe.weargallery.wearvision.preference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.wearvision.R
import kotlin.math.max

private const val DIALOG_FRAGMENT_TAG = "wearvision.preference.PreferenceFragment.DIALOG"

/**
 * A subclass of [PreferenceFragmentCompat] in AndroidX. You should always consider using this implement.
 *
 * This class deals with WearVision's dialog preference and adds padding bottom to recycler view to avoid screen
 * cropping.
 */
abstract class PreferenceFragmentCompat : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(inflater: LayoutInflater?, parent: ViewGroup?, savedInstanceState: Bundle?)
            : RecyclerView {
        val padding = resources.getDimensionPixelOffset(R.dimen.wv_page_padding_bottom)
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).also { rv ->
            rv.setPadding(rv.paddingLeft, rv.paddingTop, rv.paddingTop, max(rv.paddingBottom, padding))
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        val handled = when (preference) {
            is ListPreference -> {
                ListPreferenceDialogFragmentCompat.newInstance(preference.key).apply {
                    setTargetFragment(this@PreferenceFragmentCompat, 0)
                }.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
                true
            }
            else -> false
        }
        if (!handled) {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}