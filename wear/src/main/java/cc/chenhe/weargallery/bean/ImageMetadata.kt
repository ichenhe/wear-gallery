package cc.chenhe.weargallery.bean

import cc.chenhe.weargallery.common.bean.IImage

data class ImageMetadata(
    val name: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val takenTime: Long,
    val modifiedTime: Long,
    val addedTime: Long = takenTime,
    val mime: String? = null,
)

fun IImage.toMetadata(): ImageMetadata {
    return ImageMetadata(
        name = name,
        width = width,
        height = height,
        size = size,
        takenTime = takenTime,
        modifiedTime = modifiedTime,
        addedTime = addedTime,
        mime = mime
    )
}