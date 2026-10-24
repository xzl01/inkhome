package io.github.xzl01.inkhome.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import io.github.xzl01.inkhome.data.AppEntry
import io.github.xzl01.inkhome.data.AppRepository

/** 彩色/黑白应用图标缓存（支持自定义图标文件）。 */
object IconCache {
    private val cache = LruCache<String, Bitmap>(64)

    fun invalidate(key: String) {
        val snapshot = cache.snapshot().keys.toList()
        snapshot.forEach { cacheKey ->
            if (cacheKey.startsWith("$key|")) cache.remove(cacheKey)
        }
    }

    fun get(repo: AppRepository, entry: AppEntry, sizePx: Int, color: Boolean): Bitmap? {
        val custom = entry.customIconPath
        val key = "${entry.key}|$sizePx|${if (color) 1 else 0}|${custom.orEmpty()}"
        cache.get(key)?.let { return it }

        val source: Bitmap? = if (!custom.isNullOrBlank()) {
            runCatching { BitmapFactory.decodeFile(custom) }.getOrNull()
                ?.let { scaleCenter(it, sizePx) }
        } else {
            val drawable = repo.loadIcon(entry) ?: return null
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                drawable.setBounds(0, 0, sizePx, sizePx)
                drawable.draw(canvas)
            }
        } ?: return null

        val src: Bitmap = source ?: return null
        val result = if (color) src else grayscale(src)
        cache.put(key, result)
        return result
    }

    private fun scaleCenter(src: Bitmap, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        val scale = minOf(sizePx.toFloat() / src.width, sizePx.toFloat() / src.height)
        val dw = (src.width * scale).toInt().coerceAtLeast(1)
        val dh = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, dw, dh, true)
        canvas.drawBitmap(scaled, (sizePx - dw) / 2f, (sizePx - dh) / 2f, null)
        return out
    }

    private fun grayscale(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        Canvas(out).drawBitmap(source, 0f, 0f, paint)
        return out
    }
}

@Composable
fun AppIcon(entry: AppEntry?, repo: AppRepository, size: Dp, color: Boolean) {
    if (entry == null) {
        Box(Modifier.size(size))
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = with(density) { size.roundToPx().coerceAtLeast(1) }
    val bitmap = remember(entry.key, entry.customIconPath, px, color, context) {
        IconCache.get(repo, entry, px, color)
    }
    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = entry.label,
            modifier = Modifier.size(size)
        )
    } else {
        Box(Modifier.size(size))
    }
}
