package cc.chenhe.weargallery.ui.preference

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.chenhe.weargallery.ui.theme.ContentAlpha
import org.koin.androidx.compose.getViewModel

@Composable
fun LicenseScreen(navUp: () -> Unit, model: LicenseViewModel = getViewModel()) {
    val uiState by model.uiState
    LicenseScreenFrame(navUp, uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseScreenFrame(navUp: () -> Unit, uiState: LicenseUiState) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navUp) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            stringResource(id = cc.chenhe.weargallery.R.string.navup),
                        )
                    }
                },
                title = { Text(stringResource(id = cc.chenhe.weargallery.R.string.about_licence)) },
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(uiState.licenses.size) { index ->
                    LicenseItem(uiState.licenses[index])
                }
            }
        }
    }
}

@Composable
private fun LicenseItem(license: LicenseViewModel.License) {
    val ctx = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                ctx.openInBrowser(license.url)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            Text(text = license.name + "-" + license.author)
            CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(alpha = ContentAlpha.medium)) {
                Text(text = license.type)
                Text(text = license.url)
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