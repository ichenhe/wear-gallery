package cc.chenhe.weargallery.utils

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable

inline fun <reified T : Parcelable> Intent.getParcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Intent.getParcelableArrayList(key: String): ArrayList<T>? =
    when {
        SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
    }

inline fun <reified T : Parcelable> Intent.getParcelableArray(key: String): List<T>? =
    when {
        SDK_INT >= 33 -> getParcelableArrayExtra(key, T::class.java)?.toList()
        else -> @Suppress("DEPRECATION") getParcelableArrayExtra(key)?.filterIsInstance<T>()
    }