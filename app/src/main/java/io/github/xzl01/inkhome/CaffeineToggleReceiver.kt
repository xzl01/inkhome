package io.github.xzl01.inkhome

import io.github.xzl01.inkhome.prefs.Prefs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CaffeineToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CaffeineService.ACTION_OFF) {
            Prefs(context).setCaffeine(false)
            context.stopService(Intent(context, CaffeineService::class.java))
        }
    }
}
