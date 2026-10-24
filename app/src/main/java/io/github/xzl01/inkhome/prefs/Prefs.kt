package io.github.xzl01.inkhome.prefs

import android.content.Context
import io.github.xzl01.inkhome.data.FolderData
import org.json.JSONArray
import org.json.JSONObject

/** 极简 SharedPreferences 封装。 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("inkhome", Context.MODE_PRIVATE)

    fun slot(index: Int): String? = sp.getString("slot_$index", null)?.takeIf { it.isNotBlank() }

    fun setSlot(index: Int, key: String?) {
        sp.edit().apply {
            if (key.isNullOrBlank()) remove("slot_$index") else putString("slot_$index", key)
        }.apply()
    }

    fun slotCount(): Int = normalize(sp.getInt("slot_count", 8))

    fun setSlotCount(count: Int) {
        sp.edit().putInt("slot_count", normalize(count)).apply()
    }

    fun iconSizeDp(): Int = sp.getInt("icon_size", 54).coerceIn(46, 62)

    fun setIconSizeDp(value: Int) {
        sp.edit().putInt("icon_size", value).apply()
    }

    fun colorIcons(): Boolean = sp.getBoolean("color_icons", true)

    fun setColorIcons(value: Boolean) {
        sp.edit().putBoolean("color_icons", value).apply()
    }

    fun hideSystemApps(): Boolean = sp.getBoolean("hide_system", true)

    fun setHideSystemApps(value: Boolean) {
        sp.edit().putBoolean("hide_system", value).apply()
    }

    fun folders(): Map<String, FolderData> {
        val raw = sp.getString("folders", null) ?: return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.getJSONObject(index)
                val appsJson = obj.optJSONArray("apps") ?: JSONArray()
                val apps = (0 until appsJson.length()).map { appsJson.getString(it) }
                FolderData(
                    id = obj.getString("id"),
                    name = obj.optString("name", "文件夹"),
                    apps = apps
                )
            }.associateBy { it.id }
        }.getOrDefault(emptyMap())
    }

    fun saveFolders(folders: Map<String, FolderData>) {
        val array = JSONArray()
        folders.values.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            obj.put("apps", JSONArray(folder.apps))
            array.put(obj)
        }
        sp.edit().putString("folders", array.toString()).apply()
    }

    fun seeded(): Boolean = sp.getBoolean("seeded", false)

    fun setSeeded(value: Boolean) {
        sp.edit().putBoolean("seeded", value).apply()
    }

    fun motto(): String = sp.getString("motto", DEFAULT_MOTTO)?.ifBlank { DEFAULT_MOTTO } ?: DEFAULT_MOTTO

    fun setMotto(value: String) {
        val text = value.trim()
        sp.edit().apply {
            if (text.isBlank()) remove("motto") else putString("motto", text)
        }.apply()
    }

    companion object {
        const val DEFAULT_MOTTO = "书读百遍，其义自见。\n一页有声，世界便慢。"
        val SLOT_OPTIONS = listOf(4, 8, 12, 16)
        val ICON_SIZE_OPTIONS = listOf(46, 54, 62)
        const val MAX_SLOTS = 16
        fun normalize(raw: Int): Int = SLOT_OPTIONS.minByOrNull { kotlin.math.abs(it - raw) } ?: 8
    }
}
