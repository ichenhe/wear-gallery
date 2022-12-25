@file:OptIn(ExperimentalMaterial3Api::class)

package cc.chenhe.weargallery.ui.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.GITHUB
import cc.chenhe.weargallery.common.util.openMarket
import cc.chenhe.weargallery.common.util.openWithBrowser
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme
import org.koin.androidx.compose.getViewModel

@Composable
fun MainScreen(
    navToPreferences: () -> Unit,
    navToLegacy: () -> Unit,
    model: MainScreenViewModel = getViewModel()
) {

    val uiState by model.uiState
    MainScreenFrame(
        uiState = uiState,
        navToPreferences = navToPreferences,
        navToLegacy = navToLegacy,
    )
}

@Composable
@Preview
fun MainFramePreview() {
    WearGalleryTheme {
        val uiState = MainUiState(
            showTicTip = true,
            updateInfo = MainScreenViewModel.UpdateInfo(GITHUB, "v6.5"),
        )
        MainScreenFrame(uiState = uiState)
    }
}

@Composable
private fun MainScreenFrame(
    uiState: MainUiState,
    navToPreferences: () -> Unit = {},
    navToLegacy: () -> Unit = {},
) {
    Scaffold { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            ResourcesCompat.getDrawable(
                LocalContext.current.resources,
                R.mipmap.ic_launcher_round, LocalContext.current.theme
            )?.let { drawable ->
                Image(
                    drawable.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            MessageCards(uiState.updateInfo, showTicTip = uiState.showTicTip)
            Spacer(modifier = Modifier.height(16.dp))
            Buttons(navToPreferences, navToLegacy)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 各种提示卡片
 */
@Composable
private fun MessageCards(updateInfo: MainScreenViewModel.UpdateInfo?, showTicTip: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .animateContentSize()
    ) {
        val ctx = LocalContext.current
        updateInfo?.also { info ->
            MessageCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.ArrowCircleUp,
                title = stringResource(id = R.string.card_update),
                message = stringResource(id = R.string.card_update_msg, info.versionName),
                positiveButtonText = stringResource(id = R.string.card_update_open_appstore),
                onPositiveButtonClick = {
                    openMarket(ctx) {
                        openWithBrowser(ctx, info.url)
                    }
                },
                negativeButtonText = stringResource(id = R.string.card_update_open_url),
                onNegativeButtonClick = {
                    openWithBrowser(ctx, info.url)
                }
            )
        }

        if (showTicTip) {
            MessageCard(
                icon = Icons.Rounded.Error,
                title = stringResource(id = R.string.card_tic),
                message = stringResource(id = R.string.card_tic_msg),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MessageCard(
            icon = Icons.Rounded.TipsAndUpdates,
            title = stringResource(id = R.string.card_intro),
            message = stringResource(id = R.string.card_intro_msg),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Buttons(navToPreferences: () -> Unit, navToLegacy: () -> Unit) {
    Button(onClick = navToPreferences) {
        Text(text = stringResource(id = R.string.main_preference))
    }
    TextButton(onClick = navToLegacy) {
        Text(text = stringResource(id = R.string.main_legacy))
    }
}

@Composable
@Preview
private fun MessageCardPreview() {
    WearGalleryTheme {
        MessageCard(
            Icons.Rounded.TipsAndUpdates,
            "Android",
            "Jetpack Compose",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MessageCard(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    positiveButtonText: String? = null,
    onPositiveButtonClick: () -> Unit = {},
    negativeButtonText: String? = null,
    onNegativeButtonClick: () -> Unit = {},
) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val titleColor = MaterialTheme.colorScheme.secondary
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentColor provides titleColor) {
                    Icon(icon, contentDescription = null)
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message)
            if (negativeButtonText != null || positiveButtonText != null) {
                Row(modifier = Modifier.align(Alignment.End)) {
                    negativeButtonText?.also { negative ->
                        TextButton(onClick = onNegativeButtonClick) {
                            Text(text = negative)
                        }
                    }
                    positiveButtonText?.also { positive ->
                        TextButton(onClick = onPositiveButtonClick) {
                            Text(text = positive)
                        }
                    }
                }
            }
        }
    }
}