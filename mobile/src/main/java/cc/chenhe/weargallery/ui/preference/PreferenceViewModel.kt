package cc.chenhe.weargallery.ui.preference

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import cc.chenhe.weargallery.service.ForegroundService
import cc.chenhe.weargallery.utils.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

data class PreferenceUiState(
    val tipWhenWatchOperating: Boolean = false,
    val foregroundService: Boolean = false,
    val foregroundServiceNotification: Boolean = false,
    val overallNotification: Boolean = false,
    val sendImagesProgressNotification: Boolean = false,
    val sendImagesResultNotification: Boolean = false,
)

sealed class PreferenceIntent {
    data class SetTipWhenWatchOperating(val enable: Boolean) : PreferenceIntent()
    data class SetForegroundService(val enable: Boolean) : PreferenceIntent()
    object RecheckNotificationState : PreferenceIntent()
}

class PreferenceViewModel(
    application: Application,
    private val notificationChecker: NotificationChecker,
    notificationUtils: NotificationUtils,
) : AndroidViewModel(application) {
    private var _uiState = mutableStateOf(
        PreferenceUiState(
            tipWhenWatchOperating = isTipWithWatch(application),
            foregroundService = isForegroundService(application),
        )
    )
    val uiState: State<PreferenceUiState> = _uiState

    private val uiIntents = MutableSharedFlow<PreferenceIntent>()

    private val onSpChangedListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                PREFERENCE_TIP_WITH_WATCH -> _uiState.value =
                    _uiState.value.copy(tipWhenWatchOperating = isTipWithWatch(getApplication()))
                PREFERENCE_FOREGROUND_SERVICE -> _uiState.value =
                    _uiState.value.copy(foregroundService = isForegroundService(getApplication()))
            }
        }

    init {
        PreferenceManager.getDefaultSharedPreferences(application)
            .registerOnSharedPreferenceChangeListener(onSpChangedListener)
        notificationUtils.registerNotificationChannel(NotificationUtils.CHANNEL_ID_SENDING)
        notificationUtils.registerNotificationChannel(NotificationUtils.CHANNEL_ID_SEND_RESULT)
        recheckNotificationState()
        subscribeIntent()
    }

    override fun onCleared() {
        PreferenceManager.getDefaultSharedPreferences(getApplication())
            .unregisterOnSharedPreferenceChangeListener(onSpChangedListener)
    }

    fun sendIntent(intent: PreferenceIntent) {
        viewModelScope.launch {
            uiIntents.emit(intent)
        }
    }

    private fun subscribeIntent() {
        viewModelScope.launch {
            uiIntents.collect { intent ->
                when (intent) {
                    is PreferenceIntent.SetForegroundService -> {
                        setForegroundService(
                            getApplication(),
                            intent.enable
                        )
                        if (intent.enable) {
                            ForegroundService.start(getApplication())
                        } else {
                            ForegroundService.stop(getApplication())
                        }
                    }
                    is PreferenceIntent.SetTipWhenWatchOperating -> setTipWithWatch(
                        getApplication(),
                        intent.enable
                    )
                    PreferenceIntent.RecheckNotificationState -> recheckNotificationState()
                }
            }
        }
    }

    private fun recheckNotificationState() {
        _uiState.value = uiState.value.copy(
            foregroundServiceNotification = notificationChecker.isNotificationChannelEnabled(
                NotificationUtils.CHANNEL_ID_FOREGROUND_SERVICE
            ),
            overallNotification = notificationChecker.areNotificationsEnabled(),
            sendImagesProgressNotification = notificationChecker.isNotificationChannelEnabled(
                NotificationUtils.CHANNEL_ID_SENDING
            ),
            sendImagesResultNotification = notificationChecker.isNotificationChannelEnabled(
                NotificationUtils.CHANNEL_ID_SEND_RESULT
            ),
        )
    }
}