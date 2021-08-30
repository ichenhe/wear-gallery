package cc.chenhe.weargallery.common.comm.bean

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionResp(
    val code: Long,
    val name: String,
    /** Minimum version required for pairing applications. */
    val minPairedVersion: Long,
)
