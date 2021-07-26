package cc.chenhe.weargallery.common.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageDateGroup
import cc.chenhe.weargallery.common.bean.ImageFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("InlinedApi") // See https://stackoverflow.com/a/68515869/9150068
@Suppress("DEPRECATION") // We use `DATA` field to show file path information.
object ImageUtil {

    private const val IMAGE_SORT_ORDER =
        "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

    private fun parentDirName(file: String?): String? {
        if (file == null) return null
        val sp = file.split(File.separator)
        if (sp.isEmpty()) return file
        if (sp.size == 1) return sp[0]
        return sp[sp.size - 2]
    }

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
                val preview = ImageFolder.Preview(
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgId),
                    cursor.getLong(sizeIndex),
                    cursor.getLong(takeTimeIndex),
                    cursor.getLong(addedTimeIndex),
                    cursor.getLong(modifyTimeIndex),
                )
                result += ImageFolder(
                    cursor.getString(bucketNameIndex),
                    cursor.getInt(bucketIndex),
                    -1,
                    preview,
                    cursor.getString(dataIndex).filePath ?: File.separator,
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
     * Query all eligible images in [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] in order
     * of [IMAGE_SORT_ORDER].
     *
     * The image must meet the following conditions:
     *
     * - Both width and height are greater than 0.
     */
    suspend fun queryImages(context: Context, bucketId: Int = -1, ids: List<Long>? = null)
            : List<Image> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )

        var selection =
            "${MediaStore.Images.Media.WIDTH} > ? AND ${MediaStore.Images.Media.HEIGHT} > ?"

        val selectionArgs = mutableListOf("0", "0")

        if (bucketId >= 0) {
            selection += " AND ${MediaStore.Images.Media.BUCKET_ID} = ?"
            selectionArgs.add(bucketId.toString())
        }
        if (ids != null) {
            val idStr = ids.joinToString(separator = ",", prefix = "(", postfix = ")")
            selection += " AND ${MediaStore.Images.Media._ID} in $idStr"
        }

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs.toTypedArray(),
            IMAGE_SORT_ORDER
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketNameIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            val result = ArrayList<Image>(cursor.count)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val file: String? = cursor.getString(dataIndex)
                result += Image(
                    uri = uri,
                    name = cursor.getString(nameIndex),
                    takenTime = cursor.getLong(dateIndex),
                    size = cursor.getLong(sizeIndex),
                    width = cursor.getInt(widthIndex),
                    height = cursor.getInt(heightIndex),
                    mime = cursor.getString(mimeIndex),
                    bucketName = cursor.getString(bucketNameIndex) ?: parentDirName(file) ?: "",
                    bucketId = cursor.getInt(bucketIndex),
                    file = file
                )
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