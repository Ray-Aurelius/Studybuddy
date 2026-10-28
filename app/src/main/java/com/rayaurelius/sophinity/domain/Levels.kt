package com.rayaurelius.sophinity.domain

data class LevelInfo(
    val level: Int,
    val hoursIntoLevel: Double,
    val hoursForLevel: Double,
    val totalHours: Double,
) {
    val progress: Float get() = if (hoursForLevel <= 0) 1f else (hoursIntoLevel / hoursForLevel).toFloat().coerceIn(0f, 1f)
}

/**
 * Levels reward accumulated learning time. Early levels come quickly (fast feedback builds
 * self-efficacy); later levels stretch out to mark genuine mastery-scale investment.
 * Titles are separate and earned through laurels; see [Titles].
 */
object Levels {
    // Cumulative hours needed to *reach* level index+1.
    private val thresholds = listOf(
        0.0, 1.0, 3.0, 6.0, 10.0, 15.0, 22.0, 30.0, 40.0, 52.0,
        66.0, 82.0, 100.0, 125.0, 150.0, 180.0, 215.0, 255.0, 300.0, 350.0,
        410.0, 480.0, 560.0, 650.0, 750.0, 860.0, 1000.0,
    )

    private fun threshold(i: Int): Double =
        if (i < thresholds.size) thresholds[i] else thresholds.last() + (i - thresholds.lastIndex) * 150.0

    fun forSeconds(totalSeconds: Long): LevelInfo {
        val hours = totalSeconds / 3600.0
        var i = 0
        while (hours >= threshold(i + 1)) i++
        val lo = threshold(i)
        val hi = threshold(i + 1)
        val level = i + 1
        return LevelInfo(level, hours - lo, hi - lo, hours)
    }
}
