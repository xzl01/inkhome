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

    fun customIcon(appKey: String): String? =
        sp.getString("icon_$appKey", null)?.takeIf { java.io.File(it).exists() }

    fun setCustomIcon(appKey: String, path: String?) {
        sp.edit().apply {
            if (path.isNullOrBlank()) remove("icon_$appKey") else putString("icon_$appKey", path)
        }.apply()
    }

    fun exportJson(): String {
        val root = JSONObject()
        val slotsArray = JSONArray()
        repeat(MAX_SLOTS) { slotsArray.put(slot(it) ?: "") }
        root.put("slots", slotsArray)
        root.put("slot_count", slotCount())
        root.put("color_icons", colorIcons())
        root.put("hide_system", hideSystemApps())
        root.put("icon_size", iconSizeDp())
        root.put("motto", motto())
        val foldersArray = JSONArray()
        folders().values.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            obj.put("apps", JSONArray(folder.apps))
            foldersArray.put(obj)
        }
        root.put("folders", foldersArray)
        return root.toString()
    }

    /** 恢复备份；成功返回 true。自定义图标文件不包含在备份里。 */
    fun importJson(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        val slotsArray = root.optJSONArray("slots")
        for (i in 0 until MAX_SLOTS) {
            val value = slotsArray?.optString(i, "")?.takeIf { it.isNotBlank() }
            setSlot(i, value)
        }
        root.optInt("slot_count", 8).let { setSlotCount(it) }
        root.optBoolean("color_icons", true).let { setColorIcons(it) }
        root.optBoolean("hide_system", true).let { setHideSystemApps(it) }
        root.optInt("icon_size", 54).let { setIconSizeDp(it) }
        root.optString("motto", "").let { if (it.isNotBlank()) setMotto(it) }
        val foldersArray = root.optJSONArray("folders") ?: JSONArray()
        val restored = LinkedHashMap<String, FolderData>()
        for (i in 0 until foldersArray.length()) {
            val obj = foldersArray.getJSONObject(i)
            val appsArray = obj.optJSONArray("apps") ?: JSONArray()
            val apps = (0 until appsArray.length()).map { appsArray.getString(it) }
            val folder = FolderData(
                id = obj.getString("id"),
                name = obj.optString("name", "文件夹"),
                apps = apps
            )
            restored[folder.id] = folder
        }
        saveFolders(restored)
        sp.edit().putBoolean("seeded", true).apply()
        true
    }.getOrDefault(false)

    fun caffeine(): Boolean = sp.getBoolean("caffeine", false)

    fun setCaffeine(value: Boolean) {
        sp.edit().putBoolean("caffeine", value).apply()
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
