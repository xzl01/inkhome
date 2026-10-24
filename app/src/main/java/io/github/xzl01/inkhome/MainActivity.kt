package io.github.xzl01.inkhome

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
    val slots = remember {
        mutableStateListOf<String?>().apply {
            repeat(Prefs.MAX_SLOTS) { add(prefs.slot(it)) }
        }
    }
    val scope = rememberCoroutineScope()

    fun refreshApps() {
        scope.launch {
            allApps = withContext(Dispatchers.IO) { repo.loadApps() }
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
                onMottoSave = { value ->
                    prefs.setMotto(value)
                    motto = prefs.motto()
                },
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
            onDismiss = { showTools = false },
            onCleanBackground = { cleanBackground() },
            onLockScreen = { lockScreen() },
            onFlashRefresh = { flashCounter++ },
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
            }
        )
    }
}
