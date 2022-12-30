package cc.chenhe.weargallery.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cc.chenhe.weargallery.ui.legacy.LegacyAty
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme
import cc.chenhe.weargallery.utils.shouldUseOneColumnLayout

class MainFr : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MainContent()
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    private fun MainContent() {
        WearGalleryTheme {
            val windowSizeClass = calculateWindowSizeClass(activity = requireActivity())
            MainScreen(
                navToPreferences = {
                    findNavController().navigate(MainFrDirections.actionMainFrToPreferenceFr())
                },
                navToLegacy = {
                    startActivity(Intent(requireContext(), LegacyAty::class.java))
                },
                oneColumnLayout = windowSizeClass.shouldUseOneColumnLayout(),
                widthLooseLayout = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
            )
        }
    }
}