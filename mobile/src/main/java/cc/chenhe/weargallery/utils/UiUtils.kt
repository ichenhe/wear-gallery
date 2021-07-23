package cc.chenhe.weargallery.utils

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

/**
 * Reset the color of text in status bar to match the current theme color.
 */
internal fun Activity.resetStatusBarTextColor(rootView: View) {
    val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    WindowInsetsControllerCompat(window, rootView).isAppearanceLightStatusBars =
        mode != Configuration.UI_MODE_NIGHT_YES
}

internal fun Fragment.requireCompatAty() = requireActivity() as AppCompatActivity

internal fun Fragment.setupToolbar(toolbar: Toolbar) {
    requireCompatAty().apply {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
    }
}