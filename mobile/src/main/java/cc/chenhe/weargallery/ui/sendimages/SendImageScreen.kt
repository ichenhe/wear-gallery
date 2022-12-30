package cc.chenhe.weargallery.ui.sendimages

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.bean.Image
import cc.chenhe.weargallery.common.util.getActivity
import cc.chenhe.weargallery.common.util.isAlwaysDenied
import cc.chenhe.weargallery.ui.common.PermissionRequestCard
import cc.chenhe.weargallery.ui.theme.ContentAlpha
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SendImageScreen(
    napUp: () -> Unit,
    intent: Intent,
    model: SendImagesViewModel = getViewModel { parametersOf(intent) },
    oneColumnLayout: Boolean,
) {
    val state by model.uiState
    SendImageScreenFrame(
        state,
        oneColumnLayout = oneColumnLayout,
        navUp = napUp,
        sendIntent = model::sendIntent,
        targetFolderValidator = SendImagesViewModel::isFolderPathValid,
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val ob = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                model.sendIntent(SendImagesIntent.CheckNotificationPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(ob)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(ob)
        }
    }
}

@Composable
@Preview
private fun SendImageScreenFramePreview() {
    WearGalleryTheme {
        SendImagesUiState(targetFolder = "custom").also { uiState ->
            SendImageScreenFrame(uiState = uiState, oneColumnLayout = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SendImageScreenFrame(
    uiState: SendImagesUiState,
    oneColumnLayout: Boolean,
    navUp: () -> Unit = {},
    sendIntent: (SendImagesIntent) -> Unit = {},
    targetFolderValidator: (targetFolder: String?) -> Boolean = { true },
) {
    var showSelectTargetTipDialog by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(stringResource(id = R.string.send_images_title))

                val imageCountStr = pluralStringResource(
                    id = R.plurals.send_images_subtitle,
                    count = uiState.images.size,
                    uiState.images.size
                )
                Text(imageCountStr, style = MaterialTheme.typography.bodyLarge)
            }
        },
            navigationIcon = {
                IconButton(onClick = navUp) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.navup)
                    )
                }
            })
    }, floatingActionButton = {
        FloatingActionButton(onClick = {
            // start send
            if (uiState.targetDevice != null) {
                sendIntent(SendImagesIntent.StartSend)
                navUp()
            } else {
                showSelectTargetTipDialog = true
            }
        }) {
            Icon(
                Icons.Rounded.Send,
                contentDescription = stringResource(R.string.share_image_label)
            )
        }
    }) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            if (oneColumnLayout) {
                OneColumnLayout(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    uiState = uiState,
                    sendIntent = sendIntent,
                    targetFolderValidator = targetFolderValidator,
                )
            } else {
                TwoColumnsLayout(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    uiState = uiState,
                    sendIntent = sendIntent,
                    targetFolderValidator = targetFolderValidator,
                )
            }
        }

        if (showSelectTargetTipDialog) {
            AlertDialog(
                onDismissRequest = { showSelectTargetTipDialog = false },
                title = { Text(text = stringResource(id = R.string.tip)) },
                text = { Text(text = stringResource(id = R.string.send_images_select_device)) },
                confirmButton = {
                    TextButton(onClick = { showSelectTargetTipDialog = false }) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                },
            )
        }
    }
}

@Composable
private fun OneColumnLayout(
    modifier: Modifier = Modifier,
    uiState: SendImagesUiState,
    sendIntent: (SendImagesIntent) -> Unit,
    targetFolderValidator: (targetFolder: String?) -> Boolean,
) {
    Column(modifier = modifier) {
        Options(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            targetFolder = uiState.targetFolder,
            targetDevice = uiState.targetDevice,
            devices = uiState.devices,
            onTargetFolderChange = { sendIntent(SendImagesIntent.SelectTargetFolder(it)) },
            onTargetDeviceChange = { sendIntent(SendImagesIntent.SetTargetDevice(it)) },
            targetFolderValidator = targetFolderValidator,
        )
        AnimatedVisibility(
            visible = uiState.notificationPermission != SendImagesUiState.PermissionState.Granted
        ) {
            NotificationPermissionCard(
                uiState.notificationPermission,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        ImageList(uiState.images, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun TwoColumnsLayout(
    modifier: Modifier = Modifier,
    uiState: SendImagesUiState,
    sendIntent: (SendImagesIntent) -> Unit,
    targetFolderValidator: (targetFolder: String?) -> Boolean,
) {
    Row(modifier = modifier) {
        // first column
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(0.5f)
                .padding(horizontal = 16.dp),
        ) {
            AnimatedVisibility(
                visible = uiState.notificationPermission != SendImagesUiState.PermissionState.Granted
            ) {
                NotificationPermissionCard(
                    uiState.notificationPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            Options(
                modifier = Modifier.fillMaxWidth(),
                targetFolder = uiState.targetFolder,
                targetDevice = uiState.targetDevice,
                devices = uiState.devices,
                onTargetFolderChange = { sendIntent(SendImagesIntent.SelectTargetFolder(it)) },
                onTargetDeviceChange = { sendIntent(SendImagesIntent.SetTargetDevice(it)) },
                targetFolderValidator = targetFolderValidator,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        // second column
        ImageList(
            uiState.images,
            modifier = Modifier.weight(0.5f),
        )
    }
}

@Composable
private fun NotificationPermissionCard(
    notificationPermissionState: SendImagesUiState.PermissionState,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    // no need to recheck permission in activity result
    // we do it in lifecycle event
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ctx.getActivity()
                    ?.isAlwaysDenied(Manifest.permission.POST_NOTIFICATIONS) == true
            ) {
                // post notification permission is always denied
                ctx.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    }
                )
            }
        }
    PermissionRequestCard(
        modifier = modifier,
        icon = Icons.Rounded.Notifications,
        title = stringResource(R.string.send_images_permission_card_notification_title),
        message = stringResource(R.string.send_images_permission_card_notification_message),
        onConfirm = {
            requestNotificationPermission(
                context = ctx,
                state = notificationPermissionState,
                requestPostNotificationPermission = {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                startActivity = { ctx.startActivity(it) })
        })
}

private fun requestNotificationPermission(
    context: Context,
    state: SendImagesUiState.PermissionState,
    requestPostNotificationPermission: () -> Unit,
    startActivity: (Intent) -> Unit,
) {
    when (state) {
        SendImagesUiState.PermissionState.Granted -> {}
        SendImagesUiState.PermissionState.NotGranted -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPostNotificationPermission()
        }
        SendImagesUiState.PermissionState.Disabled -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }.also {
                startActivity(it)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }.also {
                startActivity(it)
            }
        }
        is SendImagesUiState.PermissionState.ChannelDisabled -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, state.channelId)
            }.also {
                startActivity(it)
            }
        }
    }
}

@Composable
private fun Options(
    targetFolder: String?,
    targetDevice: SendImagesViewModel.NodeInfo?,
    devices: List<SendImagesViewModel.NodeInfo>,
    onTargetFolderChange: (newFolder: String) -> Unit,
    targetFolderValidator: (targetFolder: String?) -> Boolean,
    onTargetDeviceChange: (newDevice: SendImagesViewModel.NodeInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TargetDeviceOption(targetDevice, devices, onTargetDeviceChange)
            TargetFolderOption(
                targetFolder = targetFolder,
                onTargetFolderChange = onTargetFolderChange,
                validator = targetFolderValidator,
            )
        }
    }
}

@Composable
private fun TargetDeviceOption(
    targetDevice: SendImagesViewModel.NodeInfo?,
    devices: List<SendImagesViewModel.NodeInfo>,
    onTargetDeviceChange: (newDevice: SendImagesViewModel.NodeInfo?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSelectDeviceDialog by remember { mutableStateOf(false) }
    PropertyItemFrame(modifier = modifier,
        icon = Icons.Rounded.Watch,
        title = stringResource(id = R.string.send_images_device),
        onClick = { showSelectDeviceDialog = true }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = targetDevice?.name ?: "",
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.Edit, contentDescription = null)
        }
    }
    if (showSelectDeviceDialog) {
        @Composable
        fun DeviceItem(device: SendImagesViewModel.NodeInfo) {
            val selected = device.id == targetDevice?.id
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        onTargetDeviceChange(device)
                        showSelectDeviceDialog = false
                    }
                    .padding(16.dp)) {
                RadioButton(selected = selected, onClick = null)
                Text(
                    text = device.name,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        AlertDialog(onDismissRequest = { showSelectDeviceDialog = false },
            title = { Text(stringResource(id = R.string.send_images_device)) },
            text = {
                CompositionLocalProvider(LocalIndication provides rememberRipple()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (devices.isEmpty()) {
                            item(contentType = "header") {
                                Text(text = stringResource(id = R.string.send_images_device_none))
                            }
                        }
                        items(devices.size) { index ->
                            DeviceItem(device = devices[index])
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectDeviceDialog = false }) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun TargetFolderOption(
    modifier: Modifier = Modifier,
    targetFolder: String?,
    onTargetFolderChange: (String) -> Unit,
    validator: (targetFolder: String?) -> Boolean,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    PropertyItemFrame(
        modifier = modifier,
        icon = Icons.Rounded.Folder,
        title = stringResource(id = R.string.send_images_directory),
        onClick = { showEditDialog = true },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                targetFolder ?: stringResource(id = R.string.send_images_directory_default),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.Edit, contentDescription = null)
        }
    }

    if (showEditDialog) {
        var value by remember { mutableStateOf(targetFolder ?: "") }
        val error = !validator(value)
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = true),
            title = { Text(text = stringResource(R.string.send_images_directory)) },
            text = {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = stringResource(R.string.send_images_directory_description))
                    TextField(
                        value = value,
                        onValueChange = { s -> value = s },
                        singleLine = true,
                        isError = error,
                        label = {
                            if (error) {
                                Text(text = stringResource(R.string.send_images_directory_invalid))
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !error,
                    onClick = {
                        showEditDialog = false
                        onTargetFolderChange(value)
                    },
                ) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
        )
    }
}

@Composable
private fun PropertyItemFrame(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    value: @Composable ColumnScope.() -> Unit = {},
) {
    CompositionLocalProvider(LocalIndication provides rememberRipple()) {
        Box(modifier = modifier.clip(MaterialTheme.shapes.medium).let {
            if (onClick != null) it.clickable(onClick = onClick) else it
        }) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(title, color = LocalContentColor.current.copy(alpha = ContentAlpha.medium))
                    value()
                }
            }
        }
    }
}

@Composable
private fun ImageList(images: List<Image>, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val reqBuilder = remember {
        ImageRequest.Builder(ctx).transitionFactory(CrossfadeTransition.Factory())
    }
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(images) { image ->
            var showPlaceholder by remember { mutableStateOf(true) }
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
            ) {
                AsyncImage(
                    model = reqBuilder.data(image.uri).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            showPlaceholder = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .placeholder(
                            visible = showPlaceholder, highlight = PlaceholderHighlight.shimmer()
                        ),
                )
            }
        }
    }
}