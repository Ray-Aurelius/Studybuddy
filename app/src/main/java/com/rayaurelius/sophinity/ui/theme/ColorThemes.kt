package com.rayaurelius.sophinity.ui.theme

/** A named color theme built from three seed colors. */
data class ColorTheme(
    val id: String,
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
) {
    fun roles(dark: Boolean, highContrast: Boolean): ColorEngine.Roles = ColorEngine.roles(
        ColorEngine.seedOf(primary), ColorEngine.seedOf(secondary), ColorEngine.seedOf(tertiary), dark, highContrast,
    )
}

object ColorThemes {
    const val WALLPAPER = "wallpaper"
    const val CUSTOM = "custom"
    const val DEFAULT = "aegean"

    val presets = listOf(
        ColorTheme("aegean", 0xFF2F9BFF.toInt(), 0xFF14A8A6.toInt(), 0xFF3DAA6B.toInt()),
        ColorTheme("olive", 0xFF6B8E23.toInt(), 0xFFB8860B.toInt(), 0xFFC8553D.toInt()),
        ColorTheme("terracotta", 0xFFC8553D.toInt(), 0xFFD2A15A.toInt(), 0xFF2A9D8F.toInt()),
        ColorTheme("tyrian", 0xFF8E3B9E.toInt(), 0xFFB5517A.toInt(), 0xFFC9A227.toInt()),
        ColorTheme("delphi", 0xFFE26D8A.toInt(), 0xFFF08A5D.toInt(), 0xFF7B6CD9.toInt()),
        ColorTheme("agora", 0xFF4659C8.toInt(), 0xFF26A69A.toInt(), 0xFFE0A800.toInt()),
        ColorTheme("marble", 0xFF5F6B7A.toInt(), 0xFF8C7B6B.toInt(), 0xFF5F8C8C.toInt()),
    )

    fun byId(id: String) = presets.firstOrNull { it.id == id }

    /** A custom theme from one color: same-hue secondary, neighboring-hue tertiary. */
    fun fromSeed(seed: Int): ColorTheme {
        val s = ColorEngine.seedOf(seed)
        val sec = ColorEngine.solve(s.hue + 20, 0.07, 0.3)
        val ter = ColorEngine.solve((s.hue + 70) % 360, 0.12, 0.3)
        return ColorTheme(CUSTOM, seed, sec, ter)
    }

    /** Swatches offered for the custom color picker. */
    val customSwatches = listOf(
        0xFFE53935, 0xFFF4511E, 0xFFFB8C00, 0xFFFDD835, 0xFFC0CA33, 0xFF7CB342, 0xFF43A047, 0xFF00897B,
        0xFF00ACC1, 0xFF039BE5, 0xFF1E88E5, 0xFF3949AB, 0xFF5E35B1, 0xFF8E24AA, 0xFFD81B60, 0xFF6D4C41,
    ).map { it.toInt() }
}
