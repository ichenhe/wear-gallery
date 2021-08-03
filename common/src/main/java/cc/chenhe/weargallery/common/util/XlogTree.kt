package cc.chenhe.weargallery.common.util

import android.content.Context
import com.tencent.mars.xlog.Log
import com.tencent.mars.xlog.Xlog
import timber.log.Timber

class XlogTree(context: Context, logLevel: Int) : Timber.Tree() {
    companion object {
        init {
            System.loadLibrary("c++_shared")
            System.loadLibrary("marsxlog")
        }
    }

    init {
        Log.setLogImp(Xlog())

        Log.appenderOpen(
            logLevel,
            Xlog.AppednerModeAsync,
//            File(context.filesDir, "xlog_cache").absolutePath,
            "", //FIXME: add xlog cache. https://github.com/Tencent/mars/issues/960
            getLogDir(context).absolutePath,
            null,
            10
        )
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            android.util.Log.VERBOSE -> Log.v(tag, message)
            android.util.Log.DEBUG -> Log.d(tag, message)
            android.util.Log.INFO -> Log.i(tag, message)
            android.util.Log.WARN -> if (t == null)
                Log.w(tag, message)
            else
                Log.w(tag, "$message   %s", t)
            android.util.Log.ERROR -> if (t == null)
                Log.e(tag, message)
            else
                Log.printErrStackTrace(tag, t, message)
        }
    }
}