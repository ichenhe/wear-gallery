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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright 2016 Zheng Zibin
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package cc.chenhe.weargallery.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Another implementation of the [Luban](https://github.com/Curzibn/Luban/blob/master/DESCRIPTION.md) algorithm.
 *
 * This is a simple tool to compress the image in coroutines.
 */
object Luban {

    private fun computeSize(srcWidth: Int, srcHeight: Int): Int {
        // make sure they are even
        val w = if (srcWidth and 1 == 1) srcWidth + 1 else srcWidth
        val h = if (srcHeight and 1 == 1) srcHeight + 1 else srcHeight

        val longSide: Int = max(w, h)
        val shortSide: Int = min(w, h)

        val ratio = shortSide.toFloat() / longSide

        return if (ratio <= 1 && ratio > 0.5625) {
            if (longSide < 1664) {
                1
            } else if (longSide < 4990) {
                2
            } else if (longSide in 4991 until 10240) {
                4
            } else {
                if (longSide / 1280 == 0) 1 else longSide / 1280
            }
        } else if (ratio <= 0.5625 && ratio > 0.5) {
            if (longSide / 1280 == 0) 1 else longSide / 1280
        } else {
            ceil(longSide / (1280.0 / ratio)).toInt()
        }
    }

    /**
     * Extract the rotation degrees form the exif.
     *
     * @return The rotation degrees, default is 0.
     */
    private fun readDegrees(cr: ContentResolver, uri: Uri): Int {
        return cr.openInputStream(uri)?.use { stream ->
            val o = ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL)
            when (o) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        return Matrix().let { m ->
            m.postRotate(degrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }
    }

    /**
     * Call [compress] in the try block.
     *
     * @return `null` if any exceptions are caught.
     */
    suspend fun compressQuietly(cr: ContentResolver, uri: Uri): ByteArrayOutputStream? {
        return try {
            compress(cr, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Call [compressLegacy] in the try block.
     *
     * @return `null` if any exceptions are caught.
     */
    suspend fun compressLegacyQuietly(
            cr: ContentResolver,
            uri: Uri,
            requireWidth: Int,
            requireHeight: Int): ByteArrayOutputStream? {
        return try {
            compressLegacy(cr, uri, requireWidth, requireHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compress the given image to `webp` format. Gif animation will be lost.
     *
     * @throws FileNotFoundException There is no data associated with the URI.
     */
    suspend fun compress(cr: ContentResolver, uri: Uri): ByteArrayOutputStream? = withContext(Dispatchers.IO) {
        // calculate scale
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        options.inSampleSize = cr.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
            computeSize(options.outWidth, options.outHeight)
        } ?: return@withContext null

        // decode bitmap
        options.inJustDecodeBounds = false
        var bitmap = cr.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return@withContext null

        // rotate
        bitmap = readDegrees(cr, uri).let { degrees ->
            if (degrees == 0) bitmap else rotate(bitmap, degrees)
        }

        // compress
        val outs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 60, outs)
        bitmap.recycle()
        return@withContext outs
    }

    /**
     * Compress the given image to `webp` format. Gif animation will be lost.
     *
     * @throws FileNotFoundException There is no data associated with the URI.
     */
    suspend fun compressLegacy(
            cr: ContentResolver,
            uri: Uri,
            requireWidth: Int,
            requireHeight: Int): ByteArrayOutputStream? = withContext(Dispatchers.IO) {
        // calculate scale
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        cr.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val w = options.outWidth
        val h = options.outHeight
        if (w <= 0 || h <= 0) {
            return@withContext null
        }

        var scale = 1
        if (w >= h && w > requireWidth) {
            scale = w / requireWidth
        } else if (h > requireHeight) {
            scale = h / requireHeight
        }
        scale = max(1, scale)

        // decode bitmap
        options.inSampleSize = scale
        options.inJustDecodeBounds = false
        var bitmap = cr.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return@withContext null

        // rotate
        bitmap = readDegrees(cr, uri).let { degrees ->
            if (degrees == 0) bitmap else rotate(bitmap, degrees)
        }

        // compress
        val outs = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 50, outs)
        bitmap.recycle()
        return@withContext outs
    }
}