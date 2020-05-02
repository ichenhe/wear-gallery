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

package cc.chenhe.weargallery.watchface.style

import android.app.Application
import androidx.annotation.ColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cc.chenhe.weargallery.uilts.getWatchFaceTimeTextColor
import cc.chenhe.weargallery.uilts.setWatchFaceTimeTextColor

class TimeTextColorViewModel(application: Application) : AndroidViewModel(application) {

    private val _color = MutableLiveData(getWatchFaceTimeTextColor(application))
    val color: LiveData<Int?> = _color

    fun setColor(@ColorInt newColor: Int) {
        _color.postValue(newColor)
    }

    fun saveColor() {
        _color.value?.let {
            setWatchFaceTimeTextColor(getApplication(), it)
        }
    }

}