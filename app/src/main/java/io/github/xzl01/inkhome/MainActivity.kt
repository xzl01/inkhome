package io.github.xzl01.inkhome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.AppRepository
import io.github.xzl01.inkhome.prefs.Prefs
import io.github.xzl01.inkhome.ui.AppPickerScreen
import io.github.xzl01.inkhome.ui.DrawerScreen
import io.github.xzl01.inkhome.ui.HomeScreen
import io.github.xzl01.inkhome.ui.SettingsScreen
import io.github.xzl01.inkhome.ui.theme.InkHomeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val repo = AppRepository(this)
        setContent {
            InkHomeTheme {
                InkHomeRoot(prefs, repo)
            }
        }
    }
}

@Composable
private fun InkHomeRoot(prefs: Prefs, repo: AppRepository) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var colorIcons by remember { mutableStateOf(prefs.colorIcons()) }
    var hideSystem by remember { mutableStateOf(prefs.hideSystemApps()) }
    var slotCount by remember { mutableStateOf(prefs.slotCount()) }
    var motto by remember { mutableStateOf(prefs.motto()) }
    var pickIndex by remember { mutableIntStateOf(-1) }
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

    val visibleApps = remember(allApps, hideSystem) {
        if (hideSystem) allApps.filter { !it.isSystem } else allApps
    }
    val appsByKey = remember(allApps) { allApps.associateBy { it.key } }

    BackHandler(enabled = screen != "home") { screen = "home" }

    when (screen) {
        "drawer" -> DrawerScreen(
            apps = visibleApps,
            repo = repo,
            colorIcons = colorIcons,
            onBack = { screen = "home" },
            onLaunch = { repo.launch(it) }
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
            onRemoveSlot = { index ->
                prefs.setSlot(index, null)
                slots[index] = null
            },
            onLaunch = { repo.launch(it) },
            onOpenDrawer = {
                refreshApps()
                screen = "drawer"
            },
            onOpenSettings = { screen = "settings" }
        )
    }
}
