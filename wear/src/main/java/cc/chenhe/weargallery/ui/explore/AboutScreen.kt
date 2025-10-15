package cc.chenhe.weargallery.ui.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.tooling.preview.devices.WearDevices
import cc.chenhe.weargallery.R
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun AboutScreen() {
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding()
    ScreenScaffold(
        scrollState = columnState,
        contentPadding = contentPadding,
    ) { contentPadding ->
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.app_name))
            Text(
                stringResource(R.string.about_github),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.wear.compose.material.MaterialTheme.colors.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.about_telegram),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.wear.compose.material.MaterialTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
@Preview(device = WearDevices.SMALL_ROUND, backgroundColor = 0xFF000000, showBackground = true)
private fun AboutScreenPreview() {
    AboutScreen()
}