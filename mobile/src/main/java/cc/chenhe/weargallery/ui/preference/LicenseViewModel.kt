package cc.chenhe.weargallery.ui.preference

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

data class LicenseUiState(
    val licenses: List<LicenseViewModel.License> = emptyList()
)

class LicenseViewModel : ViewModel() {
    companion object {
        private const val MIT = "MIT License"
        private const val APACHE_2 = "Apache Software License 2.0"
    }

    val licenses = listOf(
        License("Android Open Source Project", "AOSP", APACHE_2, "https://source.android.com/"),
        License("Compressor", "zetbaitsu", MIT, "https://github.com/zetbaitsu/Compressor"),
        License("Coil", "coil-kt", APACHE_2, "https://github.com/coil-kt/coil"),
        License("Koin", "Koin", APACHE_2, "https://github.com/InsertKoinIO/koin"),
        License("Kotlin", "kotlinlang", APACHE_2, "https://github.com/Kotlin/"),
        License(
            "material-intro",
            "heinrichreimer",
            MIT,
            "https://github.com/heinrichreimer/material-intro"
        ),
        License("Moshi", "square", APACHE_2, "https://github.com/square/moshi"),
        License(
            "subsampling-scale-image-view",
            "davemorrissey",
            APACHE_2,
            "https://github.com/davemorrissey/subsampling-scale-image-view"
        ),
        License("Timber", "JakeWharton", APACHE_2, "https://github.com/JakeWharton/timber"),
        License("Wear-Msger", "Chenhe", MIT, "https://github.com/ichenhe/Wear-Msger"),
    )

    private var _uiState = mutableStateOf(LicenseUiState(licenses = licenses))
    val uiState: State<LicenseUiState> = _uiState

    data class License(
        val name: String,
        val author: String,
        val type: String,
        val url: String
    )
}