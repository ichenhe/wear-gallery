package cc.chenhe.weargallery.common.log

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object MLog {

    var logLevel: Int = Log.INFO

    fun init(
        cacheDir: String,
        logDir: String,
        maxSize: Int = 5 * Mmap.MB,
        cacheSize: Int = 2 * Mmap.KB,
        expiration: Int = 3,
        minKeepFileCount: Int = 1,
        logLevel: Int = Log.INFO,
    ) {
        Mmap.init(cacheDir, logDir, maxSize, cacheSize, expiration, minKeepFileCount)
        this.logLevel = logLevel
    }

    fun isInitialized(): Boolean {
        return Mmap.isInitialized
    }

    fun v(tag: String?, message: String, t: Throwable? = null) {
        if (Log.DEBUG >= logLevel) {
            Mmap.write(buildLog('V', tag, message, t))
        }
    }

    fun d(tag: String?, message: String, t: Throwable? = null) {
        if (Log.DEBUG >= logLevel) {
            Mmap.write(buildLog('D', tag, message, t))
        }
    }

    fun i(tag: String?, message: String, t: Throwable? = null) {
        if (Log.INFO >= logLevel) {
            Mmap.write(buildLog('I', tag, message, t))
        }
    }

    fun w(tag: String?, message: String, t: Throwable? = null) {
        if (Log.INFO >= logLevel) {
            Mmap.write(buildLog('W', tag, message, t))
        }
    }

    fun e(tag: String?, message: String, t: Throwable?) {
        if (Log.INFO >= logLevel) {
            Mmap.write(buildLog('E', tag, message, t))
        }
    }

    private val logTimeFormat by lazy {
        SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] [Z]", Locale.US)
    }

    private val date by lazy {
        Date()
    }

    var sb = StringBuilder(64)

    private fun buildLog(level: Char, tag: String?, message: String, t: Throwable?): ByteArray {
        sb.clear()
        date.time = System.currentTimeMillis()
        sb.apply {
            append(logTimeFormat.format(date))
            append(" [").append(level).append("] [").append(tag).append("] ")
            append(message)
            if (t != null) {
                appendLine().append("[STACK] ")
                t.printStackTrace(sb)
            }
            appendLine()
        }
        val data = sb.toString().toByteArray(Charsets.UTF_8)
        if (sb.capacity() > 1024) {
            sb = StringBuilder(64)
        }
        return data
    }

    private fun Throwable.printStackTrace(sb: StringBuilder): StringBuilder {
        val sw = StringWriter()
        printStackTrace(PrintWriter(sw))
        sb.append(sw.toString())
        return sb
    }
}