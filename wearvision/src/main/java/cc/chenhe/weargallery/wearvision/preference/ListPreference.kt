/*
 * Copyright (c) 2020 Chenhe
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.chenhe.weargallery.wearvision.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.ArrayRes
import cc.chenhe.weargallery.wearvision.R
import cc.chenhe.weargallery.wearvision.util.TypedArrayUtils
import cc.chenhe.weargallery.wearvision.util.logw

const val TAG = "ListPreference"

/**
 * Copied from [androidx.preference.ListPreference] but extends our own [DialogPreference].
 */
@Suppress("unused")
@SuppressLint("PrivateResource")
class ListPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
    : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Sets the human-readable entries to be shown in the list. This will be shown in subsequent dialogs.
     *
     * Each entry must have a corresponding index in [entryValues].
     *
     * @see [entryValues]
     */
    var entries: Array<CharSequence>?

    /**
     * The array to find the value to save for a preference when an entry from entries is selected. If a user clicks on
     * the second item in entries, the second item in this array will be saved to the preference.
     */
    var entryValues: Array<CharSequence>?

    /**
     * The value of the key. This should be one of the entries in [entryValues].
     */
    var value: String? = null
        set(value) {
            // Always persist/notify the first time.
            val changed = field != value
            if (changed || !mValueSet) {
                field = value
                mValueSet = true
                persistString(value)
                if (changed) {
                    notifyChanged()
                }
            }
        }
    private var mSummary: String? = null
    private var mValueSet = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, TypedArrayUtils.getAttr(context,
            R.attr.dialogPreferenceStyle, android.R.attr.dialogPreferenceStyle))

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes)

        entries = TypedArrayUtils.getTextArray(a, R.styleable.ListPreference_entries,
                R.styleable.ListPreference_android_entries)
        entryValues = TypedArrayUtils.getTextArray(a, R.styleable.ListPreference_entryValues,
                R.styleable.ListPreference_android_entryValues)
        if (TypedArrayUtils.getBoolean(a, R.styleable.ListPreference_useSimpleSummaryProvider,
                        R.styleable.ListPreference_useSimpleSummaryProvider, false)) {
            summaryProvider = SimpleSummaryProvider.instance
        }

        a.recycle()
    }

    /**
     * @param entriesResId The entries array as a resource.
     * @see [entries]
     */
    fun setEntries(@ArrayRes entriesResId: Int) {
        entries = context.resources.getTextArray(entriesResId)
    }

    /**
     * @param entryValuesResId The entry values array as a resource.
     * @see [entryValues]
     */
    fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        entryValues = context.resources.getTextArray(entryValuesResId)
    }

    override fun setSummary(summary: CharSequence?) {
        super.setSummary(summary)
        mSummary = summary?.toString()
    }

    override fun getSummary(): CharSequence? {
        summaryProvider?.let { return it.provideSummary(this) }
        val entry: CharSequence? = getEntry()
        val summary = super.getSummary()

        return mSummary?.let {
            val formattedString = String.format(it, entry ?: "")
            if (formattedString == summary) {
                summary
            } else {
                logw(TAG, "Setting a summary with a String formatting marker is no longer supported."
                        + " You should use a SummaryProvider instead.")
                formattedString
            }
        } ?: summary
    }

    /**
     * Returns the entry corresponding to the current value.
     *
     * @return The entry corresponding to the current value, or `null`.
     */
    fun getEntry(): CharSequence? {
        return entries?.getOrNull(getValueIndex())
    }

    /**
     * Returns the index of the given value (in the entry values array).
     *
     * @param value The value whose index should be returned.
     * @return The index of the value, or -1 if not found.
     */
    fun findIndexOfValue(value: String?): Int {
        if (value == null) {
            return -1
        }
        return entryValues?.indexOf(value) ?: -1
    }

    /**
     * Sets the value to the given index from the entry values.
     *
     * @param index The index of the value to set.
     */
    fun setValueIndex(index: Int) {
        entryValues?.let {
            value = it[index].toString()
        }
    }

    private fun getValueIndex(): Int {
        return findIndexOfValue(value)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedString(defaultValue as String?)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }
        return SavedState(superState).also {
            it.value = value
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state !is SavedState) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        value = state.value
    }

    private class SavedState : BaseSavedState {
        var value: String? = null

        constructor(source: Parcel) : super(source) {
            value = source.readString()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    /**
     * A simple [androidx.preference.Preference.SummaryProvider] implementation for a [ListPreference]. If no value has
     * been set, the summary displayed will be 'Not set', otherwise the summary displayed will be the entry set for
     * this preference.
     */
    class SimpleSummaryProvider private constructor() : SummaryProvider<ListPreference> {
        @SuppressLint("PrivateResource")
        override fun provideSummary(preference: ListPreference): CharSequence {
            val entry = preference.getEntry()
            return if (entry.isNullOrEmpty()) {
                preference.context.getString(androidx.preference.R.string.not_set)
            } else {
                entry
            }
        }

        companion object {
            private var sSimpleSummaryProvider: SimpleSummaryProvider? = null

            /**
             * Retrieve a singleton instance of this simple
             * [androidx.preference.Preference.SummaryProvider] implementation.
             *
             * @return a singleton instance of this simple
             * [androidx.preference.Preference.SummaryProvider] implementation.
             */
            val instance: SimpleSummaryProvider
                get() {
                    return sSimpleSummaryProvider ?: SimpleSummaryProvider().also {
                        sSimpleSummaryProvider = it
                    }
                }
        }
    }

}