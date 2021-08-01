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
import androidx.lifecycle.AndroidViewModel
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.uilts.*
import timber.log.Timber

private const val TAG = "TimeTextViewModel"

class TimeTextFormatViewModel(application: Application) : AndroidViewModel(application) {

    val tagsMap = mapOf(
        R.id.line to TIME_TAG_LINE,
        R.id.hour to TIME_TAG_HOUR,
        R.id.min to TIME_TAG_MIN,
        R.id.colon to TIME_TAG_COLON,
        R.id.year to TIME_TAG_YEAR,
        R.id.month to TIME_TAG_MONTH,
        R.id.day to TIME_TAG_DAY,
        R.id.slash to TIME_TAG_SLASH
    )

    val tagsNameIds = mapOf(
        TIME_TAG_LINE to R.string.wf_text_line,
        TIME_TAG_HOUR to R.string.wf_text_hour,
        TIME_TAG_MIN to R.string.wf_text_min,
        TIME_TAG_COLON to R.string.wf_text_colon,
        TIME_TAG_YEAR to R.string.wf_text_year,
        TIME_TAG_MONTH to R.string.wf_text_month,
        TIME_TAG_DAY to R.string.wf_text_day,
        TIME_TAG_SLASH to R.string.wf_text_slash
    )

    /**
     * List of tags in current time format. Each item must conform to any of the following formats:
     *
     * - `{*}`: Dynamic tag. when displayed it will be replaced with real value. All dynamic tags are listed as
     * constants `TIME_TAG_*`.
     */
    private var _tags = mutableListOf<String>()
    val tags: List<String> get() = _tags

    fun loadPreference() {
        _tags = getWatchFaceTimeFormat(getApplication()).toMutableList()
    }

    fun savePreference(): Boolean {
        return if (_tags.isNotEmpty()) {
            setWatchFaceTimeFormat(getApplication(), _tags)
            true
        } else {
            Timber.tag(TAG).d("tags list is empty, skip save.")
            false
        }
    }

    fun resetPreference() {
        resetWatchFaceTimeFormat(getApplication())
    }

    fun addTag(tag: String) {
        _tags.add(tag)
    }

    fun removeTag(index: Int) {
        _tags.removeAt(index)
    }
}