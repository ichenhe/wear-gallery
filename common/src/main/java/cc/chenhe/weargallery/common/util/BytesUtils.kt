package cc.chenhe.weargallery.common.util

/**
 * Convert a int to byte array. If the size of given array is greater than 4, index 0 to 3 will be
 * used.
 *
 * @return The result bytes. A new array will be created if the given bytes is null.
 */
fun Int.toBytes(bytes: ByteArray? = null): ByteArray {
    val result = bytes ?: ByteArray(4)
    if (result.size < 4) {
        throw IllegalArgumentException("bytes capacity must >= 4")
    }
    result[0] = (this shr 24).toByte()
    result[1] = (this shr 16).toByte()
    result[2] = (this shr 8).toByte()
    result[3] = this.toByte()
    return result
}

/**
 * Convert a byte array to int. The first 4 members will be used for conversion.
 */
fun ByteArray.toInt(): Int {
    if (this.size < 4) {
        throw IllegalArgumentException("bytes size must >= 4")
    }
    return ((this[0].toInt() and 0xff) shl 24) or
            ((this[1].toInt() and 0xff) shl 16) or
            ((this[2].toInt() and 0xff) shl 8) or
            (this[3].toInt() and 0xff)
}
