package io.github.xzl01.inkhome.data

/** 一个可启动应用的稳定标识 + 展示信息。 */
data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val isSystem: Boolean,
    val customIconPath: String? = null
)
