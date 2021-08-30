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

/**
 * This file contains the communication contracts between the watch and the mobile phone.
 *
 * When modify PATH_* 's value, the corresponding value in the Manifests must be consistent.
 */


/**
 * The capabilities that the wear client has.
 *
 * This value must be the same as that in wear's wear.xml.
 */
const val CAP_WEAR = "wg_watch"

const val PATH_CHANNEL_BATCH_SEND = "/batch-send"

// --------------------------------------------------------------------------------------------------
// Message
// --------------------------------------------------------------------------------------------------

/**
 * Both way message request.
 *
 * The receiver should respond to a [cc.chenhe.weargallery.common.comm.bean.VersionResp] object in
 * the form of Message.
 */
const val PATH_REQ_VERSION = "/request/version"

/**
 * Both way message request.
 *
 * The receiver should respond to a [cc.chenhe.weargallery.common.comm.bean.IRemoteImageFolder] list in the form of a
 * Message.
 */
const val PATH_REQ_IMAGE_FOLDERS = "/request/image-folders"

/**
 * Both way message request with a [cc.chenhe.weargallery.common.comm.bean.ImagePreviewReq] object.
 *
 * The receiver should respond a DataMap that contains:
 *
 * - [ITEM_RESULT]
 * - [ITEM_IMAGE] The image data.
 */
const val PATH_REQ_IMAGE_PREVIEW = "/request/image-preview"

/**
 * Both way message request with a [cc.chenhe.weargallery.common.comm.bean.ImagesReq] object.
 *
 * The receiver should respond to a [cc.chenhe.weargallery.common.comm.bean.ImagesResp] in the
 * form of a Message.
 */
const val PATH_REQ_IMAGES = "/request/images"

/**
 * Both way message request with a [cc.chenhe.weargallery.common.comm.bean.ImageHdReq] object.
 *
 * The receiver should respond a DataMap that contains:
 *
 * - [ITEM_RESULT]
 * - [ITEM_IMAGE] The image data.
 */
const val PATH_REQ_IMAGE_HD = "/request/image-hd"

/**
 * Both way data request indicates that the caller is sending a picture to the opposite device.
 *
 * This request should contains the following fields:
 *
 *  - [ITEM_IMAGE]
 *  - [ITEM_IMAGE_INFO] The json data of [cc.chenhe.weargallery.common.bean.Image] object.
 *  - [ITEM_SAVE_PATH]
 *  - [ITEM_INDEX] Current picture index. Starts from 1.
 *  - [ITEM_TOTAL] Total count.
 */
const val PATH_SEND_IMAGE = "/send-image"


// --------------------------------------------------------------------------------------------------
// Items in data map
// --------------------------------------------------------------------------------------------------

/** Asset */
const val ITEM_IMAGE = "image"

const val ITEM_IMAGE_INFO = "image_info"

/**
 * Int
 *
 * Should be [RESULT_OK] or [RESULT_ERROR] unless otherwise specified.
 */
const val ITEM_RESULT = "result"

/**
 * String
 *
 * The relative path that the file should be saved to (without `/` prefix).
 */
const val ITEM_SAVE_PATH = "save_path"

/** Int */
const val ITEM_INDEX = "index"

/** Int */
const val ITEM_TOTAL = "total"


// --------------------------------------------------------------------------------------------------
// Others
// --------------------------------------------------------------------------------------------------

const val RESULT_OK = 1
const val RESULT_ERROR = -1
const val RESULT_NO_PERMISSION = -2