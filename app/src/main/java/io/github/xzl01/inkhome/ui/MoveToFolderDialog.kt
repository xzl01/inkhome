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
import io.github.xzl01.inkhome.ui.theme.InkGray

@Composable
fun MoveToFolderDialog(
    folders: Map<String, FolderData>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到文件夹", color = InkBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                folders.values.sortedBy { it.name }.forEach { folder ->
                    Text(
                        text = folder.name,
                        color = InkBlack,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .clickable { onPick(folder.id) }
                            .padding(vertical = 10.dp)
                    )
                }
                if (folders.isEmpty()) {
                    Text("还没有文件夹", color = InkGray, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
