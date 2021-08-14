package cc.chenhe.weargallery.common.bean

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageFolder(
    val name: String,
    val id: Int,
    val imgNum: Int,
    val preview: Preview,
    /** For display only, may be empty */
    val path: String,
) : Parcelable {

    /**
     * The latest time of the preview picture, include takenTime, addedTime and modifiedTime.
     *
     * If the preview is the latest image of this folder, then this field can be regarded as the
     * latest time of this folder.
     */
    val latestTime: Long
        get() = maxOf(preview.takenTime, preview.addedTime, preview.modifiedTime)

    @Parcelize
    data class Preview(
        val uri: Uri,
        val size: Long,
        val takenTime: Long,
        val addedTime: Long,
        val modifiedTime: Long,
    ) : Parcelable
}
