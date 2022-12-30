package cc.chenhe.weargallery.ui.preference

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.chenhe.weargallery.repo.PreferenceRepo
import cc.chenhe.weargallery.service.ForegroundService
import cc.chenhe.weargallery.utils.NotificationChecker
import cc.chenhe.weargallery.utils.NotificationUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val preferenceRepo: PreferenceRepo,
) : AndroidViewModel(application) {
    private var _uiState = mutableStateOf(PreferenceUiState())
    val uiState: State<PreferenceUiState> = _uiState

    private val uiIntents = MutableSharedFlow<PreferenceIntent>()

    init {
        notificationUtils.registerNotificationChannel(NotificationUtils.CHANNEL_ID_SENDING)
        notificationUtils.registerNotificationChannel(NotificationUtils.CHANNEL_ID_SEND_RESULT)
        recheckNotificationState()
        viewModelScope.launch {
            preferenceRepo.shouldTipOnWatchOperating().collectLatest {
                _uiState.value = _uiState.value.copy(tipWhenWatchOperating = it)
            }
        }
        viewModelScope.launch {
            preferenceRepo.keepForegroundService().collectLatest {
                _uiState.value = _uiState.value.copy(foregroundService = it)
            }
        }

        subscribeIntent()
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
                        preferenceRepo.setKeepForegroundService(intent.enable)
                        if (intent.enable) {
                            ForegroundService.start(getApplication())
                        } else {
                            ForegroundService.stop(getApplication())
                        }
                    }
                    is PreferenceIntent.SetTipWhenWatchOperating ->
                        preferenceRepo.setTipOnWatchOperating(intent.enable)
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