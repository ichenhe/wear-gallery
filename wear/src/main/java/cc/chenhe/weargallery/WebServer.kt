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

package cc.chenhe.weargallery

import android.content.Context
import cc.chenhe.weargallery.bean.toMetadata
import cc.chenhe.weargallery.common.util.ImageExifUtil
import cc.chenhe.weargallery.repository.ImageRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.io.InputStream

private const val MIME_JS = "text/javascript"
private const val MIME_CSS = "text/css"
private const val MIME_GIF = "image/gif"
private const val MIME_JPG = "image/jpeg"
private const val MIME_PNG = "image/png"
private const val MIME_JSON = "application/json"

private const val PORT = 7160

class WebServer(context: Context, private val repository: ImageRepository) : NanoHTTPD(PORT) {

    private val ctx = context.applicationContext

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        // shortcut to upload
        if (uri == "/upload" && session.method == Method.POST) {
            return upload(session, "image")
        }

        val filename = if (uri == "/") "index.html" else uri.substring(1)
        if (filename == "favicon.ico") {
            // shortcut to ico
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, null)
        }
        var mimeType = MIME_HTML

        if (filename.endsWith(".js")) {
            mimeType = MIME_JS
        } else if (filename.endsWith(".css")) {
            mimeType = MIME_CSS
        } else if (filename.endsWith(".gif")) {
            mimeType = MIME_GIF
        } else if (filename.endsWith(".jpeg") || filename.endsWith(".jpg")) {
            mimeType = MIME_JPG
        } else if (filename.endsWith(".png")) {
            mimeType = MIME_PNG
        }

        var ins: InputStream? = null
        return try {
            // Do NOT use `use` here because the input stream is needed after return this function.
            ins = ctx.assets.open("web" + File.separator + filename)
            newFixedLengthResponse(Response.Status.OK, mimeType, ins, ins.available().toLong())
        } catch (e: IOException) {
            e.printStackTrace()
            ins?.close()
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, e.message)
        }

    }

    @Suppress("SameParameterValue")
    private fun upload(session: IHTTPSession, fieldName: String): Response {
        val files: MutableMap<String, String> = mutableMapOf()
        try {
            session.parseBody(files)
        } catch (e: IOException) {
            e.printStackTrace()
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, e.message)
        } catch (e: ResponseException) {
            e.printStackTrace()
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, e.message)
        }

        val tmpFile = files[fieldName]?.let { File(it) }
        val fileName = session.parameters[fieldName]?.firstOrNull()
        if (tmpFile == null || fileName == null) {
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_HTML,
                "Unknown file or params."
            )
        }

        val localUri = runBlocking(Dispatchers.IO) {
            val metadata = ImageExifUtil.parseImageFromFile(tmpFile).toMetadata()
            tmpFile.inputStream().use { ins ->
                repository.saveImage(ctx, metadata, ins)
            }
        }

        tmpFile.delete()
        return if (localUri != null) {
            newFixedLengthResponse(Response.Status.OK, MIME_JSON, "{\"code\":0}")
        } else {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, "Copy file failed.")
        }
    }

}