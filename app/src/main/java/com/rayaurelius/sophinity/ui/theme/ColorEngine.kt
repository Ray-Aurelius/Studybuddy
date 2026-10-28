package com.rayaurelius.sophinity.ui.theme

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Generates accessible light/dark color roles from seed colors.
 *
 * Every role is solved for a target WCAG relative luminance (not a lightness guess), working in
 * OKLCH so hue and saturation stay true to the seed. Because contrast depends only on luminance,
 * the text/background pairs below meet WCAG AA (4.5:1) for any hue — and AAA-level targets in
 * high-contrast mode. ColorEngineTest checks this across the whole color wheel.
 */
object ColorEngine {

    /** All roles as ARGB ints, mirroring Material 3 color-scheme slots. */
    data class Roles(
        val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
        val secondary: Int, val onSecondary: Int, val secondaryContainer: Int, val onSecondaryContainer: Int,
        val tertiary: Int, val onTertiary: Int, val tertiaryContainer: Int, val onTertiaryContainer: Int,
        val background: Int, val onBackground: Int, val surface: Int, val onSurface: Int, val onSurfaceVariant: Int,
        val outline: Int, val outlineVariant: Int,
        val surfaceContainerLowest: Int, val surfaceContainerLow: Int, val surfaceContainer: Int,
        val surfaceContainerHigh: Int, val surfaceContainerHighest: Int,
    )

    /** A seed expressed as OKLCH hue (degrees) and chroma. */
    data class Seed(val hue: Double, val chroma: Double)

    fun seedOf(argb: Int): Seed {
        val (l, a, b) = toOklab(argb)
        val hue = Math.toDegrees(atan2(b, a)).let { if (it < 0) it + 360 else it }
        return Seed(hue, hypot(a, b)).also { require(l >= 0) }
    }

    private data class Targets(
        val primary: Double, val onPrimary: Double, val container: Double, val onContainer: Double,
        val background: Double, val onSurface: Double, val onVariant: Double,
        val outline: Double, val outlineVariant: Double, val surfaces: List<Double>,
    )

    // Target relative luminance (0 = black, 1 = white) for each role.
    private val LIGHT = Targets(0.13, 1.0, 0.75, 0.03, 0.95, 0.025, 0.11, 0.28, 0.72, listOf(1.0, 0.92, 0.89, 0.85, 0.81))
    private val LIGHT_HC = Targets(0.07, 1.0, 0.80, 0.012, 0.97, 0.01, 0.05, 0.14, 0.55, listOf(1.0, 0.95, 0.93, 0.90, 0.87))
    private val DARK = Targets(0.55, 0.05, 0.06, 0.72, 0.012, 0.82, 0.45, 0.16, 0.04, listOf(0.007, 0.016, 0.02, 0.03, 0.045))
    private val DARK_HC = Targets(0.72, 0.012, 0.025, 0.88, 0.004, 0.92, 0.70, 0.35, 0.10, listOf(0.002, 0.008, 0.012, 0.018, 0.026))

    fun roles(primary: Seed, secondary: Seed, tertiary: Seed, dark: Boolean, highContrast: Boolean): Roles {
        val t = when {
            dark && highContrast -> DARK_HC
            dark -> DARK
            highContrast -> LIGHT_HC
            else -> LIGHT
        }
        val pc = min(primary.chroma, 0.17)
        val sc = min(secondary.chroma, 0.12)
        val tc = min(tertiary.chroma, 0.13)
        fun c(seed: Seed, chroma: Double, y: Double) = solve(seed.hue, chroma, y)
        val nH = primary.hue
        val n = 0.012 // neutrals keep a faint tint of the primary hue
        val nv = 0.025
        return Roles(
            primary = c(primary, pc, t.primary), onPrimary = c(primary, 0.02, t.onPrimary),
            primaryContainer = c(primary, 0.07, t.container), onPrimaryContainer = c(primary, 0.08, t.onContainer),
            secondary = c(secondary, sc, t.primary), onSecondary = c(secondary, 0.02, t.onPrimary),
            secondaryContainer = c(secondary, 0.06, t.container), onSecondaryContainer = c(secondary, 0.07, t.onContainer),
            tertiary = c(tertiary, tc, t.primary), onTertiary = c(tertiary, 0.02, t.onPrimary),
            tertiaryContainer = c(tertiary, 0.07, t.container), onTertiaryContainer = c(tertiary, 0.08, t.onContainer),
            background = solve(nH, n, t.background), onBackground = solve(nH, nv, t.onSurface),
            surface = solve(nH, n, t.background), onSurface = solve(nH, nv, t.onSurface),
            onSurfaceVariant = solve(nH, nv, t.onVariant),
            outline = solve(nH, nv, t.outline), outlineVariant = solve(nH, n, t.outlineVariant),
            surfaceContainerLowest = solve(nH, n, t.surfaces[0]), surfaceContainerLow = solve(nH, n, t.surfaces[1]),
            surfaceContainer = solve(nH, n, t.surfaces[2]), surfaceContainerHigh = solve(nH, n, t.surfaces[3]),
            surfaceContainerHighest = solve(nH, n, t.surfaces[4]),
        )
    }

    /** Finds the OKLCH color with this hue/chroma (reduced to fit sRGB) whose luminance is [targetY]. */
    fun solve(hue: Double, chroma: Double, targetY: Double): Int {
        if (targetY >= 0.999) return 0xFFFFFFFF.toInt()
        var lo = 0.0
        var hi = 1.0
        var best = 0
        repeat(40) {
            val l = (lo + hi) / 2
            val argb = inGamut(l, chroma, hue)
            if (luminance(argb) < targetY) lo = l else hi = l
            best = argb
        }
        return best
    }

    private fun inGamut(l: Double, c: Double, hDeg: Double): Int {
        val h = Math.toRadians(hDeg)
        var lo = 0.0
        var hi = c
        var rgb = oklabToLinear(l, 0.0, 0.0)
        if (fits(oklabToLinear(l, c * cos(h), c * sin(h)))) return toArgb(oklabToLinear(l, c * cos(h), c * sin(h)))
        repeat(20) {
            val mid = (lo + hi) / 2
            val cand = oklabToLinear(l, mid * cos(h), mid * sin(h))
            if (fits(cand)) { lo = mid; rgb = cand } else hi = mid
        }
        return toArgb(rgb)
    }

    private fun fits(rgb: DoubleArray) = rgb.all { it in -1e-4..1.0001 }

    // ---- sRGB / OKLab conversions ----

    private fun toLinear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun toGamma(c: Double) = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055

    fun luminance(argb: Int): Double {
        val r = toLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = toLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = toLinear((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun toOklab(argb: Int): Triple<Double, Double, Double> {
        val r = toLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = toLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = toLinear((argb and 0xFF) / 255.0)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return Triple(
            0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    private fun oklabToLinear(lab: Double, a: Double, b: Double): DoubleArray {
        val l = (lab + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lab - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lab - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return doubleArrayOf(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    private fun toArgb(rgb: DoubleArray): Int {
        fun ch(v: Double) = (toGamma(v.coerceIn(0.0, 1.0)) * 255).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(rgb[0]) shl 16) or (ch(rgb[1]) shl 8) or ch(rgb[2])
    }
}
