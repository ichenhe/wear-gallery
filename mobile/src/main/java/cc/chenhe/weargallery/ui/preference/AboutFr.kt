package cc.chenhe.weargallery.ui.preference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme

class AboutFr : Fragment() {
    private val navController by lazy { findNavController() }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WearGalleryTheme {
                val windowSizeClass = calculateWindowSizeClass(activity = requireActivity())
                AboutScreen(
                    navUp = { navController.navigateUp() },
                    navToLicenses = {
                        navController.navigate(AboutFrDirections.actionAboutFrToLicenseFr())
                    },
                    oneColumnLayout = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
                            || windowSizeClass.heightSizeClass == WindowHeightSizeClass.Expanded
                )
            }
        }
    }
}