package com.rayaurelius.sophinity.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rayaurelius.sophinity.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rayaurelius.sophinity.data.ThemeMode

/**
 * Sophinity brand: Aegean blue skies, marble white, laurel gold.
 * Accents carry meaning, not decoration: flame = consistency, laurel green = goals met, gold = achievement.
 */
object Brand {
    val Aegean = Color(0xFF2F9BFF)
    val Sea = Color(0xFF17B3B0)
    val Navy = Color(0xFF13294B)
    val Gold = Color(0xFFFFD23F)
    val Laurel = Color(0xFF3DAA6B)
    val Flame = Color(0xFFE2683C)
    val Marble = Color(0xFFF7FBFF)

    val subjectPalette = listOf(
        0xFF2F9BFF, 0xFF3DAA6B, 0xFFE2683C, 0xFFE0A800, 0xFF8E3B8E,
        0xFF17A2A0, 0xFF7A8B2E, 0xFFD9577A, 0xFF2B4C8C, 0xFF7C8794,
    ).map { it.toInt() }
}

private val Light = lightColorScheme(
    primary = Color(0xFF0A6FD8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF002A52),
    secondary = Color(0xFF00696A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFEE),
    onSecondaryContainer = Color(0xFF002020),
    tertiary = Color(0xFF1F7A47),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFF2DC),
    onTertiaryContainer = Color(0xFF00210E),
    background = Color(0xFFF7FBFF),
    onBackground = Color(0xFF13294B),
    surface = Color(0xFFF7FBFF),
    onSurface = Color(0xFF13294B),
    onSurfaceVariant = Color(0xFF4A5A73),
    outline = Color(0xFF7B8BA3),
    outlineVariant = Color(0xFFC9D4E3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F6FD),
    surfaceContainer = Color(0xFFEAF1FA),
    surfaceContainerHigh = Color(0xFFE3ECF7),
    surfaceContainerHighest = Color(0xFFDCE6F2),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FCBFF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF0D4F8F),
    onPrimaryContainer = Color(0xFFD4E8FF),
    secondary = Color(0xFF6FD6D4),
    onSecondary = Color(0xFF003737),
    secondaryContainer = Color(0xFF0F4A4A),
    onSecondaryContainer = Color(0xFFCDEFEE),
    tertiary = Color(0xFF7FD9A2),
    onTertiary = Color(0xFF00391C),
    tertiaryContainer = Color(0xFF0E5A32),
    onTertiaryContainer = Color(0xFFCFF2DC),
    background = Color(0xFF0D1626),
    onBackground = Color(0xFFE3ECF7),
    surface = Color(0xFF0D1626),
    onSurface = Color(0xFFE3ECF7),
    onSurfaceVariant = Color(0xFFA9B7CC),
    outline = Color(0xFF6E7D94),
    outlineVariant = Color(0xFF2E3B52),
    surfaceContainerLowest = Color(0xFF08101D),
    surfaceContainerLow = Color(0xFF121D2E),
    surfaceContainer = Color(0xFF172235),
    surfaceContainerHigh = Color(0xFF1F2B40),
    surfaceContainerHighest = Color(0xFF29364B),
)

/** Laurel gold, reserved for achievement: earned laurels, titles and the streak. */
data class GoldColors(val container: Color, val onContainer: Color)
private val LightGold = GoldColors(Color(0xFFFFF0B8), Color(0xFF3A2C00))
private val DarkGold = GoldColors(Color(0xFF4A3B00), Color(0xFFFFE9A8))
val LocalGold = androidx.compose.runtime.staticCompositionLocalOf { LightGold }

/** Marcellus: a flared classical serif, used for display and headings only. Body text stays system sans for readability. */
val Classical = FontFamily(Font(R.font.marcellus))

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Classical),
    displayMedium = base.displayMedium.copy(fontFamily = Classical),
    displaySmall = base.displaySmall.copy(fontFamily = Classical),
    headlineLarge = base.headlineLarge.copy(fontFamily = Classical),
    headlineMedium = base.headlineMedium.copy(fontFamily = Classical),
    headlineSmall = base.headlineSmall.copy(fontFamily = Classical),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val TimerDigits = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
/** Slightly smaller so h:mm:ss media positions fit inside the ring. */
val MediaDigits = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)

@Composable
fun SophinityTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamic: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalGold provides if (dark) DarkGold else LightGold) {
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
    }
}
