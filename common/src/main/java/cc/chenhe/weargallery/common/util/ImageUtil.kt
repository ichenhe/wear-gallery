package cc.chenhe.weargallery.common.util

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.bean.ImageDateGroup
import cc.chenhe.weargallery.common.bean.ImageFolderGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION") // We use `DATA` field to show file path information.
object ImageUtil {

    suspend fun groupImagesByDate(images: List<Image>): List<ImageDateGroup> = withContext(Dispatchers.Default) {
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

    suspend fun groupImagesByFolder(images: List<Image>): List<ImageFolderGroup> = withContext(Dispatchers.Default) {
        val collect = LinkedHashMap<Int, MutableList<Image>>(32)

        var lastBucket = -1
        var values = mutableListOf<Image>()
        images.forEach {
            // Because we are using absolute paths, there should be no null. Just in case, let's skip them.
            val currentBucket = it.bucketId
            if (currentBucket == lastBucket) {
                values.add(it)
            } else {
                if (values.isNotEmpty()) {
                    if (collect.containsKey(lastBucket)) {
                        collect[lastBucket]!!.addAll(values)
                    } else {
                        collect[lastBucket] = values
                    }
                }
                values = mutableListOf()
                values.add(it)
            }
            lastBucket = currentBucket
        }
        if (values.isNotEmpty()) {
            if (collect.containsKey(lastBucket)) {
                collect[lastBucket]!!.addAll(values)
            } else {
                collect[lastBucket] = values
            }
        }
        val result = ArrayList<ImageFolderGroup>(collect.size)
        collect.forEach { (k, v) ->
            val first = v.first()
            result += ImageFolderGroup(k, first.bucketName, first.file?.filePath, v)
        }
        result
    }

    suspend fun queryBucketImages(context: Context, bucketId: Int): List<Image> {
        return queryImages(context, bucketId)
    }

    /**
     * Query all eligible images in [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] in descending order of
     * [MediaStore.Video.Media.DATE_TAKEN].
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

        var selection = "${MediaStore.Images.Media.WIDTH} > ? AND ${MediaStore.Images.Media.HEIGHT} > ?"

        val selectionArgs = mutableListOf("0", "0")

        if (bucketId >= 0) {
            selection += " AND ${MediaStore.Images.Media.BUCKET_ID} = ?"
            selectionArgs.add(bucketId.toString())
        }
        if (ids != null) {
            val idStr = ids.joinToString(separator = ",", prefix = "(", postfix = ")")
            selection += " AND ${MediaStore.Images.Media._ID} in $idStr"
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media._ID} DESC"
        val images = mutableListOf<Image>()
        context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs.toTypedArray(),
                sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val file: String? = cursor.getString(dataIndex)
                images += Image(
                        uri = uri,
                        name = cursor.getString(nameIndex),
                        takenTime = cursor.getLong(dateIndex),
                        size = cursor.getLong(sizeIndex),
                        width = cursor.getInt(widthIndex),
                        height = cursor.getInt(heightIndex),
                        mime = cursor.getString(mimeIndex),
                        bucketName = cursor.getString(bucketNameIndex) ?: file?.fileName ?: "",
                        bucketId = cursor.getInt(bucketIndex),
                        file = file)
            }
        }
        images
    }

}

/**
 * A LiveData that provides image list. The data is fetched from content provider and update automatically.
 *
 * It is highly recommended to inherit this class and to implement [getCoroutineScope].
 */
open class ImageLiveData(context: Context)
    : ContentProviderLiveData<List<Image>>(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {

    override suspend fun getContentProviderValue(): List<Image> {
        return ImageUtil.queryImages(ctx)
    }
}