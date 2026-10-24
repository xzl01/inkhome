package io.github.xzl01.inkhome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.AppRepository
import io.github.xzl01.inkhome.prefs.Prefs
import io.github.xzl01.inkhome.ui.AppMenuDialog
import io.github.xzl01.inkhome.ui.AppPickerScreen
import io.github.xzl01.inkhome.ui.DrawerScreen
import io.github.xzl01.inkhome.ui.HomeScreen
import io.github.xzl01.inkhome.ui.SettingsScreen
import io.github.xzl01.inkhome.ui.appInfoIntent
import io.github.xzl01.inkhome.ui.theme.InkHomeTheme
import io.github.xzl01.inkhome.ui.uninstallIntent
import kotlinx.coroutines.Dispatchers
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
        // 从任何 App 按 HOME 回到桌面时，强制回到主屏（而不是停留在抽屉/设置）
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
    var motto by remember { mutableStateOf(prefs.motto()) }
    var pickIndex by remember { mutableIntStateOf(-1) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var menuHomeIndex by remember { mutableStateOf<Int?>(null) }
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

    LaunchedEffect(Unit) { refreshApps() }

    // 每次从别的 App 回到桌面：刷新应用列表 + 回到主屏
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            refreshApps()
            screen = "home"
        }
    }

    // 从设置/安装器返回时刷新应用列表
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshApps()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次启动：用前 8 个非系统应用填充桌面
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

    val visibleApps = remember(allApps, hideSystem) {
        if (hideSystem) allApps.filter { !it.isSystem } else allApps
    }
    val appsByKey = remember(allApps) { allApps.associateBy { it.key } }

    BackHandler {
        if (screen != "home") screen = "home"
    }

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
            onBack = { screen = "home" },
            onPick = { app ->
                if (pickIndex in 0 until Prefs.MAX_SLOTS) {
                    prefs.setSlot(pickIndex, app.key)
                    slots[pickIndex] = app.key
                }
                screen = "home"
            }
        )

        "settings" -> SettingsScreen(
            colorIcons = colorIcons,
            hideSystemApps = hideSystem,
            slotCount = slotCount,
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
            motto = motto,
            repo = repo,
            onAddSlot = { index ->
                pickIndex = index
                refreshApps()
                screen = "picker"
            },
            onLongPressSlot = { index, app ->
                menuApp = app
                menuHomeIndex = index
            },
            onLaunch = { repo.launch(it) },
            onOpenDrawer = {
                refreshApps()
                screen = "drawer"
            },
            onOpenSettings = { screen = "settings" }
        )
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
            }
        )
    }
}
