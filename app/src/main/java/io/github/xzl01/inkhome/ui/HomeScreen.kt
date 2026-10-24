package io.github.xzl01.inkhome.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.FolderData
import io.github.xzl01.inkhome.data.AppRepository
import io.github.xzl01.inkhome.ui.theme.DISPLAY
import io.github.xzl01.inkhome.ui.theme.InkAccent
import io.github.xzl01.inkhome.ui.theme.InkBlack
import io.github.xzl01.inkhome.ui.theme.InkGray
import io.github.xzl01.inkhome.ui.theme.InkGreen
import io.github.xzl01.inkhome.ui.theme.InkLine
import io.github.xzl01.inkhome.ui.theme.InkRed
import io.github.xzl01.inkhome.ui.theme.InkWhite
import io.github.xzl01.inkhome.ui.theme.Serif
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    appsByKey: Map<String, AppEntry>,
    slots: List<String?>,
    slotCount: Int,
    colorIcons: Boolean,
    iconSizeDp: Int,
    folders: Map<String, FolderData>,
    motto: String,
    repo: AppRepository,
    onAddSlot: (Int) -> Unit,
    onOpenFolder: (String) -> Unit,
    onLongPressFolder: (String) -> Unit,
    onLongPressSlot: (Int, AppEntry) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(20_000)
        }
    }
    var battery by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                battery = level * 100 / scale
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    var wifi by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        while (true) {
            wifi = runCatching {
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }.getOrDefault(false)
            delay(5_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkWhite)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = InkBlack,
                    fontFamily = DISPLAY,
                    fontWeight = FontWeight.Black,
                    fontSize = 64.sp,
                    letterSpacing = (-1).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                        color = InkGray,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.padding(horizontal = 5.dp))
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)),
                        color = InkGray,
                        fontSize = 15.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (battery >= 0) "$battery%" else "--",
                    color = when {
                        charging -> InkGreen
                        battery in 0..20 -> InkRed
                        else -> InkBlack
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (wifi) "WLAN" else "离线",
                    color = if (wifi) InkAccent else InkGray,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = motto,
            color = InkBlack,
            fontFamily = Serif,
            fontSize = 30.sp,
            lineHeight = 46.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(22.dp))

        val rows = (slotCount / 4).coerceAtLeast(1)
        val cellHeight = (iconSizeDp + 64).dp
        for (row in 0 until rows) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(cellHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0 until 4) {
                    val index = row * 4 + col
                    val key = slots.getOrNull(index)
                    val folder = FolderData.idFromSlot(key)?.let { folders[it] }
                    val app = key?.let { appsByKey[it] }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, InkLine)
                            .combinedClickable(
                                onClick = {
                                    when {
                                        folder != null -> onOpenFolder(folder.id)
                                        app != null -> onLaunch(app)
                                        else -> onAddSlot(index)
                                    }
                                },
                                onLongClick = {
                                    when {
                                        folder != null -> onLongPressFolder(folder.id)
                                        app != null -> onLongPressSlot(index, app)
                                        else -> onAddSlot(index)
                                    }
                                }
                            )
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            folder != null -> {
                                FolderGlyph(
                                    folder = folder,
                                    appsByKey = appsByKey,
                                    repo = repo,
                                    colorIcons = colorIcons,
                                    iconSizeDp = iconSizeDp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = folder.name,
                                    color = InkBlack,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            app != null -> {
                                AppIcon(entry = app, repo = repo, size = iconSizeDp.dp, color = colorIcons)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = app.label,
                                    color = InkBlack,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            else -> {
                                Text(
                                    text = "+",
                                    color = InkGray,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Text(text = "添加应用", color = InkGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NavButton("应用", Modifier.weight(1f), onOpenDrawer)
            NavButton("工具", Modifier.weight(1f), onOpenTools)
            NavButton("设置", Modifier.weight(1f), onOpenSettings)
        }
    }
}

@Composable
private fun NavButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .border(1.5.dp, InkBlack, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = InkBlack, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun FolderGlyph(
    folder: FolderData,
    appsByKey: Map<String, AppEntry>,
    repo: AppRepository,
    colorIcons: Boolean,
    iconSizeDp: Int
) {
    val mini = (iconSizeDp * 0.44f).dp
    val items = folder.apps.mapNotNull { appsByKey[it] }.take(4)
    Column {
        for (row in 0 until 2) {
            Row {
                for (col in 0 until 2) {
                    val index = row * 2 + col
                    val app = items.getOrNull(index)
                    Box(
                        modifier = Modifier.padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app != null) {
                            AppIcon(entry = app, repo = repo, size = mini, color = colorIcons)
                        } else {
                            Box(Modifier.height(mini).fillMaxWidth(0.5f))
                        }
                    }
                }
            }
        }
    }
}
