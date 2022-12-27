package cc.chenhe.weargallery.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cc.chenhe.weargallery.R
import cc.chenhe.weargallery.ui.theme.WearGalleryTheme

@Composable
@Preview
fun PermissionRequestCardPreview() {
    WearGalleryTheme() {
        PermissionRequestCard(
            icon = Icons.Rounded.Person,
            title = "通知栏",
            message = "开启通知栏权限",
            onConfirm = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 展示需要的权限的卡片。
 * @param onConfirm 点击授权按钮的回调
 * @param necessary 是否是必须的权限，必须的权限使用错误颜色显示。
 */
@Composable
fun PermissionRequestCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    necessary: Boolean = false
) {
    val bgColor = if (necessary)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (necessary)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = contentColor),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp)
            )
            // 按钮
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                ) {
                    Text(text = stringResource(id = R.string.permission_grant))
                }
            }
        }
    }
}