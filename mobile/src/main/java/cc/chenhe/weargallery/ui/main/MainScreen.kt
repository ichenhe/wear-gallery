@file:OptIn(ExperimentalMaterial3Api::class)

package cc.chenhe.weargallery.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.*
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme
import cc.chenhe.weargallery.utils.runIf
import org.koin.androidx.compose.getViewModel

@Composable
fun MainScreen(
    navToPreferences: () -> Unit,
    navToLegacy: () -> Unit,
    oneColumnLayout: Boolean,
    widthLooseLayout: Boolean,
    model: MainScreenViewModel = getViewModel()
) {
    val uiState by model.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val ob = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                model.sendUiIntent(MainUiIntent.RecheckPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(ob)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(ob)
        }
    }
    MainScreenFrame(
        oneColumnLayout = oneColumnLayout,
        uiState = uiState,
        navToPreferences = navToPreferences,
        navToLegacy = navToLegacy,
        onUiIntent = { model.sendUiIntent(it) },
        widthLooseLayout = widthLooseLayout,
    )
}

@Composable
@Preview
fun MainFramePreview() {
    WearGalleryTheme {
        val uiState = MainUiState(
            showTicTip = true,
            updateInfo = MainScreenViewModel.UpdateInfo(GITHUB, "v6.5"),
            lackingNecessaryPermissions = listOf("android.permission.READ_EXTERNAL_STORAGE"),
        )
        MainScreenFrame(uiState = uiState, oneColumnLayout = true, widthLooseLayout = false)
    }
}

@Composable
private fun MainScreenFrame(
    oneColumnLayout: Boolean,
    widthLooseLayout: Boolean,
    uiState: MainUiState,
    navToPreferences: () -> Unit = {},
    navToLegacy: () -> Unit = {},
    onUiIntent: (MainUiIntent) -> Unit = {},
) {
    Scaffold { contentPadding ->
        if (oneColumnLayout) {
            OneColumnLayout(
                navToPreferences = navToPreferences,
                navToLegacy = navToLegacy,
                uiState = uiState,
                onUiIntent = onUiIntent,
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxWidth(),
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TwoColumnsLayout(
                    navToPreferences = navToPreferences,
                    navToLegacy = navToLegacy,
                    uiState = uiState,
                    onUiIntent = onUiIntent,
                    modifier = Modifier
                        .padding(horizontal = if (widthLooseLayout) 128.dp else 32.dp)
                        .fillMaxHeight(),
                    looseLayout = widthLooseLayout,
                )
            }
        }
    }
}

@Composable
private fun OneColumnLayout(
    navToPreferences: () -> Unit,
    navToLegacy: () -> Unit,
    uiState: MainUiState,
    onUiIntent: (MainUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Header()
        Spacer(modifier = Modifier.height(32.dp))
        MessageCards(
            uiState.updateInfo,
            showTicTip = uiState.showTicTip,
            lackingPermissions = uiState.lackingNecessaryPermissions,
            recheckPermissions = { onUiIntent(MainUiIntent.RecheckPermissions) },
            modifier = Modifier
                .padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Buttons(navToPreferences, navToLegacy)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TwoColumnsLayout(
    navToPreferences: () -> Unit,
    navToLegacy: () -> Unit,
    uiState: MainUiState,
    onUiIntent: (MainUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    looseLayout: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        // first column
        Column(
            modifier = Modifier
                .runIf(looseLayout) {
                    weight(0.25f)
                }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header()
            Spacer(modifier = Modifier.height(16.dp))
            Buttons(navToPreferences, navToLegacy)
        }
        Spacer(modifier = Modifier.width(32.dp))
        // second column
        Box(modifier = Modifier
            .runIf(looseLayout) {
                weight(0.75f)
            }
            .verticalScroll(rememberScrollState())) {
            MessageCards(
                uiState.updateInfo,
                showTicTip = uiState.showTicTip,
                lackingPermissions = uiState.lackingNecessaryPermissions,
                recheckPermissions = { onUiIntent(MainUiIntent.RecheckPermissions) },
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
    }
}

/**
 * 各种提示卡片
 */
@Composable
private fun MessageCards(
    updateInfo: MainScreenViewModel.UpdateInfo?,
    showTicTip: Boolean,
    lackingPermissions: List<String>,
    recheckPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .animateContentSize()
            .then(modifier)
    ) {
        val ctx = LocalContext.current

        // permission problem
        if (lackingPermissions.isNotEmpty()) {
            val permissionRequestLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                onResult = {
                    val alwaysDenied = it.any { item ->
                        !item.value && ctx.getActivity()?.isAlwaysDenied(item.key) == true
                    }
                    if (alwaysDenied) {
                        ctx.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", ctx.packageName, null)
                            )
                        )
                    } else {
                        recheckPermissions()
                    }
                }
            )
            MessageCard(
                errorStyle = true,
                icon = Icons.Rounded.Error,
                title = stringResource(id = R.string.main_card_permission_title),
                message = stringResource(id = R.string.main_card_permission_message),
                positiveButtonText = stringResource(id = R.string.permission_grant),
                onPositiveButtonClick = {
                    permissionRequestLauncher.launch(lackingPermissions.toTypedArray())
                }
            )
        }

        // new app version
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
                title = stringResource(id = R.string.main_card_tic),
                message = stringResource(id = R.string.main_card_tic_msg),
                modifier = Modifier.fillMaxWidth(),
                expandable = true,
                initialExpanded = true,
            )
        }
        // tips
        MessageCard(
            icon = Icons.Rounded.TipsAndUpdates,
            title = stringResource(id = R.string.main_card_intro),
            message = stringResource(id = R.string.main_card_intro_msg),
            modifier = Modifier.fillMaxWidth(),
            expandable = true,
            initialExpanded = true,
        )
        // install on watch
        MessageCard(
            icon = Icons.Rounded.Watch,
            title = stringResource(id = R.string.main_card_install_to_watch_title),
            message = stringResource(id = R.string.main_card_install_to_watch_message),
            modifier = Modifier.fillMaxWidth(),
            expandable = true,
        )
        // auto run
        MessageCard(
            icon = Icons.Rounded.PlayCircle,
            title = stringResource(id = R.string.main_card_auto_run_title),
            message = stringResource(id = R.string.main_card_auto_run_message),
            modifier = Modifier.fillMaxWidth(),
            expandable = true,
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
    errorStyle: Boolean = false,
    expandable: Boolean = false,
    initialExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val containerColor = if (errorStyle) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val titleColor = if (errorStyle) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val content = @Composable {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CompositionLocalProvider(LocalContentColor provides titleColor) {
                        Icon(icon, contentDescription = null)
                        Text(
                            text = title,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                if (expandable) {
                    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
            if (!expandable || expanded) {
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

    if (expandable) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            onClick = { expanded = !expanded },
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            content()
        }
    }
}