package cc.chenhe.weargallery.common.util

import timber.log.Timber

object UncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        Timber.tag("Uncaught").e(e, "Uncaught Exception")
        xlogAppenderCloseSafely()
        throw e
    }
}