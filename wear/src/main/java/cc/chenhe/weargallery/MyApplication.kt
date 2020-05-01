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
import cc.chenhe.weargallery.common.util.FIREBASE_KEY_DEVICE
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().setCustomKey(FIREBASE_KEY_DEVICE, "watch")
        FirebaseAnalytics.getInstance(this).setUserProperty(FIREBASE_KEY_DEVICE, "watch")

        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }

        initWearMsger()
    }

    private fun initWearMsger() {
        try {
            WM.init(this, WM.Mode.AUTO)
            WM.bothWayTimeout = 5000L
        } catch (e: Exception) {
            Toast.makeText(this, "neither mms nor gms is available.", Toast.LENGTH_SHORT).show()
        }
    }

}