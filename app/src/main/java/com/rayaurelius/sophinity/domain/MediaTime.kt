package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
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

    /**
     * The "completed before tracking" amount that puts the learner at [positionSec] in the media,
     * given the study time already logged on it (and any still running). Logged time is never
     * changed. If they've gone back to before what was logged, this bottoms out at zero.
     */
    fun priorForPosition(item: LearningItem, positionSec: Long, loggedSec: Long, liveSec: Long = 0): Long {
        val speed = item.playbackSpeed.takeIf { it > 0f } ?: 1f
        return (positionSec - ((loggedSec + liveSec) * speed).roundToLong()).coerceAtLeast(0)
    }

    /**
     * Marking an item completed credits its full length: the study time still needed for the item
     * to total its run time, whatever the playback speed (a 41-minute video counts as 41 minutes).
     * Time already logged, or running now ([liveSec]), is not counted twice. Null without a run time.
     */
    fun creditToCompleteSec(item: LearningItem, loggedSec: Long, liveSec: Long = 0): Long? {
        val runtime = item.runtimeMin?.takeIf { it > 0 } ?: return null
        return (runtime * 60L - loggedSec - liveSec).coerceAtLeast(0)
    }
}
