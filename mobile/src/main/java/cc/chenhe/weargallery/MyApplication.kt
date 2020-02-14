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
import android.widget.Toast
import cc.chenhe.lib.wearmsger.WM
import cc.chenhe.weargallery.utils.WearMode
import cc.chenhe.weargallery.utils.getWearMode
import cc.chenhe.weargallery.utils.logd
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

private const val TAG = "MyApplication"

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }

        initWearMsger()
    }

    private fun initWearMsger() {
        try {
            val mode = when (getWearMode(this)) {
                WearMode.Auto -> WM.Mode.AUTO
                WearMode.Gms -> WM.Mode.GMS
                WearMode.Mms -> WM.Mode.MMS
            }
            logd(TAG, "Init wear msger, mode=$mode")
            WM.init(this, mode)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.app_none_transfer_available, Toast.LENGTH_LONG).show()
        }
    }

}