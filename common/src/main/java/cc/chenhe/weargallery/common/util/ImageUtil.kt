package cc.chenhe.weargallery.common.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageDateGroup
import cc.chenhe.weargallery.common.bean.ImageFolder
import cc.chenhe.weargallery.common.comm.bean.ImagesResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.min

@SuppressLint("InlinedApi") // See https://stackoverflow.com/a/68515869/9150068
@Suppress("DEPRECATION") // We use `DATA` field to show file path information.
object ImageUtil {

    private const val IMAGE_SORT_ORDER =
        "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

    suspend fun groupImagesByDate(images: List<Image>): List<ImageDateGroup> =
        withContext(Dispatchers.Default) {
            val groups = mutableListOf<ImageDateGroup>()

            var lastDate = 0L
            var values: MutableList<Image> = mutableListOf()
            images.forEach {
                if (isSameDay(it.takenTime, lastDate)) {
                    values.add(it)
                } else {
                    if (values.isNotEmpty()) {
                        groups += ImageDateGroup(lastDate, values)
                    }
                    values = mutableListOf()
                    values.add(it)
                }
                lastDate = it.takenTime
            }
            if (values.isNotEmpty()) {
                groups += ImageDateGroup(lastDate, values)
            }
            groups
        }

    // --------------------------------------------------------------------------------
    // Safely get value from cursor.
    // Because whether 'get*()' method throws an exception is implementation-defined.
    // --------------------------------------------------------------------------------

    private fun Cursor.getStringSafely(column: Int) = try {
        if (isNull(column)) null else getString(column)
    } catch (e: Exception) {
        null
    }

    private fun Cursor.getIntOrZero(column: Int) = if (isNull(column)) 0 else getInt(column)

    private fun Cursor.getLongOrZero(column: Int) = if (isNull(column)) 0L else getLong(column)

    // ~Safely get value from cursor
    // --------------------------------------------------------------------------------


    /**
     * Query image folders (buckets) along with the first image as preview directly from local
     * media store.
     */
    suspend fun queryImageFolders(ctx: Context)
            : List<ImageFolder> = withContext(Dispatchers.IO) {

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
        )

        val selection =
            "${MediaStore.Images.Media.WIDTH} > ? AND ${MediaStore.Images.Media.HEIGHT} > ?"

        ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf("0", "0"),
            IMAGE_SORT_ORDER,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val bucketNameIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val takeTimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedTimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val modifyTimeIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            val result = mutableListOf<ImageFolder>()

            val bucketImgNum = mutableMapOf<Int, Int>() // bucketId -> img num
            while (cursor.moveToNext() && isActive) {
                val imgId = cursor.getLong(idIndex)
                val bucketId = cursor.getInt(bucketIndex)
                if (bucketImgNum.containsKey(bucketId)) {
                    bucketImgNum[bucketId] = requireNotNull(bucketImgNum[bucketId]) + 1
                    continue
                }

                bucketImgNum[bucketId] = 1
                val data = cursor.getStringSafely(dataIndex)
                val preview = ImageFolder.Preview(
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgId),
                    cursor.getLongOrZero(sizeIndex),
                    cursor.getLongOrZero(takeTimeIndex),
                    cursor.getLongOrZero(addedTimeIndex),
                    cursor.getLongOrZero(modifyTimeIndex),
                )
                result += ImageFolder(
                    cursor.getStringSafely(bucketNameIndex) ?: data?.filePath?.fileName ?: "",
                    cursor.getInt(bucketIndex),
                    -1, // collecting
                    preview,
                    data?.filePath ?: "",
                )
            }
            result.map { folder ->
                folder.copy(imgNum = bucketImgNum[folder.id]!!)
            }
        } ?: emptyList()
    }

    suspend fun queryBucketImages(context: Context, bucketId: Int): List<Image> {
        return queryImages(context, bucketId)
    }

    /**
     * A util to extract [Image] entity from cursor.
     *
     * The query must include all of [imageProjection] projection, and [getIndex] must be called
     * before [parserRow].
     */
    private class ImageParser {
        private var idIndex: Int = -1
        private var nameIndex: Int = -1
        private var dateTakenIndex: Int = -1
        private var dateModifiedIndex: Int = -1
        private var dateAddedIndex: Int = -1
        private var sizeIndex: Int = -1
        private var dataIndex: Int = -1
        private var widthIndex: Int = -1
        private var heightIndex: Int = -1
        private var mimeIndex: Int = -1
        private var bucketNameIndex: Int = -1
        private var bucketIndex: Int = -1

        val imageProjection: Array<String>
            get() = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID
            )

        fun getIndex(cursor: Cursor) {
            idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            bucketNameIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        }

        /**
         * **Must call [getIndex] method first.**
         */
        fun parserRow(cursor: Cursor): Image {
            assert(idIndex >= 0) { "Must call <getIndex> method first." }
            val id = cursor.getLong(idIndex)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val file: String? = cursor.getString(dataIndex)

            return Image(
                uri = uri,
                name = cursor.getStringSafely(nameIndex) ?: file?.fileName ?: "",
                takenTime = cursor.getLongOrZero(dateTakenIndex),
                modifiedTime = cursor.getLongOrZero(dateModifiedIndex),
                addedTime = cursor.getLongOrZero(dateAddedIndex),
                size = cursor.getLongOrZero(sizeIndex),
                width = cursor.getIntOrZero(widthIndex),
                height = cursor.getIntOrZero(heightIndex),
                mime = cursor.getStringSafely(mimeIndex),
                bucketName = cursor.getStringSafely(bucketNameIndex) ?: file?.filePath?.fileName
                ?: "",
                bucketId = cursor.getInt(bucketIndex),
                file = file
            )
        }
    }

    suspend fun queryPagingImages(
        context: Context,
        bucketId: Int,
        offset: Int,
        limit: Int,
    ): ImagesResp = withContext(Dispatchers.IO) {
        assert(offset >= 0) { "offset must >=0." }
        assert(limit > 0) { "limit must >0." }
        val selection =
            "${MediaStore.Images.Media.WIDTH} > ? AND ${MediaStore.Images.Media.HEIGHT} > ? AND ${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf("0", "0", bucketId.toString())

        MediaStore.Images.Media.MIME_TYPE

        // query total count
        var totalCount = 0
        context.applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            totalCount = cursor.count
        }
        if (totalCount == 0) {
            // shortcut for empty result
            return@withContext ImagesResp(emptyList(), 0)
        }

        // query paged data
        val imageParser = ImageParser()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            context.applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageParser.imageProjection,
                selection,
                selectionArgs,
                "$IMAGE_SORT_ORDER LIMIT $limit OFFSET $offset"
            )
        } else {
            context.applicationContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageParser.imageProjection,
                Bundle().apply {
                    // selection
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    // order
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, IMAGE_SORT_ORDER)
                    // pagination
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                },
                null
            )
        }?.use { cursor ->
            if (cursor.count == 0) {
                // shortcut for empty page
                return@withContext ImagesResp(emptyList(), totalCount)
            }
            imageParser.getIndex(cursor)

            // just in case the implementation dose not support pagination
            // we must skip the first {offset} rows and  pick up the next {limit} rows
            if (cursor.count > limit && offset > 0) {
                for (i in 0 until offset) {
                    if (!cursor.moveToNext())
                        break
                }
            }

            // assemble result
            val images = ArrayList<Image>(min(cursor.count, limit))
            while (images.size < limit && cursor.moveToNext()) {
                images += imageParser.parserRow(cursor)
            }
            ImagesResp(images, totalCount)
        } ?: ImagesResp(emptyList(), totalCount)
    }

    /**
     * Query all eligible images in [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] in order
     * of [IMAGE_SORT_ORDER].
     *
     * The image must meet the following conditions:
     *
     * - Both width and height are greater than 0.
     */
    suspend fun queryImages(
        context: Context,
        bucketId: Int? = null, // bucketId can be negative
        ids: List<Long>? = null,
    ): List<Image> = withContext(Dispatchers.IO) {
        var selection =
            "${MediaStore.Images.Media.WIDTH} > ? AND ${MediaStore.Images.Media.HEIGHT} > ?"

        val selectionArgs = mutableListOf("0", "0")

        if (bucketId != null) {
            selection += " AND ${MediaStore.Images.Media.BUCKET_ID} = ?"
            selectionArgs += bucketId.toString()
        }
        if (ids != null) {
            val idStr = ids.joinToString(separator = ",", prefix = "(", postfix = ")")
            selection += " AND ${MediaStore.Images.Media._ID} in $idStr"
        }

        val imageParser = ImageParser()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageParser.imageProjection,
            selection,
            selectionArgs.toTypedArray(),
            IMAGE_SORT_ORDER
        )?.use { cursor ->
            imageParser.getIndex(cursor)

            val result = ArrayList<Image>(cursor.count)
            while (cursor.moveToNext()) {
                result += imageParser.parserRow(cursor)
            }
            return@withContext result
        }
        emptyList()
    }

    /**
     * Get a cold flow to obtain image folders from the media store.
     *
     * The first value is null indicating loading state. A list will be sent once the init query
     * success. The returned flow will observe URI's change and send the last collection until it
     * is closed.
     */
    fun imageFoldersFlow(context: Context) =
        contentProviderFlow(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
            queryImageFolders(context)
        }

    fun imagesFlow(context: Context, bucketId: Int = -1) =
        contentProviderFlow(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
            queryBucketImages(context, bucketId)
        }
}

/**
 * A LiveData that provides image list. The data is fetched from content provider and update automatically.
 *
 * It is highly recommended to inherit this class and to implement [getCoroutineScope].
 */
open class ImageLiveData(context: Context) :
    ContentProviderLiveData<List<Image>>(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {

    override suspend fun getContentProviderValue(): List<Image> {
        return ImageUtil.queryImages(ctx)
    }
}