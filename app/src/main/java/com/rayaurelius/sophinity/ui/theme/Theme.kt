package com.rayaurelius.sophinity.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.rayaurelius.sophinity.data.AppSettings
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
import androidx.compose.ui.platform.LocalConfiguration
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

private fun ColorEngine.Roles.toScheme(dark: Boolean): ColorScheme {
    fun c(v: Int) = Color(v)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
        inversePrimary = c(if (dark) primaryContainer else primaryContainer),
        secondary = c(secondary), onSecondary = c(onSecondary), secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
        background = c(background), onBackground = c(onBackground), surface = c(surface), onSurface = c(onSurface),
        surfaceVariant = c(surfaceContainerHighest), onSurfaceVariant = c(onSurfaceVariant),
        surfaceTint = c(primary), outline = c(outline), outlineVariant = c(outlineVariant),
        surfaceBright = c(if (dark) surfaceContainerHighest else surfaceContainerLowest), surfaceDim = c(if (dark) surface else surfaceContainerHighest),
        surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow), surfaceContainer = c(surfaceContainer),
        surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
    )
}

/** Laurel gold, reserved for achievement: earned laurels, titles and the streak. */
/** [ink] is gold line art (laurel wreaths) drawn on the page; it keeps at least 3:1 contrast against the background. */
data class GoldColors(val container: Color, val onContainer: Color, val ink: Color)
private val LightGold = GoldColors(Color(0xFFFFF0B8), Color(0xFF3A2C00), Color(0xFF946E00))
private val DarkGold = GoldColors(Color(0xFF4A3B00), Color(0xFFFFE9A8), Color(0xFFE2B93B))
private val LightGoldHc = GoldColors(Color(0xFFFFF4C9), Color(0xFF1F1700), Color(0xFF654B00))
private val DarkGoldHc = GoldColors(Color(0xFF2E2400), Color(0xFFFFF4C9), Color(0xFFF5D36A))
val LocalGold = staticCompositionLocalOf { LightGold }

/** True when animations should be skipped (setting, or the system "remove animations" option). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Marcellus: a flared classical serif, used for display and headings only. Body text stays system sans for readability. */
val Classical = FontFamily(Font(R.font.marcellus))

/** Atkinson Hyperlegible: designed by the Braille Institute for low-vision readers. */
val Readable = FontFamily(Font(R.font.atkinson_regular, FontWeight.Normal), Font(R.font.atkinson_bold, FontWeight.Bold))

/** Heading font in use: Marcellus by default, Atkinson in readable-font mode. */
val LocalHeadingFont = staticCompositionLocalOf { Classical }

/**
 * Marcellus and Atkinson only carry Latin glyphs (no Vietnamese stacked diacritics either), and
 * Android fills missing glyphs one by one from system fonts, which would mix typefaces inside a word.
 * So each language gets the bundled fonts only when they cover its script; otherwise the system serif
 * (Noto Serif, which covers Greek, Cyrillic and Vietnamese) keeps the classical feel for headings,
 * and scripts with no serif tradition on Android use the default system font.
 */
internal enum class ScriptFonts { BUNDLED, SYSTEM_SERIF, SYSTEM }

internal fun scriptFontsFor(language: String): ScriptFonts = when (language) {
    "en", "es", "pt", "fr", "de", "it", "nl", "pl", "tr", "id", "in" -> ScriptFonts.BUNDLED
    "el", "ru", "uk", "vi" -> ScriptFonts.SYSTEM_SERIF
    else -> ScriptFonts.SYSTEM
}

internal fun headingAndBody(script: ScriptFonts, readable: Boolean): Pair<FontFamily, FontFamily?> = when (script) {
    ScriptFonts.BUNDLED -> if (readable) Readable to Readable else Classical to null
    // System sans is the most legible option these scripts have, so readable mode uses it for headings too.
    ScriptFonts.SYSTEM_SERIF -> if (readable) FontFamily.SansSerif to null else FontFamily.Serif to null
    ScriptFonts.SYSTEM -> FontFamily.Default to null
}

private fun typography(heading: FontFamily, body: FontFamily?, bold: Boolean): Typography {
    val base = Typography()
    fun TextStyle.w(default: FontWeight?, family: FontFamily?): TextStyle {
        val weight = (default ?: fontWeight ?: FontWeight.Normal).let { if (bold) FontWeight((it.weight + 300).coerceAtMost(900)) else it }
        return copy(fontWeight = weight, fontFamily = family ?: fontFamily)
    }
    return Typography(
        displayLarge = base.displayLarge.w(null, heading), displayMedium = base.displayMedium.w(null, heading),
        displaySmall = base.displaySmall.w(null, heading), headlineLarge = base.headlineLarge.w(null, heading),
        headlineMedium = base.headlineMedium.w(null, heading), headlineSmall = base.headlineSmall.w(null, heading),
        titleLarge = base.titleLarge.w(FontWeight.SemiBold, body), titleMedium = base.titleMedium.w(FontWeight.SemiBold, body),
        titleSmall = base.titleSmall.w(null, body), bodyLarge = base.bodyLarge.w(null, body),
        bodyMedium = base.bodyMedium.w(null, body), bodySmall = base.bodySmall.w(null, body),
        labelLarge = base.labelLarge.w(FontWeight.SemiBold, body), labelMedium = base.labelMedium.w(null, body),
        labelSmall = base.labelSmall.w(null, body),
    )
}

val TimerDigits = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
/** Slightly smaller so h:mm:ss media positions fit inside the ring. */
val MediaDigits = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)

@Composable
fun SophinityTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val hc = settings.highContrast
    val ctx = LocalContext.current
    val scheme = remember(settings.colorTheme, settings.customSeed, dark, hc) {
        when {
            settings.colorTheme == ColorThemes.WALLPAPER && Build.VERSION.SDK_INT >= 31 ->
                if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            settings.colorTheme == ColorThemes.CUSTOM -> ColorThemes.fromSeed(settings.customSeed).roles(dark, hc).toScheme(dark)
            else -> (ColorThemes.byId(settings.colorTheme) ?: ColorThemes.byId(ColorThemes.DEFAULT)!!).roles(dark, hc).toScheme(dark)
        }
    }
    val language = LocalConfiguration.current.locales[0].language
    val script = scriptFontsFor(language)
    val (heading, body) = headingAndBody(script, settings.readableFont)
    val typography = remember(script, settings.readableFont, settings.boldText) {
        typography(heading, body, settings.boldText)
    }
    val systemNoAnimations = remember {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    val density = LocalDensity.current
    val gold = when {
        dark && hc -> DarkGoldHc
        dark -> DarkGold
        hc -> LightGoldHc
        else -> LightGold
    }
    CompositionLocalProvider(
        LocalGold provides gold,
        LocalReduceMotion provides (settings.reduceMotion ?: systemNoAnimations),
        LocalHeadingFont provides heading,
        com.rayaurelius.sophinity.ui.components.LocalChoiceStyle provides settings.choiceStyle,
        LocalDensity provides Density(density.density, density.fontScale * settings.textScale),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}
