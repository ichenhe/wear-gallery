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

package cc.chenhe.weargallery.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import cc.chenhe.weargallery.uilts.IMAGE_RECEIVE_FOLDER_NAME
import cc.chenhe.weargallery.uilts.imageReceiveRelativePath
import cc.chenhe.weargallery.uilts.scopeStorageEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

open class ImageRepository {

    /**
     * Save a image input stream to file system and media store.
     *
     * **Notice:** It is the caller's responsibility to close the [ins].
     *
     * @param folderName Relative path to save the picture to (without `/` prefix). The final path is
     * [Environment.DIRECTORY_PICTURES]/[folderName]. If this parameter is null or empty, default value is
     * [IMAGE_RECEIVE_FOLDER_NAME].
     *
     * @return The [Uri] in local device, `null` if failed to save.
     */
    suspend fun saveImage(
        context: Context,
        displayName: String,
        takenTime: Long,
        ins: InputStream,
        folderName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        if (scopeStorageEnabled()) {
            saveImageScopeStorage(context, displayName, ins, folderName)
        } else {
            saveImageLegacy(context, displayName, takenTime, ins, folderName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatcher
    private suspend fun saveImageScopeStorage(
        context: Context,
        displayName: String,
        ins: InputStream,
        folderName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        // create item
        val saveFolder = if (folderName.isNullOrEmpty()) {
            imageReceiveRelativePath
        } else {
            Environment.DIRECTORY_PICTURES + File.separator + folderName
        }
        var values = ContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, saveFolder)
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.IS_PENDING, true)
        }
        val uri =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
        // write file
        context.contentResolver.openOutputStream(uri)?.use { os ->
            ins.copyTo(os)
        } ?: return@withContext null

        // finish insert
        values = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, false)
        }
        context.contentResolver.update(uri, values, null, null)
        return@withContext uri
    }

    @Suppress("DEPRECATION", "BlockingMethodInNonBlockingContext") // IO Dispatcher
    private suspend fun saveImageLegacy(
        context: Context,
        displayName: String,
        takenTime: Long,
        ins: InputStream,
        folderName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        // create folder
        val folder = if (folderName.isNullOrEmpty()) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_RECEIVE_FOLDER_NAME
            )
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                folderName
            )
        }
        if (!folder.isDirectory) {
            folder.mkdirs()
        }

        // write file: override old data
        val file = File(folder, displayName)
        file.outputStream().buffered().use { out ->
            ins.copyTo(out)
            out.flush()
        }

        // check by file path if it already exists in media store
        // existUri=null means not exist
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DATA} = ?"
        val existUri: Uri? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
            selection, arrayOf(file.absolutePath), null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            } else {
                null
            }
        }

        // decode picture information
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        // insert into or update media store
        @Suppress("InlinedApi")
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.WIDTH, options.outWidth)
            put(MediaStore.Images.Media.HEIGHT, options.outHeight)
            put(MediaStore.Images.Media.DATE_TAKEN, takenTime)
            put(MediaStore.Images.Media.MIME_TYPE, options.outMimeType)
        }
        if (existUri != null) {
            context.contentResolver.update(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(ContentUris.parseId(existUri).toString())
            )
            existUri
        } else {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    /**
     * Delete local image from files and media store. Update the database if it is associated with a remote cache.
     *
     * @return `null` if success. An [IntentSender] will be returned if scope storage is enabled and we have no
     * permissions to delete the target.
     */
    suspend fun deleteLocalImage(context: Context, localUri: Uri)
            : IntentSender? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(
                localUri,
                "${MediaStore.Images.Media._ID} = ?",
                arrayOf(ContentUris.parseId(localUri).toString())
            )
        } catch (e: SecurityException) {
            if (scopeStorageEnabled()) {
                val recoverableException =
                    e as? RecoverableSecurityException ?: throw RuntimeException(e.message, e)
                return@withContext recoverableException.userAction.actionIntent.intentSender
            } else {
                throw e
            }
        }
        null
    }
}