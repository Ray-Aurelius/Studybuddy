package com.rayaurelius.studybuddy.domain

import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTimeTest {
    private val book = LearningItem(type = ItemType.AUDIOBOOK, title = "A", runtimeMin = 600) // 10h

    @Test fun noRuntimeMeansNoMediaProgress() {
        assertNull(MediaTime.progress(book.copy(runtimeMin = null), 3600))
        assertNull(MediaTime.progress(book.copy(runtimeMin = 0), 3600))
    }

    @Test fun loggedTimeCountsAtNormalSpeed() {
        val p = MediaTime.progress(book, loggedSec = 3 * 3600)!!
        assertEquals(3 * 3600L, p.completedSec)
        assertEquals(30, p.percent)
        assertEquals(7 * 3600L, p.remainingAtSpeedSec)
    }

    @Test fun playbackSpeedScalesMediaTime() {
        val p = MediaTime.progress(book.copy(playbackSpeed = 1.5f), loggedSec = 2 * 3600)!!
        assertEquals(3 * 3600L, p.completedSec) // 2h listening at 1.5× = 3h of audio
        assertEquals(7 * 3600L, p.remainingSec)
        assertEquals((7 * 3600 / 1.5).toLong(), p.remainingAtSpeedSec)
    }

    @Test fun priorProgressAndLiveSessionAdd() {
        val p = MediaTime.progress(book.copy(priorSec = 3600), loggedSec = 1800, liveSec = 1800)!!
        assertEquals(2 * 3600L, p.completedSec)
    }

    @Test fun completionCapsFractionAt100() {
        val p = MediaTime.progress(book, loggedSec = 11 * 3600)!!
        assertTrue(p.isComplete)
        assertEquals(1f, p.fraction, 0f)
        assertEquals(0L, p.remainingSec)
        assertFalse(MediaTime.progress(book, loggedSec = 9 * 3600)!!.isComplete)
    }

    @Test fun whichTypesAreTimed() {
        assertTrue(MediaTime.supportsRuntime(ItemType.PODCAST))
        assertTrue(MediaTime.supportsRuntime(ItemType.SEMINAR))
        assertTrue(MediaTime.supportsRuntime(ItemType.COURSE))
        assertTrue(MediaTime.supportsRuntime(ItemType.VIDEO))
        assertFalse(MediaTime.supportsRuntime(ItemType.BOOK))
        assertFalse(MediaTime.supportsSpeed(ItemType.SEMINAR))
    }

    @Test fun speedLabels() {
        assertEquals("2×", MediaTime.formatSpeed(2f))
        assertEquals("1.25×", MediaTime.formatSpeed(1.25f).replace(',', '.'))
        assertEquals("1.5×", MediaTime.formatSpeed(1.5f).replace(',', '.'))
    }
}
