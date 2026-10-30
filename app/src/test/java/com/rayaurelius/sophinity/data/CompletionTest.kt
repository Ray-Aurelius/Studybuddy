package com.rayaurelius.sophinity.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.domain.MediaTime
import com.rayaurelius.sophinity.timer.TimerMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CompletionTest {
    private val c get() = ApplicationProvider.getApplicationContext<SophinityApp>().container
    private val video = LearningItem(type = ItemType.VIDEO, title = "Talk", runtimeMin = 41, playbackSpeed = 1.5f)

    @Test fun creditIsWhatsLeftOfTheFullLengthWhateverTheSpeed() {
        assertEquals(41 * 60L, MediaTime.creditToCompleteSec(video, 0))
        assertEquals(21 * 60L, MediaTime.creditToCompleteSec(video, 20 * 60))
        assertEquals(16 * 60L, MediaTime.creditToCompleteSec(video, 20 * 60, liveSec = 5 * 60))
        assertEquals(0L, MediaTime.creditToCompleteSec(video, 50 * 60))
        assertNull(MediaTime.creditToCompleteSec(video.copy(runtimeMin = null), 0))
    }

    @Test fun completingFromTheLibraryLogsTheRestAndFinishesIt() = runBlocking {
        val id = c.repo.saveItem(video)
        c.repo.addSession(StudySession(itemId = id, startedAt = 1_000, durationSec = 20 * 60))
        assertEquals(21 * 60L, c.repo.completeItem(id, now = 10_000_000)!!.creditSec)
        assertEquals(41 * 60L, c.repo.secondsForItem(id))
        assertEquals(ItemStatus.DONE, c.repo.getItem(id)!!.status)
        // Completing again adds nothing more.
        val again = c.repo.completeItem(id)!!
        assertEquals(0L, again.creditSec)
        assertEquals(41 * 60L, c.repo.secondsForItem(id))
    }

    @Test fun completingCanBeUndone() = runBlocking {
        val id = c.repo.saveItem(video)
        val sid = c.repo.addSession(StudySession(itemId = id, startedAt = 1_000, durationSec = 20 * 60))
        val fromLibrary = c.repo.completeItem(id)!!
        c.repo.undo(fromLibrary)
        assertEquals(20 * 60L, c.repo.secondsForItem(id))
        assertEquals(ItemStatus.ACTIVE, c.repo.getItem(id)!!.status)
        val fromSession = c.repo.completeFromSession(sid)!!
        assertEquals(41 * 60L, c.repo.getSession(sid)!!.durationSec)
        c.repo.undo(fromSession)
        assertEquals(20 * 60L, c.repo.getSession(sid)!!.durationSec)
        assertEquals(ItemStatus.ACTIVE, c.repo.getItem(id)!!.status)
    }

    @Test fun completingALoggedSessionStretchesIt() = runBlocking {
        val id = c.repo.saveItem(video)
        val sid = c.repo.addSession(StudySession(itemId = id, startedAt = 1_000, durationSec = 15 * 60))
        assertEquals(26 * 60L, c.repo.completeFromSession(sid)!!.creditSec)
        assertEquals(41 * 60L, c.repo.getSession(sid)!!.durationSec)
        assertEquals(1, c.repo.sessions.first().count { it.itemId == id })
        assertEquals(ItemStatus.DONE, c.repo.getItem(id)!!.status)
    }

    @Test fun completingWhileTimingEndsTheSessionAtFullLength() = runBlocking {
        c.timer.discard()
        val id = c.repo.saveItem(video)
        c.repo.addSession(StudySession(itemId = id, startedAt = 1_000, durationSec = 10 * 60))
        c.timer.start(TimerMode.STOPWATCH, null, id, headStartMs = 5 * 60_000L)
        val sid = c.timer.finishCompleted()!!
        assertFalse(c.timer.current().isActive)
        assertEquals(31 * 60L, c.repo.getSession(sid)!!.durationSec) // 5 timed + 26 to reach 41 with the 10 logged
        assertEquals(41 * 60L, c.repo.secondsForItem(id))
        assertEquals(ItemStatus.DONE, c.repo.getItem(id)!!.status)
    }
}
