package cc.chenhe.weargallery.common.comm.bean

import cc.chenhe.weargallery.common.bean.Image
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImagesResp(
    val data: List<Image>,
    val totalCount: Int,
)
