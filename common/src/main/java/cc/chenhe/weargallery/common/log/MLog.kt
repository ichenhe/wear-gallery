package cc.chenhe.weargallery.common.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

object MLog {

    var logLevel: Int = Log.INFO
    var maxLinesOfThrowable: Int = 8

    fun init(
        cacheDir: String,
        logDir: String,
        maxSize: Int = 5 * Mmap.MB,
        cacheSize: Int = 2 * Mmap.KB,
        expiration: Int = 3,
        minKeepFileCount: Int = 1,
        logLevel: Int = Log.INFO,
        maxLinesOfThrowable: Int = 8,
    ) {
        Mmap.init(cacheDir, logDir, maxSize, cacheSize, expiration, minKeepFileCount)
        this.logLevel = logLevel
        this.maxLinesOfThrowable = maxLinesOfThrowable
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
                appendLine().append("[STACK] ").append(printStackTrace(t, maxLinesOfThrowable, sb))
            }
            appendLine()
        }
        val data = sb.toString().toByteArray(Charsets.UTF_8)
        if (sb.capacity() > 1024) {
            sb = StringBuilder(64)
        }
        return data
    }

    private const val CAUSE = "Caused by: "
    private const val SUPPRESSED = "Suppressed: "

    private fun printStackTrace(
        e: Throwable,
        maxLineCount: Int,
        sb: StringBuilder = StringBuilder(maxLineCount * 10)
    ): StringBuilder {
        if (maxLineCount <= 0) {
            return sb
        }
        sb.appendLine(e.toString())
        val trace = e.stackTrace ?: return sb
        val count = min(maxLineCount, trace.size)
        for (i in 0 until count) {
            sb.append("\tat ").append(trace[i]).appendLine()
        }
        if (count < trace.size) {
            sb.append("\t... ").append(trace.size - count).append(" more").appendLine()
        }
        // Print suppressed exceptions, if any
        for (se in e.suppressed) {
            sb.append(
                printEnclosedStackTrace(se, maxLineCount, trace, SUPPRESSED, "\t", sb)
            )
        }
        // Print cause, if any
        e.cause?.also { cause ->
            sb.append(printEnclosedStackTrace(cause, maxLineCount, trace, CAUSE, "", sb))

        }
        return sb
    }

    private fun printEnclosedStackTrace(
        e: Throwable, maxLineCount: Int, enclosingTrace: Array<StackTraceElement>,
        caption: String, prefix: String, sb: StringBuilder
    ) {
        val trace = e.stackTrace
        var m = trace.size - 1
        var n = enclosingTrace.size - 1
        while (m >= 0 && n >= 0 && trace[m] == enclosingTrace[n]) {
            m--
            n--
        }
        val count = min(maxLineCount, m + 2)
        // Print our stack trace
        sb.append(prefix).append(caption).append(e.toString()).appendLine()
        for (i in 0 until count) {
            sb.append(prefix).append("\tat ").append(trace[i]).appendLine()
        }
        if (count < trace.size) {
            sb.append(prefix).append("\t... ").append(trace.size - count).append(" more")
                .appendLine()
        }
        // Print suppressed exceptions, if any
        val suppressedExceptions = e.suppressed
        for (se in suppressedExceptions) {
            sb.append(
                printEnclosedStackTrace(se, maxLineCount, trace, SUPPRESSED, prefix + "\t", sb)
            )
        }
        // Print cause, if any
        e.cause?.also { cause ->
            sb.append(printEnclosedStackTrace(cause, maxLineCount, trace, CAUSE, prefix, sb))
        }
    }
}