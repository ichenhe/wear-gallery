package cc.chenhe.weargallery.wearvision.util

import android.content.Context
import android.util.Log
import android.view.View

internal fun isScreenRound(context: Context) = WearVision.isScreenRound(context)

internal fun View.visibleGone(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

private const val TAG = "WearVision"

internal fun loge(tag: String, msg: String) {
    Log.e(TAG, "[$tag] $msg")
}

internal fun logw(tag: String, msg: String) {
    Log.w(TAG, "[$tag] $msg")
}