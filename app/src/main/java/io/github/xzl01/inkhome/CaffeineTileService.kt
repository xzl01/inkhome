package io.github.xzl01.inkhome

import io.github.xzl01.inkhome.prefs.Prefs

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/** 快捷设置磁贴：咖啡因 / 保持亮屏。 */
@RequiresApi(Build.VERSION_CODES.N)
class CaffeineTileService : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        val context = applicationContext
        if (!Prefs(context).caffeine() && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivityAndCollapse(intent) }
            return
        }
        val enable = !Prefs(context).caffeine()
        Prefs(context).setCaffeine(enable)
        if (enable) {
            startForegroundService(Intent(context, CaffeineService::class.java))
        } else {
            stopService(Intent(context, CaffeineService::class.java))
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (Prefs(this).caffeine()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "咖啡因"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_caffeine)
        tile.updateTile()
    }
}
