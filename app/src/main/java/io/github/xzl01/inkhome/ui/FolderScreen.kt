package io.github.xzl01.inkhome.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.AppRepository
import io.github.xzl01.inkhome.data.FolderData
import io.github.xzl01.inkhome.ui.theme.InkBlack
import io.github.xzl01.inkhome.ui.theme.InkGray
import io.github.xzl01.inkhome.ui.theme.InkLine
import io.github.xzl01.inkhome.ui.theme.InkWhite

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderScreen(
    folder: FolderData,
    appsByKey: Map<String, AppEntry>,
    repo: AppRepository,
    colorIcons: Boolean,
    onBack: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onAddApp: () -> Unit,
    onRename: () -> Unit,
    onDissolve: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit
) {
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    val apps = folder.apps.mapNotNull { appsByKey[it] }

    Column(Modifier.fillMaxSize().background(InkWhite)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = InkBlack,
                fontSize = 30.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 16.dp)
            )
            Text(
                text = folder.name,
                color = InkBlack,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "重命名",
                color = InkGray,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(onClick = onRename)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            Text(
                text = "解散",
                color = InkGray,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(onClick = onDissolve)
                    .padding(8.dp)
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apps, key = { it.key }) { app ->
                Column(
                    modifier = Modifier
                        .border(1.dp, InkLine)
                        .combinedClickable(
                            onClick = { onLaunch(app) },
                            onLongClick = { menuApp = app }
                        )
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(entry = app, repo = repo, size = 48.dp, color = colorIcons)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = app.label,
                        color = InkBlack,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .border(1.dp, InkLine)
                        .clickable(onClick = onAddApp)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("+", color = InkGray, fontSize = 36.sp, fontWeight = FontWeight.Light)
                    Text("添加应用", color = InkGray, fontSize = 12.sp)
                }
            }
        }
    }

    menuApp?.let { app ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(app.label, color = InkBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    FolderMenuItem("移出文件夹") {
                        menuApp = null
                        onRemoveApp(app.key)
                    }
                    FolderMenuItem("应用信息") {
                        menuApp = null
                        onAppInfo(app)
                    }
                    FolderMenuItem("卸载") {
                        menuApp = null
                        onUninstall(app)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
private fun FolderMenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = InkBlack,
        fontSize = 16.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}
