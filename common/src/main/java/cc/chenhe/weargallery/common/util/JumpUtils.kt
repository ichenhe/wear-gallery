package cc.chenhe.weargallery.common.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat

fun openMarket(context: Context, fallback: (() -> Unit)?) {
    try {
        val uri = Uri.parse("market://details?id=" + context.packageName)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        fallback?.invoke()
    }
}

fun openWithBrowser(context: Context, url: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(url)
    }
    ContextCompat.startActivity(context, Intent.createChooser(intent, null), null)
}