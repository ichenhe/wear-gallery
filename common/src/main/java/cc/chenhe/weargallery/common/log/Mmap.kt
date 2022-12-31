package cc.chenhe.weargallery.common.log

import android.annotation.SuppressLint
import android.util.Log
import cc.chenhe.weargallery.common.log.Mmap.readContentLength
import cc.chenhe.weargallery.common.util.isSameDay
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

internal object Mmap {
    const val KB = 1024
    const val MB = 1024 * KB

    private const val DAY = 24 * 3600 * 1000

    private const val CACHE_FILE_NAME = "cache"
    private const val HEADER_SIZE = 4

    val isInitialized get() = ::logDir.isInitialized

    private lateinit var cacheDir: String
    private lateinit var logDir: String
    private var maxSize = 5 * MB
    private var cacheSize = 2 * KB

    /** The expiration time of the log files. (days) */
    private var expiration: Int = 0

    /** At least the number of log files retained, regardless of whether they are expired or not. */
    private var minKeepFileCount = 1
        set(value) {
            assert(value >= 0)
            field = value
        }

    private var logFileIndex = 0

    private val logFileNameFormat by lazy {
        SimpleDateFormat("yyyyMMdd", Locale.US)
    }

    private val logTimeFormat by lazy {
        SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] [Z]", Locale.US)
    }

    private fun getCacheFile(): File = File(cacheDir, CACHE_FILE_NAME)

    fun init(
        cacheDir: String,
        logDir: String,
        maxSize: Int = 5 * MB,
        cacheSize: Int = 2 * KB,
        expiration: Int = 3,
        minKeepFileCount: Int = 1,
    ) {
        this.cacheDir = cacheDir
        this.logDir = logDir
        this.maxSize = maxSize
        this.cacheSize = cacheSize
        this.expiration = expiration
        this.minKeepFileCount = minKeepFileCount
        if (this.cacheSize <= HEADER_SIZE) {
            throw IllegalArgumentException("cacheSize must be greater than $HEADER_SIZE")
        }

        // flush legacy cache and write init block
        write(
            """
            
            
            >>>--------------------------------------------------------------------------------------
            ${logTimeFormat.format(Date())} <init>
            <<<--------------------------------------------------------------------------------------
            
            
        """.trimIndent().toByteArray(Charsets.UTF_8)
        )
    }

    fun flush(time: Long = System.currentTimeMillis()) {
        getMappedByteBuffer()?.also { mappedByteBuffer ->
            flush(logFileNameFormat.format(Date(time)), mappedByteBuffer = mappedByteBuffer)
        }
    }

    private fun flush(time: Long = System.currentTimeMillis(), mappedByteBuffer: MappedByteBuffer) {
        flush(logFileNameFormat.format(Date(time)), mappedByteBuffer = mappedByteBuffer)
    }

    /**
     * Flush the cache to the real log file. Reset the cache file to contain header only and ready
     * for next write.
     */
    private fun flush(
        fileName: String,
        extName: String? = "log",
        mappedByteBuffer: MappedByteBuffer,
    ) {
        val cacheFile = getCacheFile()
        if (!cacheFile.exists()) {
            return
        }
        val logFile = determineLogFileName(fileName, extName)
        var randomAccessLogFile: RandomAccessFile? = null
        var logFileChannel: FileChannel? = null
        try {
            val contentLen = mappedByteBuffer.readContentLength()
            if (contentLen <= HEADER_SIZE) {
                // cache is empty
                return
            }

            if (!logFile.exists()) {
                logFile.parentFile!!.mkdirs()
                logFile.createNewFile()
            }
            randomAccessLogFile = RandomAccessFile(logFile, "rw")
            logFileChannel = randomAccessLogFile.channel
            logFileChannel.position(logFileChannel.size())
            mappedByteBuffer.copyTo(logFileChannel, HEADER_SIZE, contentLen - HEADER_SIZE)

            // clear cache file
            mappedByteBuffer.writeContentLength(HEADER_SIZE)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            logFileChannel?.close()
            randomAccessLogFile?.close()
        }
    }

    /**
     * Copy data `[startPosition, startPosition + length)` from [MappedByteBuffer] to file to [dst].
     *
     * It is caller's responsibility to set [dst]'s position to where want to write to.
     */
    private fun MappedByteBuffer.copyTo(
        dst: FileChannel,
        startPosition: Int,
        length: Int,
        bufferSize: Int = 8 * KB
    ) {
        val buffer = ByteBuffer.allocate(bufferSize)
        position(startPosition)
        var copied = 0
        while (copied < length) {
            if (!buffer.hasRemaining()) {
                buffer.flip()
                dst.write(buffer)
                buffer.clear()
            }
            buffer.put(get())
            copied++
        }
        if (buffer.position() > 0) {
            buffer.flip()
            dst.write(buffer)
        }
    }

    /**
     * Calculate remaining space for new content. Use [readContentLength] as current length.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun MappedByteBuffer.remainingForCache(): Int = capacity() - readContentLength()

    /**
     * Read the content length from the custom header.
     *
     * @return Content length (including header itself). Return 0 if failed to read.
     */
    private fun MappedByteBuffer.readContentLength(): Int {
        return try {
            position(0)
            val bytes = ByteArray(HEADER_SIZE)
            get(bytes)
            bytes.toInt()
        } catch (e: BufferUnderflowException) {
            0
        }
    }

    /**
     * Write header representing the length of cache file.
     * @param length The length of cache file (including header itself.
     */
    private fun MappedByteBuffer.writeContentLength(length: Int) {
        position(0)
        put(length.toBytes())
    }

    private fun determineLogFileName(fileName: String, ext: String?): File {
        var f =
            File(logDir, fileName + "_$logFileIndex" + if (ext != null) ".$ext" else null)
        while (f.exists() && f.length() >= maxSize) {
            logFileIndex++
            f = File(logDir, fileName + "_$logFileIndex" + if (ext != null) ".$ext" else null)
        }
        return f
    }

    private var lastOpTime: Long = 0L

    @SuppressLint("LogNotTimber") // intent behavior
    fun write(data: ByteArray) {
        val currentTime = System.currentTimeMillis()
        if (!isSameDay(lastOpTime, currentTime)) {
            deleteExpiredFiles()
            logFileIndex = 0
        }
        lastOpTime = currentTime
        try {
            getMappedByteBuffer()?.also { mappedByteBuffer ->
                var offset = 0
                while (offset < data.size) {
                    if (mappedByteBuffer.remainingForCache() <= 0) {
                        flush(mappedByteBuffer = mappedByteBuffer)
                    }
                    val currentLength = mappedByteBuffer.readContentLength()
                    val writtenLength = min(
                        data.size - offset,
                        mappedByteBuffer.capacity() - currentLength
                    )
                    if (writtenLength <= 0) {
                        throw IllegalStateException("Still no remaining space in cache file after flush. capacity: ${mappedByteBuffer.capacity()}")
                    }
                    Log.w(
                        "MMAP",
                        "writeLength:$writtenLength, remaining:${mappedByteBuffer.remainingForCache()}"
                    )
                    mappedByteBuffer.writeData(data, offset, writtenLength, currentLength)
                    offset += writtenLength
                }
                flush(mappedByteBuffer = mappedByteBuffer)
            }
        } catch (e: BufferOverflowException) {
            Log.e("MMAP", "No remaining space in cache file", e)
        }
    }

    /**
     * Append data to buffer and update the header. The length of data should be less than
     * [MappedByteBuffer.remainingForCache].
     *
     * @param offset The offset within the [data] of the first byte to be read; must be non-negative
     * and no larger than data.size.
     * @param length The number of bytes to be read from the given array; must be non-negative and
     * no larger than data.size - offset.
     * @param currentLength The length of cache file (including header itself).
     *
     * @throws BufferUnderflowException
     * @throws IllegalArgumentException
     */
    private fun MappedByteBuffer.writeData(
        data: ByteArray,
        offset: Int = 0,
        length: Int = data.size,
        currentLength: Int,
    ) {
        // must write data first, otherwise if the write fails (such as out of bounds),
        // the data length will be changed incorrectly
        position(currentLength)
        put(data, offset, length)
        writeContentLength(currentLength + length)
    }

    private fun deleteExpiredFiles() {
        val dir = File(logDir)
        if (!dir.exists()) {
            return
        }
        val logs = dir.listFiles() ?: return
        if (logs.size <= minKeepFileCount) {
            return
        }
        logs.sortBy { it.lastModified() }
        val currentTime = System.currentTimeMillis()
        for (i in 0 until logs.size - minKeepFileCount) {
            val f = logs[i]
            if (currentTime - f.lastModified() > expiration * DAY) {
                f.delete()
            } else {
                break
            }
        }
    }

    private var mappedByteBuffer: MappedByteBuffer? = null

    private fun getMappedByteBuffer(): MappedByteBuffer? {
        mappedByteBuffer?.let { return it }
        var raf: RandomAccessFile? = null
        var fc: FileChannel? = null
        return try {
            val cacheFile = getCacheFile()
            if (!cacheFile.exists()) {
                cacheFile.parentFile!!.mkdirs()
                cacheFile.createNewFile()
            }
            raf = RandomAccessFile(cacheFile, "rw")
            fc = raf.channel

            fc.map(FileChannel.MapMode.READ_WRITE, 0, cacheSize.toLong()).also {
                mappedByteBuffer = it

                val contentSize = it.readContentLength()
                if (contentSize > HEADER_SIZE) {
                    // there is legacy data
                    flush(System.currentTimeMillis(), mappedByteBuffer = it)
                } else {
                    it.writeContentLength(HEADER_SIZE)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            fc?.close()
            raf?.close()
        }
    }

    private fun ByteArray.toInt(): Int {
        return ((this[0].toInt() and 0xFF) shl 24) or
                ((this[1].toInt() and 0xFF) shl 16) or
                ((this[2].toInt() and 0xFF) shl 8) or
                (this[3].toInt() and 0xFF)
    }

    private fun Int.toBytes(): ByteArray {
        val bytes = ByteArray(4)
        bytes[3] = (this).toByte()
        bytes[2] = ((this shr 8)).toByte()
        bytes[1] = ((this shr 16)).toByte()
        bytes[0] = ((this shr 24)).toByte()
        return bytes
    }

}