package cc.chenhe.weargallery.ui.preference

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.chenhe.weargallery.common.util.getLogDir
import cc.chenhe.weargallery.common.util.xlogAppenderFlushSafely
import cc.chenhe.weargallery.repo.PreferenceRepo
import cc.chenhe.weargallery.service.ForegroundService
import cc.chenhe.weargallery.utils.NotificationChecker
import cc.chenhe.weargallery.utils.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class PreferenceUiState(
    val tipWhenWatchOperating: Boolean = false,
    val foregroundService: Boolean = false,
    val foregroundServiceNotification: Boolean = false,
    val overallNotification: Boolean = false,
    val sendImagesProgressNotification: Boolean = false,
    val sendImagesResultNotification: Boolean = false,
    val exportLogState: ExportLogState = ExportLogState.Idle,
) {
    sealed class ExportLogState {
        object Idle : ExportLogState()
        object Preparing : ExportLogState()
        class Prepared(val uri: Uri) : ExportLogState()

        /** no log file */
        object Empty : ExportLogState()
    }
}

sealed class PreferenceIntent {
    data class SetTipWhenWatchOperating(val enable: Boolean) : PreferenceIntent()
    data class SetForegroundService(val enable: Boolean) : PreferenceIntent()
    object RecheckNotificationState : PreferenceIntent()
    object SendLog : PreferenceIntent()
    class SaveLog(val uri: Uri) : PreferenceIntent()
    object ResetExportLogState : PreferenceIntent()
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
                    PreferenceIntent.SendLog -> sendLogFile()
                    PreferenceIntent.ResetExportLogState ->
                        _uiState.value =
                            _uiState.value.copy(exportLogState = PreferenceUiState.ExportLogState.Idle)
                    is PreferenceIntent.SaveLog -> saveLogFile(intent.uri)
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

    private suspend fun saveLogFile(uri: Uri) {
        if (uiState.value.exportLogState == PreferenceUiState.ExportLogState.Preparing) {
            return
        }
        _uiState.value =
            _uiState.value.copy(exportLogState = PreferenceUiState.ExportLogState.Preparing)
        val file = prepareLogFile()
        if (file != null) {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    file.inputStream().use { ins -> ins.copyTo(it) }
                }
            }
            PreferenceUiState.ExportLogState.Idle
        } else {
            PreferenceUiState.ExportLogState.Empty
        }.also { newState ->
            _uiState.value = _uiState.value.copy(exportLogState = newState)
        }
    }

    private suspend fun sendLogFile() {
        if (uiState.value.exportLogState == PreferenceUiState.ExportLogState.Preparing) {
            return
        }
        _uiState.value =
            _uiState.value.copy(exportLogState = PreferenceUiState.ExportLogState.Preparing)
        val file = prepareLogFile()
        if (file != null) {
            val uri = FileProvider.getUriForFile(
                getApplication(),
                "cc.chenhe.weargallery.fileprovider",
                file
            )
            PreferenceUiState.ExportLogState.Prepared(uri)
        } else {
            PreferenceUiState.ExportLogState.Empty
        }.also { newState ->
            _uiState.value = _uiState.value.copy(exportLogState = newState)
        }
    }

    /**
     * Compress log files and save to cache directory.
     * @return The shareable uri of the compressed file.
     */
    private suspend fun prepareLogFile(): File? = withContext(Dispatchers.IO) {
        xlogAppenderFlushSafely()
        val ctx = getApplication<Application>()
        val logs = getLogDir(ctx).listFiles()
        if (logs.isNullOrEmpty()) {
            return@withContext null
        }
        logs.sortByDescending { it.lastModified() }

        val outDir = File(ctx.externalCacheDir ?: ctx.cacheDir, "exported_log")
        if (!outDir.exists()) {
            outDir.mkdirs()
        }
        val outFile = File(outDir, "weargallery_log.zip")
        if (outFile.exists()) {
            outFile.delete()
        }
        ZipOutputStream(outFile.outputStream()).use { out ->
            logs.take(2).forEach { f ->
                if (isActive) {
                    out.putNextEntry(ZipEntry(f.name))
                    f.inputStream().use { input -> input.copyTo(out) }
                }
            }
        }
        outFile
    }
}