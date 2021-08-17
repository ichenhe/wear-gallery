package cc.chenhe.weargallery.common.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckUpdateResp(
    val wear: Device?,
    val mobile: Device?,
) {
    @JsonClass(generateAdapter = true)
    data class Device(
        val latest: Ver? = null,
        val url: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class Ver(
        val code: Long = 0,
        val name: String? = null
    )
}
