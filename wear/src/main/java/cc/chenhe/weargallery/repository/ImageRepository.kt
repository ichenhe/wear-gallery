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

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.database.getStringOrNull
import cc.chenhe.weargallery.bean.ImageMetadata
import cc.chenhe.weargallery.db.RemoteImageDao
import cc.chenhe.weargallery.uilts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.InputStream
import kotlin.text.isNullOrEmpty

open class ImageRepository(
    // need this to update cache's 'local_uri' field
    protected val remoteImageDao: RemoteImageDao,
) {
    companion object {
        private const val TAG = "ImageRepository"
    }

    /**
     * Represent a pending operation that need user's approve. [Activity.startIntentSender] should
     * be called with [intentSender] to make a request.
     *
     * For us, it currently only represents a delete operation.
     */
    class Pending(
        private val pendingIntent: PendingIntent,
        /** Resources associated with this operation. */
        val uris: Collection<Uri>,
    ) {
        val intentSender: IntentSender get() = pendingIntent.intentSender
    }

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
        metadata: ImageMetadata,
        ins: InputStream,
        folderName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        if (scopeStorageEnabled()) {
            saveImageScopeStorage(context, metadata, ins, folderName)
        } else {
            saveImageLegacy(context, metadata, ins, folderName)
        }
    }

    private fun ImageMetadata.toContentValues(): ContentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.WIDTH, width)
        put(MediaStore.Images.Media.HEIGHT, height)
        put(MediaStore.Images.Media.SIZE, size)
        if (takenTime > 0L)
            put(MediaStore.Images.Media.DATE_TAKEN, takenTime)
        if (modifiedTime > 0L)
            put(MediaStore.Images.Media.DATE_MODIFIED, modifiedTime)
        if (addedTime > 0L)
            put(MediaStore.Images.Media.DATE_ADDED, addedTime)
        if (!mime.isNullOrEmpty())
            put(MediaStore.Images.Media.MIME_TYPE, mime)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("BlockingMethodInNonBlockingContext") // IO Dispatcher
    private suspend fun saveImageScopeStorage(
        context: Context,
        metadata: ImageMetadata,
        ins: InputStream,
        folderName: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        // create item
        val saveFolder = if (folderName.isNullOrEmpty()) {
            imageReceiveRelativePath
        } else {
            Environment.DIRECTORY_PICTURES + File.separator + folderName
        }

        var values = metadata.toContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, saveFolder)
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
        metadata: ImageMetadata,
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
        val file = File(folder, metadata.name)
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

        // insert into or update media store
        @Suppress("InlinedApi")
        val values = metadata.toContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
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
     * Delete local images. If result is nonnull, [Activity.startIntentSenderForResult] must be
     * called with [PendingIntent.getIntentSender] to request user's confirmation. If activity
     * result is [Activity.RESULT_OK], the contents have been deleted.
     *
     * Update the database if it is associated with a remote cache. But if confirmation is required,
     * this job will be handed over to final callback to finish.
     *
     * @return PendingIntent that use to request user's confirmation.
     */
    suspend fun deleteLocalImage(context: Context, uris: Collection<Uri>): Pending? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (tryDeleteLocalImageDirectly(context, uris)) {
                remoteImageDao.clearLocalUri(uris)
                return null
            }
            val p = MediaStore.createDeleteRequest(context.applicationContext.contentResolver, uris)
            Pending(p, uris)
            // we don't know if the user will approve this deletion, so clear cache fields later
        } else {
            deleteLocalImagesLegacy(context, uris)
            remoteImageDao.clearLocalUri(uris)
            return null
        }
    }

    /**
     * On or above Android 11, try to delete contents directly. This method will check the uris'
     * permission first.
     *
     * @return Whether the content has been deleted.
     */
    private suspend fun tryDeleteLocalImageDirectly(context: Context, uris: Collection<Uri>)
            : Boolean = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext true
        // Too many iteration queries can affect performance.
        // The main use case of this function is:
        // When the user deletes the HD cache in the remote picture menu, try to avoid additional
        // authorization requests. Because usually we have direct access to these files.
        // In this case, only one picture should be deleted at a time. So 5 is enough.
        if (uris.size > 5) return@withContext false
        for (uri in uris) {
            if (context.checkUriPermission(
                    uri,
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext false
            }
        }
        // we have all permissions, delete it directly
        for (uri in uris) {
            context.applicationContext.contentResolver.delete(uri, null, null)
        }
        true
    }

    @Suppress("DEPRECATION") // for legacy
    private suspend fun deleteLocalImagesLegacy(context: Context, uris: Collection<Uri>) =
        withContext(Dispatchers.IO) {
            for (uri in uris) {
                // try to delete file
                try {
                    context.applicationContext.contentResolver.query(
                        uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                                ?.also {
                                    File(it).delete()
                                }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }

                try {
                    context.applicationContext.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    Timber.tag(TAG).e("Failed to delete local image. uri=%s", uri)
                }
            }
        }

    /**
     * Delete **all** images in given buckets.
     *
     * @see deleteLocalImage
     */
    suspend fun deleteLocalImageFolders(
        context: Context,
        bucketIds: Collection<Int>
    ): Pending? = withContext(Dispatchers.IO) {
        if (bucketIds.isEmpty()) return@withContext null // shortcut for empty
        val uris = queryImageIds(context.applicationContext, bucketIds).map {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.toLong())
        }
        if (uris.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val p = MediaStore.createDeleteRequest(context.applicationContext.contentResolver, uris)
            Pending(p, uris)
            // we don't know if the user will approve this deletion, so clear cache fields later
        } else {
            deleteImageFoldersLegacy(context, bucketIds)
            remoteImageDao.clearLocalUri(uris)
            null
        }
    }

    /**
     * Query all eligible images in [MediaStore.Images.Media.EXTERNAL_CONTENT_URI].
     *
     * @param bucketId Images' bucket id.
     */
    private suspend fun queryImageIds(context: Context, bucketId: Collection<Int>): List<Int> =
        withContext(Dispatchers.IO) {
            context.applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.BUCKET_ID} in (?)",
                arrayOf(bucketId.joinToString(", ")),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val result = ArrayList<Int>(cursor.count)
                while (cursor.moveToNext()) {
                    result += cursor.getInt(idIndex)
                }
                return@withContext result
            }
            emptyList()
        }

    /**
     * @return The number of rows deleted.
     */
    private suspend fun deleteImageFoldersLegacy(
        context: Context,
        bucketIds: Collection<Int>
    ): Int = withContext(Dispatchers.IO) {
        context.applicationContext.contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.Images.Media.BUCKET_ID} in (?)",
            arrayOf(bucketIds.joinToString(", "))
        )
    }
}