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

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.getVersionCode
import cc.chenhe.weargallery.repository.ImageRepository
import cc.chenhe.weargallery.service.UpgradeService.Companion.start
import cc.chenhe.weargallery.uilts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import java.io.File
import java.io.FileInputStream

private const val EXTRA_OLD_VERSION = "oldVersion"

private const val TAG = "UpgradeService"

private const val VERSION_5_1_1210 = 215010201L

/**
 * Service for application upgrade and data migration. Make sure call [start] to start this service to ensure that all
 * necessary arguments are provided.
 *
 * A local broadcast with action [ACTION_APPLICATION_UPGRADE_COMPLETE] is sent after the upgrade is complete. It will
 * also update the version number that was last activated.
 */
@Suppress("FunctionName")
class UpgradeService : Service() {

    companion object {

        fun start(context: Context, oldAppVersion: Long) {
            val i = Intent(context, UpgradeService::class.java).apply {
                putExtra(EXTRA_OLD_VERSION, oldAppVersion)
            }
            context.startService(i)
        }

        @Suppress("UNUSED_PARAMETER")
        fun shouldPerformUpgrade(oldVersion: Long, newVersion: Long): Boolean {
            return oldVersion in 0..VERSION_5_1_1210
        }
    }

    private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!running) {
            running = true

            val no = NotificationCompat.Builder(this, NOTIFY_CHANNEL_ID_UPGRADE)
                    .setSmallIcon(R.drawable.ic_notify_upgrade)
                    .setContentTitle(getString(R.string.notify_upgrade_title))
                    .setContentText(getString(R.string.notify_upgrade_text))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
            startForeground(NOTIFY_ID_UPGRADE, no)

            GlobalScope.launch {
                startUpgrade(intent.getLongExtra(EXTRA_OLD_VERSION, 0), getVersionCode(this@UpgradeService))
            }
        } else {
            logd(TAG, "Upgrade process already running, skip this command.")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun complete() {
        logd(TAG, "Upgrade complete.")
        lastStartVersion(this, getVersionCode(this))
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_APPLICATION_UPGRADE_COMPLETE))
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFY_CHANNEL_ID_UPGRADE,
                    getString(R.string.notify_channel_upgrade_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notify_channel_upgrade_description)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private suspend fun startUpgrade(oldVersion: Long, currentVersion: Long) = withContext(Dispatchers.Main) {
        logd(TAG, "Start upgrade process. $oldVersion → $currentVersion")

        if (oldVersion in 0..VERSION_5_1_1210) {
            migrate_5_1_1210()
        }

        complete()
    }

    @SuppressLint("RestrictedApi")
    private suspend fun migrate_5_1_1210() = withContext(Dispatchers.IO) {
        externalCacheDir?.let { cacheDir ->
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

            val hdDir = File(cacheDir, "originalPics")
            if (hdDir.isDirectory) {
                logd(TAG, "Copy pictures in HD cache dir to public dir.")
                val repo = get<ImageRepository>()
                hdDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        logd(TAG, "Save ${file.path}")
                        val time = ExifInterface(file).dateTime
                        FileInputStream(file).use { ins ->
                            repo.saveImage(this@UpgradeService, file.name, time, ins)
                        }
                        file.delete()
                    }
                }
                hdDir.deleteRecursively()
                logd(TAG, "Done!")
            }
        }
    }

}