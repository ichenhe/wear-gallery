package cc.chenhe.weargallery.ui.main

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.chenhe.weargallery.common.util.GITHUB
import cc.chenhe.weargallery.repo.CheckUpdateRepo
import cc.chenhe.weargallery.utils.checkStoragePermissions
import cc.chenhe.weargallery.utils.requiredStoragePermission
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class MainUiState(
    val showTicTip: Boolean = false,
    /** 更新链接，null 表示无需更新。 */
    val updateInfo: MainScreenViewModel.UpdateInfo? = null,
    /**
     * Permissions that needed by not granted.
     *
     * If all necessary permissions are granted, this will be empty. Or it will contains all
     * permissions (including necessary and others).
     */
    val lackingNecessaryPermissions: List<String> = emptyList(),
)

sealed class MainUiIntent {
    object RecheckPermissions : MainUiIntent()
}

class MainScreenViewModel(application: Application, private val checkUpdateRepo: CheckUpdateRepo) :
    AndroidViewModel(application) {
    private val _uiState = mutableStateOf(
        MainUiState(showTicTip = isTicHelperInstalled(application))
    )
    val uiState: State<MainUiState> = _uiState
    private val uiIntents = MutableSharedFlow<MainUiIntent>()

    data class UpdateInfo(val url: String, val versionName: String)

    init {
        viewModelScope.launch {
            val info = checkUpdateRepo.checkUpdate()?.let { resp ->
                UpdateInfo(
                    url = resp.mobile?.url ?: GITHUB,
                    versionName = resp.mobile?.latest?.name ?: ""
                )
            }
            Snapshot.withMutableSnapshot {
                _uiState.value = _uiState.value.copy(updateInfo = info)
            }
        }
        viewModelScope.launch {
            uiIntents.collect {
                handleUiIntent(it)
            }
        }
    }

    fun sendUiIntent(intent: MainUiIntent) {
        viewModelScope.launch {
            uiIntents.emit(intent)
        }
    }

    private fun handleUiIntent(intent: MainUiIntent) {
        when (intent) {
            is MainUiIntent.RecheckPermissions -> recheckNecessaryPermissions()
        }
    }

    private fun isTicHelperInstalled(context: Context): Boolean {
        val names = arrayOf(
            "com.mobvoi.companion.aw",
            "com.mobvoi.baiding",
        )
        val pm = context.packageManager
        names.forEach {
            try {
                val packageInfoFlags = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(it, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(it, packageInfoFlags)
                }
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.tag("MainFr").d("Fail to check the package $it")
            }
        }
        return false
    }

    private fun recheckNecessaryPermissions() {
        val permissions: List<String> =
            if (checkStoragePermissions(getApplication())) {
                // all necessary permissions are granted
                emptyList()
            } else {
                mutableListOf(requiredStoragePermission()).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        _uiState.value = _uiState.value.copy(lackingNecessaryPermissions = permissions)
    }
}