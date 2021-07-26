package cc.chenhe.weargallery.common.util

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> contentProviderFlow(context: Context, uri: Uri, fetch: suspend (() -> T)) = callbackFlow {
    trySend(null)
    val observer = object : ContentObserver(null) {

        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Since this method is called in work thread, we are safe to block it.
            @Suppress("BlockingMethodInNonBlockingContext")
            trySend(runBlocking { fetch() })
        }
    }

    trySend(fetch())

    context.applicationContext.contentResolver.registerContentObserver(uri, true, observer)

    awaitClose {
        context.applicationContext.contentResolver.unregisterContentObserver(observer)
    }
}