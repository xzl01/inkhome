package io.github.xzl01.inkhome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xzl01.inkhome.prefs.Prefs
import io.github.xzl01.inkhome.ui.theme.InkAccent
import io.github.xzl01.inkhome.ui.theme.InkBlack
import io.github.xzl01.inkhome.ui.theme.InkGray
import io.github.xzl01.inkhome.ui.theme.InkLine
import io.github.xzl01.inkhome.ui.theme.InkWhite

@Composable
fun SettingsScreen(
    colorIcons: Boolean,
    hideSystemApps: Boolean,
    slotCount: Int,
    iconSizeDp: Int,
    motto: String,
    onToggleColorIcons: (Boolean) -> Unit,
    onToggleHideSystem: (Boolean) -> Unit,
    onSlotCount: (Int) -> Unit,
    onIconSize: (Int) -> Unit,
    onMottoSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var mottoText by remember { mutableStateOf(motto) }
    LaunchedEffect(motto) { mottoText = motto }

    Column(
        Modifier
            .fillMaxSize()
            .background(InkWhite)
            .verticalScroll(rememberScrollState())
    ) {
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
                text = "设置",
                color = InkBlack,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
        }

        SettingRow("图标颜色") {
            Choice("彩色", colorIcons) { onToggleColorIcons(true) }
            Spacer(Modifier.width(10.dp))
            Choice("黑白", !colorIcons) { onToggleColorIcons(false) }
        }

        SettingRow("桌面格子") {
            Prefs.SLOT_OPTIONS.forEach { option ->
                Spacer(Modifier.width(8.dp))
                Choice("$option", slotCount == option) { onSlotCount(option) }
            }
        }

        SettingRow("图标大小") {
            Prefs.ICON_SIZE_OPTIONS.forEach { option ->
                Spacer(Modifier.width(8.dp))
                val label = when (option) {
                    46 -> "小"
                    54 -> "中"
                    else -> "大"
                }
                Choice(label, iconSizeDp == option) { onIconSize(option) }
            }
        }

        SettingRow("应用抽屉") {
            Choice("隐藏系统应用", hideSystemApps) { onToggleHideSystem(true) }
            Spacer(Modifier.width(10.dp))
            Choice("显示全部", !hideSystemApps) { onToggleHideSystem(false) }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("格言", color = InkBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = mottoText,
                onValueChange = { mottoText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = InkBlack,
                    fontSize = 16.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            Spacer(Modifier.height(8.dp))
            Choice("保存格言", false) { onMottoSave(mottoText) }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "墨桌 v0.1.0 · GPL-3.0 · 为彩色墨水屏重写",
            color = InkGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(title: String, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = InkBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) InkWhite else InkBlack
    val bg = if (selected) InkAccent else Color.Transparent
    Text(
        text = text,
        color = fg,
        fontSize = 14.sp,
        modifier = Modifier
            .border(1.5.dp, if (selected) InkAccent else InkLine)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
