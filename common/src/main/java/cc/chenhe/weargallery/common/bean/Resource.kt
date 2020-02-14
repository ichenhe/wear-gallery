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

package cc.chenhe.weargallery.common.bean

/**
 * A generic class that contains data and status about loading this data.
 */
sealed class Resource<T> {
    abstract val data: T?

    val state = this::class.simpleName
}

data class Success<T>(override val data: T?) : Resource<T>()

data class Loading<T>(override val data: T? = null) : Resource<T>()

data class Error<T>(
        val code: Int,
        val message: String? = null,
        override val data: T? = null
) : Resource<T>() {
    companion object {
        const val CODE_EXCEPTION = Int.MIN_VALUE + 1
    }
}
