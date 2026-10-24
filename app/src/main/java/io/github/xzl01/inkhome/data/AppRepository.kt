package io.github.xzl01.inkhome.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/** 读取设备上所有可启动应用（Launcher Activity）。 */
class AppRepository(private val context: Context) {

    fun loadApps(): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching {
            pm.queryIntentActivities(intent, 0)
                .asSequence()
                .mapNotNull { resolve ->
                    val ai = resolve.activityInfo ?: return@mapNotNull null
                    if (ai.packageName == context.packageName) return@mapNotNull null
                    AppEntry(
                        key = "${ai.packageName}/${ai.name}",
                        label = ai.loadLabel(pm)?.toString()?.ifBlank { ai.packageName } ?: ai.packageName,
                        packageName = ai.packageName,
                        activityName = ai.name,
                        isSystem = (ai.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                    )
                }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
                .toList()
        }.getOrDefault(emptyList())
    }

    fun loadIcon(entry: AppEntry): Drawable? =
        runCatching {
            context.packageManager.getActivityIcon(ComponentName(entry.packageName, entry.activityName))
        }.getOrNull()

    fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(entry.packageName, entry.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(intent) }
    }
}
