package io.github.xzl01.inkhome

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.AppRepository
import io.github.xzl01.inkhome.data.FolderData
import io.github.xzl01.inkhome.prefs.Prefs
import io.github.xzl01.inkhome.ui.AppMenuDialog
import io.github.xzl01.inkhome.ui.AppPickerScreen
import io.github.xzl01.inkhome.ui.DrawerScreen
import io.github.xzl01.inkhome.ui.FolderMenuDialog
import io.github.xzl01.inkhome.ui.FolderScreen
import io.github.xzl01.inkhome.ui.HomeScreen
import io.github.xzl01.inkhome.ui.IconCache
import io.github.xzl01.inkhome.ui.MoveToFolderDialog
import io.github.xzl01.inkhome.ui.SettingsScreen
import io.github.xzl01.inkhome.ui.ToolsDialog
import io.github.xzl01.inkhome.ui.appInfoIntent
import io.github.xzl01.inkhome.ui.theme.InkBlack
import io.github.xzl01.inkhome.ui.theme.InkHomeTheme
import io.github.xzl01.inkhome.ui.uninstallIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val homeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val repo = AppRepository(this)
        setContent {
            InkHomeTheme {
                InkHomeRoot(prefs, repo, homeSignal.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.intValue++
    }
}

@Composable
private fun InkHomeRoot(prefs: Prefs, repo: AppRepository, homeSignal: Int) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf("home") }
    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var colorIcons by remember { mutableStateOf(prefs.colorIcons()) }
    var hideSystem by remember { mutableStateOf(prefs.hideSystemApps()) }
    var slotCount by remember { mutableStateOf(prefs.slotCount()) }
    var iconSizeDp by remember { mutableStateOf(prefs.iconSizeDp()) }
    var caffeine by remember { mutableStateOf(prefs.caffeine()) }
    var motto by remember { mutableStateOf(prefs.motto()) }
    var pickIndex by remember { mutableIntStateOf(-1) }
    var pickFolderId by remember { mutableStateOf<String?>(null) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var menuHomeIndex by remember { mutableStateOf<Int?>(null) }
    var showTools by remember { mutableStateOf(false) }
    var flashCounter by remember { mutableIntStateOf(0) }
    var flashColor by remember { mutableStateOf<Color?>(null) }
    var folders by remember { mutableStateOf(prefs.folders()) }
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    var folderMenuId by remember { mutableStateOf<String?>(null) }
    var renameFolderId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var pendingIconApp by remember { mutableStateOf<AppEntry?>(null) }
    var moveApp by remember { mutableStateOf<AppEntry?>(null) }
    var moveFromIndex by remember { mutableStateOf<Int?>(null) }
    val slots = remember {
        mutableStateListOf<String?>().apply {
            repeat(Prefs.MAX_SLOTS) { add(prefs.slot(it)) }
        }
    }
    val scope = rememberCoroutineScope()

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val app = pendingIconApp
        pendingIconApp = null
        if (uri != null && app != null) {
            runCatching {
                val dir = File(context.filesDir, "icons").apply { mkdirs() }
                val outFile = File(dir, app.key.replace('/', '_') + ".png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                prefs.setCustomIcon(app.key, outFile.absolutePath)
                IconCache.invalidate(app.key)
                scope.launch { allApps = withContext(Dispatchers.IO) { repo.loadApps() } }
                Toast.makeText(context, "已设置自定义图标", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "设置图标失败：$it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshApps() {
        scope.launch {
            allApps = withContext(Dispatchers.IO) { repo.loadApps() }
        }
    }

    fun reloadFromPrefs() {
        colorIcons = prefs.colorIcons()
        hideSystem = prefs.hideSystemApps()
        slotCount = prefs.slotCount()
        iconSizeDp = prefs.iconSizeDp()
        motto = prefs.motto()
        folders = prefs.folders()
        for (i in slots.indices) slots[i] = prefs.slot(i)
        refreshApps()
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(prefs.exportJson().toByteArray())
                }
                Toast.makeText(context, "桌面配置已备份", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "备份失败：$it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (text.isNotBlank() && prefs.importJson(text)) {
                    reloadFromPrefs()
                    Toast.makeText(context, "已恢复桌面配置", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "配置文件无效", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "恢复失败：$it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveFolders(newFolders: Map<String, FolderData>) {
        folders = newFolders
        prefs.saveFolders(newFolders)
    }

    fun dissolveFolder(folder: FolderData) {
        val next = slots.toMutableList()
        folder.apps.forEach { key ->
            val index = next.indexOfFirst { it == null }
            if (index >= 0) next[index] = key
        }
        for (i in next.indices) {
            if (next[i] == FolderData.slotValue(folder.id)) next[i] = null
            prefs.setSlot(i, next[i])
            slots[i] = next[i]
        }
        saveFolders(folders - folder.id)
        if (currentFolderId == folder.id) {
            currentFolderId = null
            screen = "home"
        }
        folderMenuId = null
    }

    LaunchedEffect(Unit) { refreshApps() }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            refreshApps()
            screen = "home"
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshApps()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(allApps) {
        if (allApps.isNotEmpty() && !prefs.seeded()) {
            allApps.filter { !it.isSystem }.take(Prefs.MAX_SLOTS).forEachIndexed { index, app ->
                if (slots[index] == null) {
                    prefs.setSlot(index, app.key)
                    slots[index] = app.key
                }
            }
            prefs.setSeeded(true)
        }
    }

    LaunchedEffect(flashCounter) {
        if (flashCounter > 0) {
            repeat(3) {
                flashColor = Color.Black
                delay(160)
                flashColor = Color.White
                delay(160)
            }
            flashColor = null
        }
    }

    fun setCaffeine(enable: Boolean) {
        if (enable && !Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "请允许「显示在其他应用上层」后再开启", Toast.LENGTH_SHORT).show()
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        prefs.setCaffeine(enable)
        caffeine = enable
        if (enable) {
            context.startForegroundService(Intent(context, CaffeineService::class.java))
            Toast.makeText(context, "咖啡因已开启：屏幕保持常亮", Toast.LENGTH_SHORT).show()
        } else {
            context.stopService(Intent(context, CaffeineService::class.java))
            Toast.makeText(context, "咖啡因已关闭", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (prefs.caffeine() && Settings.canDrawOverlays(context)) {
            context.startForegroundService(Intent(context, CaffeineService::class.java))
        }
    }

    fun cleanBackground() {
        val am = context.getSystemService(ActivityManager::class.java)
        var killed = 0
        allApps.filter { !it.isSystem }.map { it.packageName }.distinct().forEach { pkg ->
            if (pkg != context.packageName) {
                runCatching {
                    am.killBackgroundProcesses(pkg)
                    killed++
                }
            }
        }
        Toast.makeText(context, "已请求清理 $killed 个后台应用", Toast.LENGTH_SHORT).show()
    }

    fun lockScreen() {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, InkAdminReceiver::class.java)
        val active = runCatching { dpm.isAdminActive(admin) }.getOrDefault(false)
        if (active) {
            runCatching { dpm.lockNow() }
        } else {
            runCatching {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "用于墨桌桌面上的「锁屏」按钮"
                    )
                context.startActivity(intent)
            }
        }
    }

    val visibleApps = remember(allApps, hideSystem) {
        if (hideSystem) allApps.filter { !it.isSystem } else allApps
    }
    val appsByKey = remember(allApps) { allApps.associateBy { it.key } }

    BackHandler {
        if (screen != "home") {
            if (screen == "folder") currentFolderId = null
            screen = "home"
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            "drawer" -> DrawerScreen(
                apps = visibleApps,
                repo = repo,
                colorIcons = colorIcons,
                onBack = { screen = "home" },
                onLaunch = { repo.launch(it) },
                onLongPress = { app ->
                    menuApp = app
                    menuHomeIndex = null
                }
            )

            "picker" -> AppPickerScreen(
                apps = visibleApps,
                repo = repo,
                colorIcons = colorIcons,
                onBack = { screen = "home"; pickFolderId = null },
                onPick = { app ->
                    val targetFolder = pickFolderId
                    if (targetFolder != null) {
                        val folder = folders[targetFolder]
                        if (folder != null) {
                            saveFolders(
                                folders + (folder.id to folder.copy(apps = (folder.apps + app.key).distinct()))
                            )
                            currentFolderId = folder.id
                            screen = "folder"
                        } else {
                            screen = "home"
                        }
                    } else if (pickIndex in 0 until Prefs.MAX_SLOTS) {
                        prefs.setSlot(pickIndex, app.key)
                        slots[pickIndex] = app.key
                        screen = "home"
                    }
                    pickFolderId = null
                }
            )

            "folder" -> {
                val folder = currentFolderId?.let { folders[it] }
                if (folder == null) {
                    LaunchedEffect(Unit) { screen = "home" }
                } else {
                    FolderScreen(
                        folder = folder,
                        appsByKey = appsByKey,
                        repo = repo,
                        colorIcons = colorIcons,
                        onBack = {
                            currentFolderId = null
                            screen = "home"
                        },
                        onLaunch = { repo.launch(it) },
                        onAddApp = {
                            pickFolderId = folder.id
                            refreshApps()
                            screen = "picker"
                        },
                        onRename = {
                            renameFolderId = folder.id
                            renameText = folder.name
                        },
                        onDissolve = { dissolveFolder(folder) },
                        onRemoveApp = { key ->
                            saveFolders(
                                folders + (folder.id to folder.copy(apps = folder.apps - key))
                            )
                        },
                        onAppInfo = { app -> runCatching { context.startActivity(appInfoIntent(app)) } },
                        onUninstall = { app -> runCatching { context.startActivity(uninstallIntent(app)) } }
                    )
                }
            }

            "settings" -> SettingsScreen(
                colorIcons = colorIcons,
                hideSystemApps = hideSystem,
                slotCount = slotCount,
                iconSizeDp = iconSizeDp,
                caffeine = caffeine,
                motto = motto,
                onToggleColorIcons = { value ->
                    colorIcons = value
                    prefs.setColorIcons(value)
                },
                onToggleHideSystem = { value ->
                    hideSystem = value
                    prefs.setHideSystemApps(value)
                },
                onSlotCount = { value ->
                    slotCount = value
                    prefs.setSlotCount(value)
                },
                onIconSize = { value ->
                    iconSizeDp = value
                    prefs.setIconSizeDp(value)
                },
                onSetCaffeine = { enable -> setCaffeine(enable) },
                onMottoSave = { value ->
                    prefs.setMotto(value)
                    motto = prefs.motto()
                },
                onBackup = { backupLauncher.launch("inkhome-backup.json") },
                onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/*")) },
                onBack = { screen = "home" }
            )

            else -> HomeScreen(
                appsByKey = appsByKey,
                slots = slots,
                slotCount = slotCount,
                colorIcons = colorIcons,
                iconSizeDp = iconSizeDp,
                folders = folders,
                motto = motto,
                repo = repo,
                onAddSlot = { index ->
                    pickIndex = index
                    pickFolderId = null
                    refreshApps()
                    screen = "picker"
                },
                onOpenFolder = { id ->
                    currentFolderId = id
                    screen = "folder"
                },
                onLongPressFolder = { id -> folderMenuId = id },
                onLongPressSlot = { index, app ->
                    menuApp = app
                    menuHomeIndex = index
                },
                onLaunch = { repo.launch(it) },
                onOpenDrawer = {
                    refreshApps()
                    screen = "drawer"
                },
                onOpenTools = { showTools = true },
                onOpenSettings = { screen = "settings" }
            )
        }

        flashColor?.let { color ->
            Box(Modifier.fillMaxSize().background(color))
        }
    }

    if (showTools) {
        ToolsDialog(
            caffeine = caffeine,
            onDismiss = { showTools = false },
            onCleanBackground = { cleanBackground() },
            onLockScreen = { lockScreen() },
            onFlashRefresh = { flashCounter++ },
            onToggleCaffeine = { setCaffeine(!caffeine) },
            onSettings = { screen = "settings" }
        )
    }

    folderMenuId?.let { id ->
        folders[id]?.let { folder ->
            FolderMenuDialog(
                folder = folder,
                onDismiss = { folderMenuId = null },
                onOpen = {
                    currentFolderId = id
                    screen = "folder"
                },
                onRename = {
                    renameFolderId = id
                    renameText = folder.name
                },
                onDissolve = { dissolveFolder(folder) }
            )
        }
    }

    renameFolderId?.let { id ->
        val folder = folders[id]
        if (folder != null) {
            AlertDialog(
                onDismissRequest = { renameFolderId = null },
                title = { Text("重命名文件夹") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = InkBlack, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = renameText.trim().ifBlank { "文件夹" }
                        saveFolders(folders + (id to folder.copy(name = name)))
                        currentFolderId = id
                        renameFolderId = null
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { renameFolderId = null }) { Text("取消") }
                }
            )
        }
    }

    menuApp?.let { app ->
        val fromHome = menuHomeIndex
        AppMenuDialog(
            app = app,
            showRemoveFromHome = fromHome != null,
            onDismiss = {
                menuApp = null
                menuHomeIndex = null
            },
            onAppInfo = {
                runCatching { context.startActivity(appInfoIntent(app)) }
            },
            onUninstall = {
                runCatching { context.startActivity(uninstallIntent(app)) }
            },
            onAddToHome = if (fromHome == null) {
                {
                    val target = slots.indexOfFirst { it == null }
                    if (target >= 0) {
                        prefs.setSlot(target, app.key)
                        slots[target] = app.key
                    }
                }
            } else null,
            onRemoveFromHome = fromHome?.let {
                {
                    prefs.setSlot(it, null)
                    slots[it] = null
                }
            },
            onCreateFolder = fromHome?.let { slotIndex ->
                {
                    val id = System.currentTimeMillis().toString()
                    val folder = FolderData(id = id, name = "文件夹", apps = listOf(app.key))
                    saveFolders(folders + (id to folder))
                    prefs.setSlot(slotIndex, FolderData.slotValue(id))
                    slots[slotIndex] = FolderData.slotValue(id)
                }
            },
            onCustomIcon = {
                pendingIconApp = app
                iconPicker.launch(arrayOf("image/*"))
            },
            onResetIcon = if (app.customIconPath != null) {
                {
                    prefs.setCustomIcon(app.key, null)
                    IconCache.invalidate(app.key)
                    refreshApps()
                }
            } else null,
            onMoveToFolder = if (fromHome != null && folders.isNotEmpty()) {
                {
                    moveApp = app
                    moveFromIndex = fromHome
                }
            } else null
        )
    }

    moveApp?.let { app ->
        MoveToFolderDialog(
            folders = folders,
            onDismiss = { moveApp = null; moveFromIndex = null },
            onPick = { folderId ->
                val folder = folders[folderId]
                if (folder != null) {
                    saveFolders(
                        folders + (folder.id to folder.copy(apps = (folder.apps + app.key).distinct()))
                    )
                }
                moveFromIndex?.let { index ->
                    prefs.setSlot(index, null)
                    slots[index] = null
                }
                moveApp = null
                moveFromIndex = null
            }
        )
    }
}
