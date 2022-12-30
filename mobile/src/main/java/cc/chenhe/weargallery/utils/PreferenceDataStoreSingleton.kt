package cc.chenhe.weargallery.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

class PreferenceDataStoreSingleton {
    companion object {
        @Volatile
        private var instance: DataStore<Preferences>? = null

        fun getInstance(context: Context): DataStore<Preferences> = instance ?: synchronized(this) {
            instance ?: PreferenceDataStoreFactory.create {
                context.applicationContext.preferencesDataStoreFile("settings")
            }.also { instance = it }
        }
    }
}