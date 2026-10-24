package io.github.xzl01.inkhome.data

/** 桌面文件夹：包含若干应用 key（"pkg/activity"）。 */
data class FolderData(
    val id: String,
    val name: String,
    val apps: List<String>
) {
    companion object {
        fun slotValue(id: String) = "folder:$id"
        fun idFromSlot(value: String?) = value?.takeIf { it.startsWith("folder:") }?.removePrefix("folder:")
    }
}
