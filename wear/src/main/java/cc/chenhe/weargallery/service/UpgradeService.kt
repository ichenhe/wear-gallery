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

package cc.chenhe.weargallery.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.bean.toMetadata
import cc.chenhe.weargallery.common.util.ImageExifUtil
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.service.UpgradeService.Companion.start
import cc.chenhe.weargallery.uilts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*

private const val EXTRA_OLD_VERSION = "oldVersion"

private const val TAG = "UpgradeService"

private const val VERSION_5_1_1210 = 215010201L
private const val VERSION_6_0_1 = 220600011L

/**
 * Service for application upgrade and data migration. Make sure call [start] to start this service to ensure that all
 * necessary arguments are provided.
 *
 * A local broadcast with action [ACTION_APPLICATION_UPGRADE_COMPLETE] is sent after the upgrade is complete. It will
 * also update the version number that was last activated.
 */
@Suppress("FunctionName")
class UpgradeService : LifecycleService() {

    companion object {

        private var instance: UpgradeService? = null

        fun isRunning(): Boolean = try {
            instance?.ping() ?: false
        } catch (e: Exception) {
            false
        }

        fun start(context: Context, oldAppVersion: Long) {
            val i = Intent(context, UpgradeService::class.java).apply {
                putExtra(EXTRA_OLD_VERSION, oldAppVersion)
            }
            context.startService(i)
        }

        fun shouldPerformUpgrade(oldVersion: Long, newVersion: Long): Boolean {
            return oldVersion < newVersion
        }
    }

    private var running = false

    private fun ping(): Boolean = true

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerImportantPrecessingNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (running) {
            Timber.tag(TAG).w("Upgrade process already running, skip this command.")
            return START_NOT_STICKY
        }

        val no = NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_IMPORTANT_PROCESSING)
            .setSmallIcon(R.drawable.ic_notify_upgrade)
            .setContentTitle(getString(R.string.notify_upgrade_title))
            .setContentText(getString(R.string.notify_upgrade_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(NOTIFY_ID_UPGRADE, no)

        lifecycleScope.launch {
            startUpgrade(
                requireNotNull(intent).getLongExtra(EXTRA_OLD_VERSION, 0),
                getVersionCode(this@UpgradeService)
            )
        }
        return START_NOT_STICKY
    }

    private fun complete() {
        Timber.tag(TAG).d("Upgrade complete.")
        lastStartVersion(this, getVersionCode(this))
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(ACTION_APPLICATION_UPGRADE_COMPLETE))
        stopForeground(true)
        stopSelf()
    }

    private suspend fun startUpgrade(oldVersion: Long, currentVersion: Long) {
        Timber.tag(TAG).i("Upgrade process started: $oldVersion → $currentVersion")

        if (oldVersion < VERSION_5_1_1210) {
            migrate_5_1_1210()
            migrate_6_0_1()
        } else if (oldVersion < VERSION_6_0_1) {
            migrate_6_0_1()
        }

        complete()
    }

    private suspend fun migrate_5_1_1210() = withContext(Dispatchers.IO) {
        externalCacheDir?.let { cacheDir ->
            // delete legacy cache
            File(cacheDir, "galleryMicroPics").also { albumCacheDir ->
                if (albumCacheDir.isDirectory) {
                    albumCacheDir.deleteRecursively()
                }
            }
            File(cacheDir, "singleMicroPics").also { imageCacheDir ->
                if (imageCacheDir.isDirectory) {
                    imageCacheDir.deleteRecursively()
                }
            }

            // move pictures to media store
            val hdDir = File(cacheDir, "originalPics")
            if (hdDir.isDirectory) {
                Timber.tag(TAG).d("Copy pictures in HD cache dir to public dir.")
                val repo = get<ImageRepository>()
                hdDir.listFiles()?.forEach { file ->
                    if (!file.isFile)
                        return@forEach
                    val metadata = ImageExifUtil.parseImageFromFile(file).toMetadata()
                    Timber.tag(TAG).d("Save %s, metadata=%s", file.path, metadata.toString())
                    FileInputStream(file).use { ins ->
                        repo.saveImage(this@UpgradeService, metadata, ins)
                    }
                    file.delete()
                }
                hdDir.deleteRecursively()
                Timber.tag(TAG).d("Done!")
            }
        }
    }

    private suspend fun migrate_6_0_1() = withContext(Dispatchers.Default) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.deleteNotificationChannel("wg.upgrade")
        }
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

}