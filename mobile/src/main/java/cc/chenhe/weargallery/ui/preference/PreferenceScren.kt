package cc.chenhe.weargallery.ui.preference

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.*
import cc.chenhe.weargallery.ui.theme.ContentAlpha
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme
import cc.chenhe.weargallery.utils.NOTIFY_CHANNEL_ID_FOREGROUND_SERVICE
import org.koin.androidx.compose.getViewModel

@Composable
fun PreferenceScreen(
    navUp: () -> Unit,
    navToAbout: () -> Unit,
    viewModel: PreferenceViewModel = getViewModel()
) {
    val uiState by viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val ob = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.sendIntent(PreferenceIntent.RecheckNotificationState)
            }
        }
        lifecycleOwner.lifecycle.addObserver(ob)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(ob)
        }
    }
    PreferenceScreenFrame(
        navUp,
        navToAbout,
        uiState = uiState,
        onIntent = { viewModel.sendIntent(it) },
    )
}

@Composable
@Preview
private fun PreferenceScreenFramePreview() {
    WearGalleryTheme {
        PreferenceScreenFrame(
            navUp = {},
            navToAbout = {},
            uiState = PreferenceUiState(),
            onIntent = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceScreenFrame(
    navUp: () -> Unit,
    navToAbout: () -> Unit,
    uiState: PreferenceUiState,
    onIntent: (PreferenceIntent) -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = navUp) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.navup)
                    )
                }
            },
            title = { Text(text = stringResource(id = R.string.pref_title)) },
        )
    }) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            RegularPreferenceGroup(
                tipOnWatchOperating = uiState.tipWhenWatchOperating,
                tipOnWatchOperatingChange = { onIntent(PreferenceIntent.SetTipWhenWatchOperating(it)) },
                foregroundService = uiState.foregroundService,
                foregroundServiceChange = { onIntent(PreferenceIntent.SetForegroundService(it)) },
                foregroundNotification = uiState.foregroundServiceNotification,
            )
            Spacer(modifier = Modifier.height(16.dp))
            val ctx = LocalContext.current
            InfoPreferenceGroup(
                versionText = stringResource(
                    R.string.pref_version, getVersionName(ctx), getVersionCode(ctx),
                ),
                onCheckUpdate = {
                    openMarket(ctx) {
                        openWithBrowser(ctx, GITHUB)
                    }
                },
                navToAbout = navToAbout,
            )
        }
    }
}

@Composable
private fun RegularPreferenceGroup(
    tipOnWatchOperating: Boolean,
    tipOnWatchOperatingChange: (Boolean) -> Unit,
    foregroundService: Boolean,
    foregroundServiceChange: (Boolean) -> Unit,
    foregroundNotification: Boolean,
) {
    Card(modifier = Modifier.animateContentSize()) {
        PreferenceItem(
            icon = Icons.Rounded.Watch,
            title = stringResource(id = R.string.pref_phone_show_tips_about_watch),
            button = { Switch(checked = tipOnWatchOperating, onCheckedChange = null) },
            onClick = { tipOnWatchOperatingChange(!tipOnWatchOperating) }
        )
        Divider(modifier = Modifier.padding(horizontal = 48.dp))
        PreferenceItem(
            icon = Icons.Rounded.PlayCircle,
            title = stringResource(id = R.string.pref_foreground_service),
            message = stringResource(id = R.string.pref_foreground_service_summary),
            button = { Switch(checked = foregroundService, onCheckedChange = null) },
            onClick = { foregroundServiceChange(!foregroundService) }
        )
        if (foregroundService) {
            Divider(modifier = Modifier.padding(horizontal = 48.dp))
            val ctx = LocalContext.current
            PreferenceItem(
                icon = null,
                title = stringResource(id = R.string.pref_foreground_service_notification),
                button = { Switch(checked = foregroundNotification, onCheckedChange = null) },
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ctx.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            putExtra(
                                Settings.EXTRA_CHANNEL_ID,
                                NOTIFY_CHANNEL_ID_FOREGROUND_SERVICE
                            )
                        })
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoPreferenceGroup(
    versionText: String,
    onCheckUpdate: () -> Unit,
    navToAbout: () -> Unit,
) {
    Card {
        PreferenceItem(
            icon = Icons.Rounded.Upgrade,
            title = stringResource(id = R.string.setting_check_update),
            message = versionText,
            onClick = onCheckUpdate,
        )
        Divider(modifier = Modifier.padding(horizontal = 48.dp))
        PreferenceItem(
            icon = Icons.Rounded.Info,
            title = stringResource(id = R.string.pref_about),
            onClick = navToAbout,
        )
    }
}

@Composable
private fun PreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    message: String? = null,
    button: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
        } else {
            Spacer(modifier = Modifier.size(32.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            message?.also { msg ->
                Text(
                    text = msg,
                    color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
                )
            }
        }
        if (button != null) {
            Spacer(modifier = Modifier.width(8.dp))
            button()
        }
    }
}