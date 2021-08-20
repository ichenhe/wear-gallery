package cc.chenhe.weargallery.common.comm.bean

import cc.chenhe.weargallery.common.bean.Image
import com.squareup.moshi.JsonClass

/**
 * An entity in batch sending.
 */
@JsonClass(generateAdapter = true)
data class SendItem(
    val image: Image,
    val folder: String?,
    val current: Int,
    val total: Int,
)