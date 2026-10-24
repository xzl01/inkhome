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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.data.AppEntry
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
    motto: String,
    repo: AppRepository,
    onAddSlot: (Int) -> Unit,
    onLongPressSlot: (Int, AppEntry) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
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
                battery = (level * 100 / scale)
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
                    Spacer(Modifier.width(10.dp))
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

        Spacer(Modifier.height(28.dp))
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
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.weight(1f))

        val rows = (slotCount / 4).coerceAtLeast(1)
        for (row in 0 until rows) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0 until 4) {
                    val index = row * 4 + col
                    val key = slots.getOrNull(index)
                    val app = key?.let { appsByKey[it] }
                    val cellModifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                    Column(
                        modifier = cellModifier
                            .border(1.dp, InkLine)
                            .combinedClickable(
                                onClick = {
                                    if (app != null) onLaunch(app) else onAddSlot(index)
                                },
                                onLongClick = {
                                    if (app != null) onLongPressSlot(index, app)
                                }
                            )
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app != null) {
                            AppIcon(entry = app, repo = repo, size = 54.dp, color = colorIcons)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = app.label,
                                color = InkBlack,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "+",
                                color = InkGray,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.height(80.dp)
                            )
                            Text(text = "添加应用", color = InkGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NavButton("应用", Modifier.weight(1f), onOpenDrawer)
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
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = InkBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** 桌面上没有条目时用于占位的透明块。 */
@Composable
fun EmptyCell(size: Int) {
    Box(Modifier.size(size.dp))
}
