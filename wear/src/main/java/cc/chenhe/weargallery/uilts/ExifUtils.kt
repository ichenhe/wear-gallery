package cc.chenhe.weargallery.uilts

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExifUtils {

    private val dateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    }

    fun getOriginalDateTime(file: File): Long {
        val s = ExifInterface(file).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        if (s == null || s.length != 19 || s.first() == Char(0x20))
            return 0L
        return try {
            dateFormatter.parse(s)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}