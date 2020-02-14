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

import androidx.annotation.IntRange
import java.util.*

interface ITimeHolder {

    fun calendar(): Calendar

    fun is24HourFormat(): Boolean

    fun getLeadingZero(num: Int, @IntRange(from = 0, to = 3) digits: Int = 2): String {
        if (digits == 2) {
            return if (num < 10) "0$num" else num.toString()
        } else if (digits == 3) {
            if (num < 10) {
                return "00$num"
            } else if (num < 100) {
                return "0$num"
            }
        }
        return num.toString()
    }

    fun getHourAuto(is24Hour: Boolean): Int {
        if (is24Hour) {
            return calendar().get(Calendar.HOUR_OF_DAY)
        } else {
            calendar().get(Calendar.HOUR).let {
                return if (it == 0) 12 else it
            }
        }
    }
}