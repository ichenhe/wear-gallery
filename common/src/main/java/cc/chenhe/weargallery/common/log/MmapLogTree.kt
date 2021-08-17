package cc.chenhe.weargallery.common.log

import android.content.Context
import android.util.Log
import cc.chenhe.weargallery.common.util.getLogDir
import timber.log.Timber
import java.io.File

class MmapLogTree(context: Context, logLevel: Int) : Timber.Tree() {

    private val explicitTag = ThreadLocal<String?>()

    @get:JvmSynthetic
    private val tag: String?
        get() {
            val tag = explicitTag.get()
            if (tag != null) {
                explicitTag.remove()
            }
            return tag
        }

    init {
        MLog.init(
            cacheDir = File(context.filesDir, "log_cache").absolutePath,
            logDir = getLogDir(context).absolutePath,
            logLevel = logLevel,
            cacheSize = 4 * Mmap.KB,
        )
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.VERBOSE -> MLog.v(tag, message, t)
            Log.DEBUG -> MLog.d(tag, message, t)
            Log.INFO -> MLog.i(tag, message, t)
            Log.WARN -> MLog.w(tag, message, t)
            Log.ERROR -> MLog.e(tag, message, t)
        }
    }

    // Hack: https://github.com/JakeWharton/timber/issues/437
    // Aim to disable Timber's default throwable stack parser, since its output is too long.
    // Timber will call `isLoggable` which we can get the tag. We return false to prevent Timber from processing.
    // We have override all log methods to call our processing method.

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        explicitTag.set(tag)
        return false
    }

    private fun prepareLog(
        tag: String?,
        priority: Int,
        t: Throwable?,
        message: String?,
        vararg args: Any?
    ) {
        if (message.isNullOrEmpty()) {
            if (t == null) {
                return  // Swallow message if it's null and there's no throwable.
            }
            log(priority, tag, "", t)
        } else {
            if (args.isEmpty()) {
                log(priority, tag, message, t)
            } else {
                log(priority, tag, formatMessage(message, args), t)
            }
        }
    }

    override fun v(t: Throwable?) {
        super.v(t)
        prepareLog(tag, Log.VERBOSE, t, null)
    }

    override fun v(message: String?, vararg args: Any?) {
        super.v(message, *args)
        prepareLog(tag, Log.VERBOSE, null, message, *args)
    }

    override fun v(t: Throwable?, message: String?, vararg args: Any?) {
        super.v(t, message, *args)
        prepareLog(tag, Log.VERBOSE, t, message, *args)
    }

    override fun d(t: Throwable?) {
        super.d(t)
        prepareLog(tag, Log.DEBUG, t, null)
    }

    override fun d(message: String?, vararg args: Any?) {
        super.d(message, *args)
        prepareLog(tag, Log.DEBUG, null, message, *args)
    }

    override fun d(t: Throwable?, message: String?, vararg args: Any?) {
        super.d(t, message, *args)
        prepareLog(tag, Log.DEBUG, t, message, *args)
    }

    override fun i(t: Throwable?) {
        super.i(t)
        prepareLog(tag, Log.INFO, t, null)
    }

    override fun i(message: String?, vararg args: Any?) {
        super.i(message, *args)
        prepareLog(tag, Log.INFO, null, message, *args)
    }

    override fun i(t: Throwable?, message: String?, vararg args: Any?) {
        super.i(t, message, *args)
        prepareLog(tag, Log.INFO, t, message, *args)
    }

    override fun w(t: Throwable?) {
        super.w(t)
        prepareLog(tag, Log.WARN, t, null)
    }

    override fun w(message: String?, vararg args: Any?) {
        super.w(message, *args)
        prepareLog(tag, Log.WARN, null, message, *args)
    }

    override fun w(t: Throwable?, message: String?, vararg args: Any?) {
        super.w(t, message, *args)
        prepareLog(tag, Log.WARN, t, message, *args)
    }

    override fun e(t: Throwable?) {
        super.e(t)
        prepareLog(tag, Log.ERROR, t, null)
    }

    override fun e(message: String?, vararg args: Any?) {
        super.e(message, *args)
        prepareLog(tag, Log.ERROR, null, message, *args)
    }

    override fun e(t: Throwable?, message: String?, vararg args: Any?) {
        super.e(t, message, *args)
        prepareLog(tag, Log.ERROR, t, message, *args)
    }

    override fun wtf(t: Throwable?) {
        super.wtf(t)
        prepareLog(tag, Log.ASSERT, t, null)
    }

    override fun wtf(message: String?, vararg args: Any?) {
        super.wtf(message, *args)
        prepareLog(tag, Log.ASSERT, null, message, *args)
    }

    override fun wtf(t: Throwable?, message: String?, vararg args: Any?) {
        super.wtf(t, message, *args)
        prepareLog(tag, Log.ASSERT, t, message, *args)
    }

    override fun log(priority: Int, t: Throwable?) {
        super.log(priority, t)
        prepareLog(tag, priority, t, null)
    }

    override fun log(priority: Int, message: String?, vararg args: Any?) {
        super.log(priority, message, *args)
        prepareLog(tag, priority, null, message, *args)
    }

    override fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
        super.log(priority, t, message, *args)
        prepareLog(tag, priority, t, message, *args)
    }

}