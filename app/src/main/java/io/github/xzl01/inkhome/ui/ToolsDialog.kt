package io.github.xzl01.inkhome.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.ui.theme.InkBlack

@Composable
fun ToolsDialog(
    onDismiss: () -> Unit,
    onCleanBackground: () -> Unit,
    onLockScreen: () -> Unit,
    onFlashRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("工具", color = InkBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ToolItem("清理后台") { onDismiss(); onCleanBackground() }
                ToolItem("锁屏") { onDismiss(); onLockScreen() }
                ToolItem("刷屏清残影") { onDismiss(); onFlashRefresh() }
                ToolItem("设置") { onDismiss(); onSettings() }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun ToolItem(text: String, onClick: () -> Unit) {
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
