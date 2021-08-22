package cc.chenhe.weargallery.common.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import cc.chenhe.weargallery.common.bean.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ImageExifUtil {

    @SuppressLint("RestrictedApi") // For convenience's sake
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatchers
    suspend fun parseImageFromIns(context: Context, uri: Uri): Image? =
        withContext(Dispatchers.IO) {
            val cr = context.contentResolver
            var image = cr.openInputStream(uri)?.use { ins ->
                val exif = ExifInterface(ins)
                Image(
                    uri,
                    name = uri.toString().fileName,
                    takenTime = exif.dateTimeOriginal ?: 0L,
                    modifiedTime = exif.dateTime ?: 0L,
                    addedTime = 0L,
                    size = 0L,
                    width = 0,
                    height = 0,
                    mime = cr.getType(uri),
                    bucketId = -1,
                    bucketName = uri.toString().filePath ?: "",
                    file = null
                )
            } ?: return@withContext null

            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { ins ->
                    BitmapFactory.decodeStream(ins, null, options)
                    image = image.copy(
                        width = options.outWidth,
                        height = options.outHeight,
                        mime = if (image.mime == null) options.outMimeType else image.mime
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
            image
        }

    @SuppressLint("RestrictedApi") // For convenience's sake
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatchers
    suspend fun parseImageFromFile(file: File): Image = withContext(Dispatchers.IO) {
        var image = Image(
            Uri.fromFile(file),
            name = file.name,
            takenTime = 0L,
            modifiedTime = 0L,
            addedTime = 0L,
            size = file.length(),
            width = 0,
            height = 0,
            mime = null,
            bucketId = -1,
            bucketName = file.parent?.fileName ?: "",
            file = file.absolutePath
        )

        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            image = image.copy(
                width = options.outWidth,
                height = options.outHeight,
                mime = options.outMimeType
            )
        } catch (e: Exception) {
            // ignore
        }
        try {
            val exif = ExifInterface(file)
            image = image.copy(
                takenTime = exif.dateTimeOriginal ?: 0L,
                modifiedTime = exif.dateTime ?: 0L,
            )
        } catch (e: Exception) {
            // ignore
        }
        image
    }
}