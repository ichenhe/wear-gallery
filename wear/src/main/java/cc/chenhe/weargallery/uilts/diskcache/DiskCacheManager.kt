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

package cc.chenhe.weargallery.uilts.diskcache

import android.content.Context
import android.net.Uri
import cc.chenhe.weargallery.bean.RemoteImage
import cc.chenhe.weargallery.common.util.getVersionCode
import okio.Okio
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest

private const val MAX_SIZE = 1024 * 1024 * 40L // 40MB


private const val CACHE_MOBILE_PREVIEW = "mobileImagesPreview"

/**
 * There must be no more than one active instance for a given name at a time.
 */
sealed class DiskCacheManager(context: Context, name: String, maxSize: Long = MAX_SIZE) {

    private val cache: DiskLruCache

    init {
        cache = DiskLruCache.open(getLruCacheFile(context, name), getVersionCode(context), maxSize)
    }

    private fun getLruCacheFile(context: Context, name: String): File {
        return File((context.externalCacheDir ?: context.cacheDir), name)
    }

    private fun edit(key: String): DiskLruCache.Editor? {
        return cache.edit(key.md5())
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    }

    // ------------------------------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------------------------------

    fun getCacheFile(key: String) = cache.getFile(key.md5())

    /**
     * [ins] will be closed internally.
     */
    fun put(key: String, ins: InputStream): Boolean {
        // do NOT use md5() here!
        val editor = edit(key) ?: return false
        try {
            Okio.source(ins).use { src ->
                Okio.sink(editor.getTargetFile()).use {
                    Okio.buffer(it).use { buffer ->
                        buffer.writeAll(src)
                    }
                }
            }
            editor.commit()
            return true
        } catch (e: java.lang.Exception) {
            editor.abort()
            return false
        } finally {
            ins.close()
        }
    }

    fun remove(key: String): Boolean {
        return cache.remove(key.md5())
    }

}

/**
 * Manage the picture preview cache. All Uri parameters refer to the remote device by default.
 */
class MobilePreviewCacheManager private constructor(context: Context)
    : DiskCacheManager(context, CACHE_MOBILE_PREVIEW) {
    companion object {
        private var instance: MobilePreviewCacheManager? = null

        fun getInstance(context: Context): MobilePreviewCacheManager {
            return instance ?: synchronized(this) {
                instance ?: MobilePreviewCacheManager(context).also { instance = it }
            }
        }
    }

    /**
     * [ins] will be closed internally.
     */
    fun saveImage(uri: Uri, ins: InputStream) {
        put(uri.toString(), ins)
    }

    fun getCacheImage(uri: Uri): File? {
        return getCacheFile(uri.toString())
    }

    fun deleteCacheImage(uri: Uri): Boolean {
        return remove(uri.toString())
    }

    fun deleteCacheImage(remoteImages: Collection<RemoteImage>) {
        remoteImages.forEach {
            remove(it.uri.toString())
        }
    }

}