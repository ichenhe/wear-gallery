package cc.chenhe.weargallery.ui.preference

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.common.util.*
import cc.chenhe.weargallery.ui.theme.ContentAlpha
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme

@Composable
fun AboutScreen(navUp: () -> Unit, navToLicenses: () -> Unit, oneColumnLayout: Boolean) {
    AboutScreenFrame(navUp, navToLicenses, oneColumnLayout)
}

@Composable
@Preview
private fun AboutScreenPreview() {
    WearGalleryTheme {
        AboutScreenFrame(navUp = {}, navToLicenses = {}, oneColumnLayout = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreenFrame(
    navUp: () -> Unit,
    navToLicenses: () -> Unit,
    oneColumnLayout: Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navUp) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.navup)
                        )
                    }
                },
                title = {}
            )
        }
    ) { paddingValues ->
        if (oneColumnLayout) {
            OneColumnLayout(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                navToLicenses = navToLicenses,
            )
        } else {
            TwoColumnLayout(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                navToLicenses = navToLicenses,
            )
        }
    }
}

@Composable
private fun OneColumnLayout(
    modifier: Modifier = Modifier, navToLicenses: () -> Unit,
) {
    Column(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Header()
            Spacer(modifier = Modifier.height(64.dp))
            Channel()
        }
        OpenSourceLicenses(
            navToLicenses = navToLicenses,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun TwoColumnLayout(modifier: Modifier, navToLicenses: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Info
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Header()
            }

            Spacer(modifier = Modifier.width(64.dp))

            // buttons
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Channel()
                    Spacer(modifier = Modifier.height(16.dp))
                    OpenSourceLicenses(navToLicenses = navToLicenses)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier) {
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
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.about_slogan),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        val ctx = LocalContext.current
        val version =
            stringResource(id = R.string.pref_version, getVersionName(ctx), getVersionCode(ctx))
        SelectionContainer {
            Text(
                text = version,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
                color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
            )
        }
    }
}

@Composable
private fun Channel() {
    val ctx = LocalContext.current
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyMedium
            .copy(textDecoration = TextDecoration.Underline)
    ) {
        SelectionContainer {
            Column {
                ChannelItem(
                    icon = rememberVectorPainter(Icons.Rounded.Public),
                    text = WEBSITE,
                    onClick = { ctx.openInBrowser(WEBSITE) },
                )
                ChannelItem(
                    icon = painterResource(id = R.drawable.ic_about_tg),
                    text = stringResource(id = R.string.about_tg),
                    onClick = { ctx.openInBrowser(TELEGRAM) },
                )
                ChannelItem(
                    icon = painterResource(id = R.drawable.ic_about_github),
                    text = stringResource(id = R.string.about_github),
                    onClick = { ctx.openInBrowser(GITHUB) },
                )
            }

        }
    }
}

private fun Context.openInBrowser(url: String) {
    try {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (_: Exception) {
    }
}

@Composable
private fun ChannelItem(icon: Painter, text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp)
            )
            Text(text = text)
        }
    }
}

@Composable
private fun OpenSourceLicenses(navToLicenses: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = navToLicenses, modifier = modifier) {
        Text(
            text = stringResource(id = R.string.about_licence),
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}