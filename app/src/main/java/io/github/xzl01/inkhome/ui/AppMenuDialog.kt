package io.github.xzl01.inkhome.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.ui.theme.InkAccent
import io.github.xzl01.inkhome.ui.theme.InkBlack

/** 长按应用后的操作菜单。 */
@Composable
fun AppMenuDialog(
    app: AppEntry,
    showRemoveFromHome: Boolean,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onAddToHome: (() -> Unit)?,
    onRemoveFromHome: (() -> Unit)?,
    onCreateFolder: (() -> Unit)? = null,
    onCustomIcon: (() -> Unit)? = null,
    onResetIcon: (() -> Unit)? = null,
    onMoveToFolder: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = app.label, color = InkBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                MenuItem("应用信息") { onDismiss(); onAppInfo() }
                MenuItem("卸载") { onDismiss(); onUninstall() }
                if (onAddToHome != null) {
                    MenuItem("添加到桌面") { onDismiss(); onAddToHome() }
                }
                if (onCreateFolder != null) {
                    MenuItem("新建文件夹") { onDismiss(); onCreateFolder() }
                }
                if (onMoveToFolder != null) {
                    MenuItem("移动到文件夹") { onDismiss(); onMoveToFolder() }
                }
                if (onCustomIcon != null) {
                    MenuItem("自定义图标") { onDismiss(); onCustomIcon() }
                }
                if (onResetIcon != null) {
                    MenuItem("恢复默认图标") { onDismiss(); onResetIcon() }
                }
                if (showRemoveFromHome && onRemoveFromHome != null) {
                    MenuItem("从桌面移除") { onDismiss(); onRemoveFromHome() }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = InkBlack,
        fontSize = 17.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .padding(horizontal = 2.dp)
    )
}

fun appInfoIntent(app: AppEntry): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))

fun uninstallIntent(app: AppEntry): Intent =
    Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
