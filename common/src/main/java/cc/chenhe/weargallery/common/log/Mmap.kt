package cc.chenhe.weargallery.common.log

import cc.chenhe.weargallery.common.util.isSameDay
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

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
        flush(logFileNameFormat.format(Date(time)))
    }

    private fun flush(fileName: String, extName: String? = "log") {
        val cacheFile = getCacheFile()
        if (!cacheFile.exists()) {
            return
        }
        val logFile = determineLogFileName(fileName, extName)

        var rafi: RandomAccessFile? = null
        var rafo: RandomAccessFile? = null
        var fci: FileChannel? = null
        var fco: FileChannel? = null
        try {
            if (!logFile.exists()) {
                logFile.parentFile!!.mkdirs()
                logFile.createNewFile()
            }
            rafi = RandomAccessFile(cacheFile, "rw")
            rafo = RandomAccessFile(logFile, "rw")
            fci = rafi.channel
            fco = rafo.channel
            val cacheSize = fci.size()
            if (cacheSize == 0L) {
                return
            }

            val mbbi = fci.map(FileChannel.MapMode.READ_WRITE, 0, cacheSize)
            val contentLen = mbbi.readContentLength()
            mbbi.position(HEADER_SIZE)
            if (contentLen > 0) {
                val mbbo = fco.map(
                    FileChannel.MapMode.READ_WRITE,
                    fco.size(),
                    contentLen.toLong() - HEADER_SIZE
                )
                for (i in 0 until contentLen.toLong() - HEADER_SIZE) {
                    mbbo.put(mbbi.get())
                }
            }

            // clear cache file
            FileWriter(cacheFile).use { writer ->
                writer.write("")
                writer.flush()
            }
            mappedByteBuffer = null
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fci?.close()
            fco?.close()
            rafi?.close()
            rafo?.close()
        }
    }

    /**
     * Read the content length from the custom header.
     *
     * @return Content length. Return 0 if failed to read.
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

    fun write(data: ByteArray) {
        if (data.size > cacheSize) {
            throw IllegalArgumentException("data size cannot be larger than cacheSize.")
        }
        val currentTime = System.currentTimeMillis()
        if (!isSameDay(lastOpTime, currentTime)) {
            deleteExpiredFiles()
            logFileIndex = 0
        }
        lastOpTime = currentTime
        try {
            getMappedByteBuffer()?.writeData(data)
        } catch (e: BufferOverflowException) {
            // flush and clear the cache
            flush(currentTime)
            // retry
            getMappedByteBuffer()?.writeData(data)
        }
    }

    /**
     * Write data to buffer and update the header.
     *
     * @throws BufferUnderflowException
     * @throws IllegalArgumentException
     */
    private fun MappedByteBuffer.writeData(data: ByteArray) {
        // read file header
        val len = readContentLength()
        // must write data first, otherwise if the write fails (such as out of bounds),
        // the data length will be changed incorrectly
        position(len)
        put(data)
        position(0)
        put((len + data.size).toBytes())
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

    /**
     * This method ensures that the returned buffer's pointer location is at the end and readies
     * to write.
     */
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

            // in case there is legacy data
            flush(System.currentTimeMillis())

            fc.map(FileChannel.MapMode.READ_WRITE, 0, cacheSize.toLong()).also {
                mappedByteBuffer = it
                if (it.readContentLength() == 0) {
                    // write header if the file is totally empty
                    it.position(0)
                    it.put(HEADER_SIZE.toBytes())
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