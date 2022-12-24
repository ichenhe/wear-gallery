package cc.chenhe.weargallery.common.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check if user always reject the permission request.
 *
 * Only accurate if called in the authorization result callback.
 */
fun Activity.isAlwaysDenied(permission: String): Boolean {
    return !checkPermission(permission) && !shouldShowRequestPermissionRationale(permission)
}