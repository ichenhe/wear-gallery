package cc.chenhe.weargallery.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking


@Suppress("PrivatePropertyName")
class PreferenceRepo(private val dataStore: DataStore<Preferences>) {

    private val LAST_START_VERSION = longPreferencesKey("last_start_version")
    private val LAST_CHECK_UPDATE_TIME = longPreferencesKey("last_check_update_time")
    private val TIP_ON_WATCH_OPERATING = booleanPreferencesKey("tip_on_watch_operating")
    private val KEEP_FOREGROUND_SERVICE = booleanPreferencesKey("keep_foreground_service")

    fun getLastStartVersionSync(): Long? = runBlocking {
        dataStore.data.map { it[LAST_START_VERSION] }.first()
    }

    suspend fun setLastStartVersion(newValue: Long) {
        dataStore.edit {
            it[LAST_START_VERSION] = newValue
        }
    }

    fun getLastCheckUpdateTimeSync(): Long = runBlocking {
        dataStore.data.map { it[LAST_CHECK_UPDATE_TIME] }.first() ?: 0
    }

    suspend fun setLastCheckUpdateTime(newValue: Long) {
        dataStore.edit {
            it[LAST_CHECK_UPDATE_TIME] = newValue
        }
    }

    fun shouldTipOnWatchOperating() = dataStore.data.map { it[TIP_ON_WATCH_OPERATING] ?: true }

    suspend fun setTipOnWatchOperating(newValue: Boolean) {
        dataStore.edit {
            it[TIP_ON_WATCH_OPERATING] = newValue
        }
    }

    fun keepForegroundService() = dataStore.data.map { it[KEEP_FOREGROUND_SERVICE] ?: false }

    suspend fun setKeepForegroundService(newValue: Boolean) {
        dataStore.edit {
            it[KEEP_FOREGROUND_SERVICE] = newValue
        }
    }
}