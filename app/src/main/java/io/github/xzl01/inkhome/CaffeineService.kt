package io.github.xzl01.inkhome

import io.github.xzl01.inkhome.prefs.Prefs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 咖啡因（保持亮屏）：用一个 1x1 透明悬浮窗 + FLAG_KEEP_SCREEN_ON
 * 让当前页面（任意 App）不自动锁屏/息屏。需要「显示在其他应用上层」权限。
 */
class CaffeineService : Service() {
    private var overlay: View? = null
    private var wm: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFY_ID, buildNotification())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WindowManager::class.java)
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        val v = View(this)
        runCatching { wm?.addView(v, params) }
        overlay = v
        Prefs(this).setCaffeine(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OFF) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlay?.let { runCatching { wm?.removeView(it) } }
        overlay = null
        Prefs(this).setCaffeine(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "咖啡因", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val off = PendingIntent.getBroadcast(
            this, 1, Intent(this, CaffeineToggleReceiver::class.java).setAction(ACTION_OFF),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("咖啡因已开启")
            .setContentText("屏幕保持常亮，点此关闭")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "关闭", off).build())
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL = "caffeine"
        const val NOTIFY_ID = 2001
        const val ACTION_OFF = "io.github.xzl01.inkhome.CAFFEINE_OFF"
    }
}
