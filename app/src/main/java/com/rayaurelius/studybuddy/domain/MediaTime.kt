package com.rayaurelius.studybuddy.domain

import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem
import kotlin.math.roundToLong

/** Progress through a piece of timed media, all in seconds of *media* time. */
data class MediaProgress(
    val completedSec: Long,
    val totalSec: Long,
    val speed: Float,
) {
    val fraction: Float get() = if (totalSec <= 0) 0f else (completedSec.toFloat() / totalSec).coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).toInt()
    val isComplete: Boolean get() = totalSec > 0 && completedSec >= totalSec
    val remainingSec: Long get() = (totalSec - completedSec).coerceAtLeast(0)
    /** Real-world time left at the current playback speed. */
    val remainingAtSpeedSec: Long get() = (remainingSec / speed.coerceAtLeast(0.1f)).roundToLong()
}

object MediaTime {
    /** Types where a total run time makes sense. Custom categories may be timed too. */
    val TIMED = setOf(ItemType.AUDIOBOOK, ItemType.PODCAST, ItemType.VIDEO, ItemType.COURSE, ItemType.SEMINAR, ItemType.CUSTOM)

    /** Types usually consumed with a playback-speed control. */
    val SPEED_TYPES = setOf(ItemType.AUDIOBOOK, ItemType.PODCAST, ItemType.VIDEO, ItemType.COURSE)

    val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

    fun supportsRuntime(type: ItemType) = type in TIMED
    fun supportsSpeed(type: ItemType) = type in SPEED_TYPES

    /**
     * Media time completed = time completed before tracking + logged study time × playback speed.
     * [liveSec] is study time from a session still running on the timer.
     */
    fun progress(item: LearningItem, loggedSec: Long, liveSec: Long = 0): MediaProgress? {
        val runtime = item.runtimeMin?.takeIf { it > 0 } ?: return null
        val speed = item.playbackSpeed.takeIf { it > 0f } ?: 1f
        val done = item.priorSec + ((loggedSec + liveSec) * speed).roundToLong()
        return MediaProgress(done, runtime * 60L, speed)
    }

    fun formatSpeed(speed: Float): String =
        if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${"%.2f".format(speed).trimEnd('0').trimEnd('.')}×"
}
