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

package cc.chenhe.weargallery.common.comm

import cc.chenhe.weargallery.common.bean.ApiEmptyResponse
import cc.chenhe.weargallery.common.bean.ApiErrorResponse
import cc.chenhe.weargallery.common.bean.ApiResponse
import cc.chenhe.weargallery.common.bean.ApiSuccessResponse
import me.chenhe.lib.wearmsger.bean.BothWayCallback
import me.chenhe.lib.wearmsger.bean.MessageCallback

/**
 * @param process A function that translate the raw data to the type we need.
 */
inline fun <T> MessageCallback.toApiResp(process: (raw: MessageCallback) -> T): ApiResponse<T> {
    return if (this.isSuccess()) {
        if (this.data == null) {
            ApiEmptyResponse()
        } else {
            ApiSuccessResponse(process(this))
        }
    } else {
        when (this.result) {
            BothWayCallback.Result.OK -> throw IllegalStateException() // should never happen
            BothWayCallback.Result.REQUEST_FAIL -> ApiResponse.CODE_SEND_FAIL
            BothWayCallback.Result.TIMEOUT -> ApiResponse.CODE_TIMEOUT
        }.let { code ->
            // return explicitly to make compiler happy :)
            // If not it says 'Not enough information to infer parameter T'.
            return ApiErrorResponse(code)
        }
    }
}