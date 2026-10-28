package com.rayaurelius.sophinity.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ColorEngineTest {
    private fun check(label: String, fg: Int, bg: Int, min: Double) {
        val c = ColorEngine.contrast(fg, bg)
        assertTrue("$label contrast %.2f < $min".format(c), c >= min)
    }

    private fun assertAccessible(r: ColorEngine.Roles, hc: Boolean, tag: String) {
        val text = if (hc) 7.0 else 4.5
        check("$tag onPrimary/primary", r.onPrimary, r.primary, text)
        check("$tag onSecondary/secondary", r.onSecondary, r.secondary, text)
        check("$tag onTertiary/tertiary", r.onTertiary, r.tertiary, text)
        check("$tag onPrimaryContainer", r.onPrimaryContainer, r.primaryContainer, text)
        check("$tag onSecondaryContainer", r.onSecondaryContainer, r.secondaryContainer, text)
        check("$tag onTertiaryContainer", r.onTertiaryContainer, r.tertiaryContainer, text)
        check("$tag onSurface", r.onSurface, r.surface, text)
        check("$tag onSurfaceVariant", r.onSurfaceVariant, r.surface, text)
        listOf(r.surfaceContainerLowest, r.surfaceContainerLow, r.surfaceContainer, r.surfaceContainerHigh, r.surfaceContainerHighest)
            .forEachIndexed { i, s ->
                check("$tag onSurface/container$i", r.onSurface, s, text)
                check("$tag onSurfaceVariant/container$i", r.onSurfaceVariant, s, 4.5)
                check("$tag primary text/container$i", r.primary, s, 4.5)
            }
        check("$tag outline (UI component)", r.outline, r.surface, 3.0)
    }

    @Test fun everyHueMeetsWcagInEveryMode() {
        for (hue in 0 until 360 step 15) {
            val p = ColorEngine.Seed(hue.toDouble(), 0.2)
            val s = ColorEngine.Seed((hue + 30) % 360.0, 0.08)
            val t = ColorEngine.Seed((hue + 60) % 360.0, 0.1)
            for (dark in listOf(false, true)) for (hc in listOf(false, true)) {
                assertAccessible(ColorEngine.roles(p, s, t, dark, hc), hc, "hue=$hue dark=$dark hc=$hc")
            }
        }
    }

    @Test fun builtInThemesAreAccessible() {
        ColorThemes.presets.forEach { theme ->
            for (dark in listOf(false, true)) for (hc in listOf(false, true)) {
                assertAccessible(theme.roles(dark, hc), hc, "${theme.id} dark=$dark hc=$hc")
            }
        }
    }

    @Test fun seedRoundTripKeepsHue() {
        val seed = ColorEngine.seedOf(0xFF2F9BFF.toInt())
        assertTrue(seed.hue in 240.0..260.0)
        assertTrue(seed.chroma > 0.1)
    }
}
