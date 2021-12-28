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
 */

package cc.chenhe.weargallery

import android.app.Application
import android.util.Log
import cc.chenhe.weargallery.common.log.MmapLogTree
import cc.chenhe.weargallery.common.util.getLogDir
import cc.chenhe.weargallery.common.util.xlogAppenderCloseSafely
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import com.microsoft.appcenter.crashes.model.ErrorReport
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(MmapLogTree(this, Log.DEBUG))
            val crashesListener = object : AbstractCrashesListener() {
                override fun getErrorAttachments(report: ErrorReport?): MutableIterable<ErrorAttachmentLog>? {
                    // worker thread
                    xlogAppenderCloseSafely()
                    val logs = getLogDir(this@MyApplication).listFiles()
                    if (logs.isNullOrEmpty())
                        return null
                    logs.sortByDescending { it.lastModified() }
                    return mutableListOf(
                        ErrorAttachmentLog.attachmentWithBinary(
                            logs.first().readBytes(),
                            logs.first().name,
                            "application/x-xlog"
                        )
                    )
                }
            }
            Crashes.setListener(crashesListener)
            AppCenter.start(
                this, "736e9baf-3c69-42ef-8c7e-7aac964d6949",
                Analytics::class.java, Crashes::class.java
            )
        }

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .componentRegistry {
            // https://coil-kt.github.io/coil/gifs/#gifs
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder(this@MyApplication))
            } else {
                add(GifDecoder())
            }
        }
        .build()
}