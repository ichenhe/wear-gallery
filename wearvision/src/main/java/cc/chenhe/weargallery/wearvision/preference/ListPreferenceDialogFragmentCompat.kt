package cc.chenhe.weargallery.wearvision.preference

import android.content.DialogInterface
import android.os.Bundle
import cc.chenhe.weargallery.wearvision.dialog.AlertDialog

class ListPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    companion object {
        private const val SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "ListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): ListPreferenceDialogFragmentCompat {
            val fragment = ListPreferenceDialogFragmentCompat()
            fragment.arguments = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
            return fragment
        }
    }

    var mClickedDialogEntryIndex = 0
    private lateinit var mEntries: Array<CharSequence>
    private lateinit var mEntryValues: Array<CharSequence>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference: ListPreference = getListPreference()
            check(!(preference.entries == null || preference.entryValues == null)) {
                "ListPreference requires an entries array and an entryValues array."
            }
            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntries = preference.entries!!
            mEntryValues = preference.entryValues!!
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            mEntries = requireNotNull(savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES))
            mEntryValues = requireNotNull(savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    private fun getListPreference(): ListPreference {
        return getPreference() as ListPreference
    }

    override fun onPrepareDialog(dialog: AlertDialog) {
        super.onPrepareDialog(dialog)
        dialog.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, DialogInterface.OnClickListener { d, i ->
            mClickedDialogEntryIndex = i

            // Clicking on an item simulates the positive button click, and dismisses the dialog.
            onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            d.dismiss()
        })
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = mEntryValues[mClickedDialogEntryIndex].toString()
            val preference = getListPreference()
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

}