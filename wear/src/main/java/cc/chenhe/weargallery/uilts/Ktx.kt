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

package cc.chenhe.weargallery.uilts

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import coil.ImageLoader
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest


fun Fragment.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), resId, duration).show()
}

val AndroidViewModel.context: Context get() = getApplication<Application>()

fun <T> MutableLiveData<T>.setIfNot(newValue: T, post: Boolean = false) {
    if (value != newValue) {
        if (post) {
            postValue(newValue)
        } else {
            value = newValue
        }
    }
}

/**
 * Load a image with coil but not allow using hardware accelerate.
 *
 * In API 30 Wear OS, activity swipe-to-dismiss transition will crash if HW-Acc is enabled.
 */
inline fun ImageView.loadWithoutHW(
    uri: Uri?,
    allowRgb565: Boolean = true,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(uri)
        .allowHardware(false)
        .allowRgb565(allowRgb565)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}