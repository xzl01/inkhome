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
import io.github.xzl01.inkhome.data.FolderData
import io.github.xzl01.inkhome.ui.theme.InkBlack

@Composable
fun FolderMenuDialog(
    folder: FolderData,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDissolve: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name, color = InkBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                FolderAction("打开") { onDismiss(); onOpen() }
                FolderAction("重命名") { onDismiss(); onRename() }
                FolderAction("解散文件夹") { onDismiss(); onDissolve() }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun FolderAction(text: String, onClick: () -> Unit) {
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
