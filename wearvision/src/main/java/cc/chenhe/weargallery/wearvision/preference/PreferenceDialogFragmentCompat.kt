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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.PreferenceFragmentCompat
import cc.chenhe.weargallery.wearvision.dialog.AlertDialog


/**
 * Copied from [androidx.preference.PreferenceDialogFragmentCompat] but uses the WearVision's [AlertDialog] instead of
 * [androidx.appcompat.app.AlertDialog.Builder]. Also, this class use WearVision's [DialogPreference] instead of
 * [androidx.preference.DialogPreference] to provides button icons.
 *
 * The following attributes are ignored:
 *
 * - [DialogPreference.mPositiveButtonText]
 * - [DialogPreference.mNegativeButtonText]
 */
abstract class PreferenceDialogFragmentCompat : DialogFragment(), DialogInterface.OnClickListener {

    companion object {
        const val ARG_KEY = "key"

        private const val SAVE_STATE_TITLE = "PreferenceDialogFragment.title"
        private const val SAVE_STATE_POSITIVE_ICON = "PreferenceDialogFragment.positiveIcon"
        private const val SAVE_STATE_NEGATIVE_ICON = "PreferenceDialogFragment.negativeIcon"
        private const val SAVE_STATE_MESSAGE = "PreferenceDialogFragment.message"
        private const val SAVE_STATE_LAYOUT = "PreferenceDialogFragment.layout"
        private const val SAVE_STATE_ICON = "PreferenceDialogFragment.icon"
    }

    private val mPreference: DialogPreference by lazy { findPreference() }

    var dialogTitle: CharSequence? = null
        private set
    var positiveButtonIcon: BitmapDrawable? = null
        private set
    var negativeButtonIcon: BitmapDrawable? = null
        private set
    var dialogMessage: CharSequence? = null
        private set

    @LayoutRes
    private var mDialogLayoutRes = 0

    private var mDialogIcon: BitmapDrawable? = null

    /** Which button was clicked.  */
    private var mWhichButtonClicked = 0

    private fun findPreference(): DialogPreference {
        val rawFragment = targetFragment
        check(rawFragment is TargetFragment) { "Target fragment must implement TargetFragment interface." }
        val fragment = rawFragment as TargetFragment
        val key = requireNotNull(arguments?.getString(ARG_KEY)) { "Must has argument named $ARG_KEY." }
        return requireNotNull(fragment.findPreference(key)) { "Can not find preference with key=$key." }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            dialogTitle = mPreference.dialogTitle
            positiveButtonIcon = toBitmapDrawable(mPreference.positiveButtonIcon)
            negativeButtonIcon = toBitmapDrawable(mPreference.negativeButtonIcon)
            dialogMessage = mPreference.dialogMessage
            mDialogLayoutRes = mPreference.dialogLayoutResource
            mDialogIcon = toBitmapDrawable(mPreference.dialogIcon)
        } else {
            dialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE)
            dialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE)
            mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0)
            savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_POSITIVE_ICON)?.let {
                positiveButtonIcon = BitmapDrawable(resources, it)
            }
            savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_NEGATIVE_ICON)?.let {
                negativeButtonIcon = BitmapDrawable(resources, it)
            }
            savedInstanceState.getParcelable<Bitmap>(SAVE_STATE_ICON)?.let {
                mDialogIcon = BitmapDrawable(resources, it)
            }
        }
    }

    private fun toBitmapDrawable(drawable: Drawable?): BitmapDrawable? {
        return if (drawable == null || drawable is BitmapDrawable) {
            drawable as? BitmapDrawable
        } else {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            BitmapDrawable(resources, bitmap)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TITLE, dialogTitle)
        outState.putCharSequence(SAVE_STATE_MESSAGE, dialogMessage)
        outState.putInt(SAVE_STATE_LAYOUT, mDialogLayoutRes)
        positiveButtonIcon?.let { outState.putParcelable(SAVE_STATE_POSITIVE_ICON, it.bitmap) }
        negativeButtonIcon?.let { outState.putParcelable(SAVE_STATE_NEGATIVE_ICON, it.bitmap) }
        mDialogIcon?.let { outState.putParcelable(SAVE_STATE_ICON, it.bitmap) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentView = onCreateDialogView(context)
        if (contentView != null) {
            onBindDialogView(contentView)
        }

        val dialog = AlertDialog(requireContext()).also { d ->
            d.icon = mDialogIcon
            d.setTitle(dialogTitle)
            if (contentView != null) {
                d.setView(contentView)
            } else {
                d.message = dialogMessage?.toString()
            }
            d.setPositiveButtonIcon(positiveButtonIcon, this@PreferenceDialogFragmentCompat)
            d.setNegativeButtonIcon(negativeButtonIcon, this@PreferenceDialogFragmentCompat)
        }
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE
        onPrepareDialog(dialog)
        if (needInputMethod()) {
            requestInputMethod(dialog)
        }
        return dialog
    }

    /**
     * Get the preference that requested this dialog. Available after [.onCreate] has
     * been called on the [PreferenceFragmentCompat] which launched this dialog.
     *
     * @return The [DialogPreference] associated with this dialog
     */
    fun getPreference(): DialogPreference {
        return mPreference
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked. Use this to set custom properties on the
     * dialog.
     *
     * Do not [AlertDialog.show].
     */
    protected open fun onPrepareDialog(dialog: AlertDialog) {}

    /**
     * Returns whether the preference needs to display a soft input method when the dialog is
     * displayed. Default is false. Subclasses should override this method if they need the soft
     * input method brought up automatically.
     *
     *
     * Note: If your application targets P or above, ensure your subclass manually requests
     * focus (ideally in [.onBindDialogView]) for the input field in order to
     * correctly attach the input method to the field.
     */
    protected open fun needInputMethod(): Boolean {
        return false
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private fun requestInputMethod(dialog: Dialog) {
        val window = dialog.window
        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    /**
     * Creates the content view for the dialog (if a custom content view is required). By default, it inflates the
     * dialog layout resource if it is set.
     *
     * @return The content view for the dialog
     * @see DialogPreference.setLayoutResource
     */
    protected open fun onCreateDialogView(context: Context?): View? {
        val resId = mDialogLayoutRes
        if (resId == 0) {
            return null
        }
        return LayoutInflater.from(context).inflate(resId, null)
    }

    /**
     * Binds views in the content view of the dialog to data.
     *
     * Make sure to call through to the superclass implementation.
     *
     * @param view The content view of the dialog, if it is custom.
     */
    @CallSuper
    protected open fun onBindDialogView(view: View) {
        view.findViewById<View>(android.R.id.message)?.let { dialogMessageView ->
            val message = dialogMessage
            var newVisibility = View.GONE
            if (!message.isNullOrEmpty()) {
                if (dialogMessageView is TextView) {
                    dialogMessageView.text = message
                }
                newVisibility = View.VISIBLE
            }
            if (dialogMessageView.visibility != newVisibility) {
                dialogMessageView.visibility = newVisibility
            }
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        mWhichButtonClicked = which
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE)
    }

    /**
     * Called in [onDismiss]. Subclass should always consider using this method.
     */
    abstract fun onDialogClosed(positiveResult: Boolean)
}