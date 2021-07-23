package cc.chenhe.weargallery.utils

import android.content.Context
import android.net.Uri
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.Compression
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

/**
 * @return 压缩过的图片文件，保存在 App cache 文件夹。
 */
suspend fun compressImage(
    context: Context,
    uri: Uri,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    compressionPatch: Compression.() -> Unit = { default() }
): File {
    val tmpSrcFile = from(context, uri)
    return try {
        Compressor.compress(context, tmpSrcFile, coroutineContext, compressionPatch)
    } finally {
        tmpSrcFile.delete()
    }
}

private fun from(context: Context, uri: Uri): File {
    val tempFile: File = File.createTempFile("weargallery_" + generateRandomString(12), null)
    tempFile.deleteOnExit()
    context.contentResolver.openInputStream(uri)?.use { ins ->
        FileOutputStream(tempFile).use {
            ins.copyTo(it)
        }
    } ?: return tempFile
    return tempFile
}
