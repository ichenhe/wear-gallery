package cc.chenhe.weargallery.common.log

import android.content.Context
import cc.chenhe.weargallery.common.util.getLogDir
import timber.log.Timber
import java.io.File

class MmapLogTree(context: Context, logLevel: Int) : Timber.Tree() {

    init {
        MLog.init(
            cacheDir = File(context.filesDir, "log_cache").absolutePath,
            logDir = getLogDir(context).absolutePath,
            logLevel = logLevel,
        )
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            android.util.Log.VERBOSE -> MLog.v(tag, message, t)
            android.util.Log.DEBUG -> MLog.d(tag, message, t)
            android.util.Log.INFO -> MLog.i(tag, message, t)
            android.util.Log.WARN -> MLog.w(tag, message, t)
            android.util.Log.ERROR -> MLog.e(tag, message, t)
        }
    }
}