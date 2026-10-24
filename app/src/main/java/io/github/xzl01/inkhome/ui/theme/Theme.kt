package io.github.xzl01.inkhome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 彩色墨水屏：白纸黑字 + 极少量低饱和强调色（避免大面积色块残影）。 */
val InkWhite = Color(0xFFFFFFFF)
val InkBlack = Color(0xFF111111)
val InkGray = Color(0xFF555555)
val InkLine = Color(0xFFBBBBBB)
val InkAccent = Color(0xFF2F5D8C)
val InkRed = Color(0xFFB3261E)
val InkGreen = Color(0xFF2E7D32)

private val colorScheme = lightColorScheme(
    primary = InkAccent,
    onPrimary = InkWhite,
    background = InkWhite,
    onBackground = InkBlack,
    surface = InkWhite,
    onSurface = InkBlack,
    surfaceVariant = InkWhite,
    onSurfaceVariant = InkGray,
    outline = InkLine
)

private val inkTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = InkBlack),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = InkBlack),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = InkGray)
)

@Composable
fun InkHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = inkTypography, content = content)
}

val Serif = FontFamily.Serif
val DISPLAY = FontFamily.SansSerif
val DisplayWeight = FontWeight.Bold
