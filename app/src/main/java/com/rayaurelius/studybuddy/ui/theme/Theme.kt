package com.rayaurelius.studybuddy.ui.theme

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rayaurelius.studybuddy.data.ThemeMode

/** Brand accents used for meaning, not decoration: flame = consistency, mint = goals met. */
object Brand {
    val Indigo = Color(0xFF5B4BDB)
    val Flame = Color(0xFFFF8A3D)
    val Mint = Color(0xFF1DB98A)
    val Sun = Color(0xFFFFC93C)

    val subjectPalette = listOf(
        0xFF5B4BDB, 0xFF1DB98A, 0xFFFF8A3D, 0xFFE5487A, 0xFF2B9AE8,
        0xFF9C5BDB, 0xFFE8B02B, 0xFF14A3A3, 0xFF7A8B99, 0xFFD9542B,
    ).map { it.toInt() }
}

private val Light = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF170A6B),
    secondary = Color(0xFFB4561A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3D2),
    onSecondaryContainer = Color(0xFF3A1600),
    tertiary = Color(0xFF00805C),
    tertiaryContainer = Color(0xFFC9F5E2),
    onTertiaryContainer = Color(0xFF002116),
    background = Color(0xFFFCFAFF),
    surface = Color(0xFFFCFAFF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F3FD),
    surfaceContainer = Color(0xFFF0EDF8),
    surfaceContainerHigh = Color(0xFFEAE7F3),
    surfaceContainerHighest = Color(0xFFE4E1ED),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF2A1B9E),
    primaryContainer = Color(0xFF4335C2),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFFFB68D),
    secondaryContainer = Color(0xFF7A3500),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFF7BDAB3),
    tertiaryContainer = Color(0xFF005A40),
    onTertiaryContainer = Color(0xFFC9F5E2),
    background = Color(0xFF131218),
    surface = Color(0xFF131218),
    surfaceContainerLowest = Color(0xFF0E0D13),
    surfaceContainerLow = Color(0xFF1B1A21),
    surfaceContainer = Color(0xFF1F1E25),
    surfaceContainerHigh = Color(0xFF2A2830),
    surfaceContainerHighest = Color(0xFF35333B),
)

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val TimerDigits = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)

@Composable
fun StudybuddyTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamic: Boolean = false, content: @Composable () -> Unit) {
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
