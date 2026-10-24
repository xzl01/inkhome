package io.github.xzl01.inkhome.prefs

import android.content.Context

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

    fun colorIcons(): Boolean = sp.getBoolean("color_icons", true)

    fun setColorIcons(value: Boolean) {
        sp.edit().putBoolean("color_icons", value).apply()
    }

    fun hideSystemApps(): Boolean = sp.getBoolean("hide_system", true)

    fun setHideSystemApps(value: Boolean) {
        sp.edit().putBoolean("hide_system", value).apply()
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
        const val MAX_SLOTS = 16
        fun normalize(raw: Int): Int = SLOT_OPTIONS.minByOrNull { kotlin.math.abs(it - raw) } ?: 8
    }
}
