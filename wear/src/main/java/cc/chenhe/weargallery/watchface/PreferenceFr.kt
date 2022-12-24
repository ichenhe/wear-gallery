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

package cc.chenhe.weargallery.watchface

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.uilts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.chenhe.wearvision.dialog.AlertDialog
import me.chenhe.wearvision.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

private const val TAG = "WfPreference"

private const val URI_APP_AW = "market://details?id=com.cotwf.watchfaceplatform.wearos"
private const val URI_APP_TW = "market://details?id=com.cotwf.watchfaceplatform.ticwear"

class PreferenceFr : PreferenceFragmentCompat() {

    private lateinit var analogStyle: String
    private lateinit var digitalStyle: String

    private val model by viewModel<PreferenceViewModel>()

    private lateinit var positionPreference: Preference
    private lateinit var textPreference: Preference
    private lateinit var textColorPreference: Preference
    private lateinit var dimPreference: SwitchPreferenceCompat
    private var dimFlag = false

    private val selectBgImageLauncher =
        registerForActivityResult(object : ActivityResultContract<Unit, Uri?>() {
            override fun createIntent(context: Context, input: Unit): Intent {
                return Intent(Intent.ACTION_PICK).apply {
                    setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                return if (resultCode == Activity.RESULT_OK) intent?.data else null
            }
        }) { uri ->
            if (uri != null) {
                ProcessLifecycleOwner.get().lifecycleScope.launch {
                    copyBackground(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analogStyle = getString(R.string.wf_preference_type_entry_value_analog)
        digitalStyle = getString(R.string.wf_preference_type_entry_value_digital)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_wf_main, rootKey)

        dimPreference = requireNotNull(findPreference("preference_watchface_dim"))
        positionPreference = requireNotNull(findPreference("preference_watchface_text_position"))
        textPreference = requireNotNull(findPreference("preference_watchface_text"))
        textColorPreference = requireNotNull(findPreference("preference_watchface_text_color"))

        dimPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == false || dimFlag)
                return@setOnPreferenceChangeListener true
            AlertDialog(requireContext()).apply {
                setTitle(R.string.tip)
                setMessage(R.string.wf_dim_dialog)
                setPositiveButtonIcon(R.drawable.ic_dialog_confirm) { dialog, _ ->
                    dimFlag = true
                    dimPreference.isChecked = true
                    dialog.dismiss()
                }
            }.show()
            false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.styleType.observe(viewLifecycleOwner) { style ->
            when (style) {
                digitalStyle -> setDigitalPreferencesVisible(true)
                analogStyle -> setDigitalPreferencesVisible(false)
                else -> setDigitalPreferencesVisible(false)
            }
        }
    }

    private fun setDigitalPreferencesVisible(visible: Boolean) {
        positionPreference.isVisible = visible
        textPreference.isVisible = visible
        textColorPreference.isVisible = visible
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "preference_watchface_image" -> {
                selectBgImageLauncher.launch(Unit)
            }
            "preference_watchface_text_position" -> {
                findNavController().navigate(PreferenceFrDirections.actionPreferenceFrToTimeTextStyleFr())
            }
            "preference_watchface_text" -> {
                findNavController().navigate(PreferenceFrDirections.actionPreferenceFrToTimeTextFr())
            }
            "preference_watchface_text_color" -> {
                findNavController().navigate(PreferenceFrDirections.actionPreferenceFrToTimeTextColorFr())
            }
            "preference_cot" -> {
                val uri = Uri.parse(if (isTicwear()) URI_APP_TW else URI_APP_AW)
                startIntent(Intent(Intent.ACTION_VIEW, uri)) {
                    toast(R.string.wf_preference_intent_err)
                }
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatcher
    private suspend fun copyBackground(uri: Uri) = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("Copying background image, uri=%s", uri)
        val targetFile = File(getWatchFaceResFolder(requireContext()), WATCH_FACE_BACKGROUND)
        if (targetFile.isFile) {
            targetFile.delete()
        }
        requireContext().contentResolver.openInputStream(uri)?.use { ins ->
            FileOutputStream(targetFile).use { fos ->
                ins.copyTo(fos)
            }
        }
        LocalBroadcastManager.getInstance(get())
            .sendBroadcast(Intent(ACTION_WATCH_FACE_BACKGROUND_CHANGED))
        Timber.tag(TAG).d("Copy background image complete.")
    }

    private fun testIntent(intent: Intent): Boolean {
        return context?.let { intent.resolveActivity(it.packageManager) != null } ?: false
    }

    private inline fun startIntent(intent: Intent, err: () -> Unit) {
        try {
            if (testIntent(intent)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                err()
            }
        } catch (e: Exception) {
            err()
        }
    }

}
