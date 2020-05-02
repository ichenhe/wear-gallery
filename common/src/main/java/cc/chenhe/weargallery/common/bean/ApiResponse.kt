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

sealed class ApiResponse<T> {
    abstract val code: Int

    companion object {
        const val CODE_DEFAULT = Int.MIN_VALUE

        // ----------------------------------------
        // WearMsger
        // ----------------------------------------
        const val CODE_SEND_FAIL = 1
        const val CODE_TIMEOUT = 2
    }
}

/**
 * Separate class for view_empty responses so that we can make [ApiSuccessResponse]'s data non-null.
 */
data class ApiEmptyResponse<T>(
        override val code: Int = CODE_DEFAULT
) : ApiResponse<T>()

data class ApiSuccessResponse<T>(
        val data: T,
        override val code: Int = CODE_DEFAULT
) : ApiResponse<T>()

data class ApiErrorResponse<T>(
        override val code: Int = CODE_DEFAULT,
        val errorMessage: String? = null
) : ApiResponse<T>()