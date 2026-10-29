package com.rayaurelius.sophinity.timer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.timer.LinkTimer.Action
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LinkTimerTest {
    private val timer get() = ApplicationProvider.getApplicationContext<SophinityApp>().container.timer
    private val repo get() = ApplicationProvider.getApplicationContext<SophinityApp>().container.repo

    @Test fun decisions() {
        assertEquals(Action.STARTED, LinkTimer.decide(TimerState(), 1))
        assertEquals(Action.STARTED, LinkTimer.decide(TimerState(status = TimerStatus.BREAK), 1))
        assertEquals(Action.ALREADY_TIMING, LinkTimer.decide(TimerState(status = TimerStatus.RUNNING, itemId = 1), 1))
        assertEquals(Action.ALREADY_TIMING, LinkTimer.decide(TimerState(status = TimerStatus.RUNNING, mode = TimerMode.FOCUS, itemId = 1), 1))
        assertEquals(Action.RESUMED, LinkTimer.decide(TimerState(status = TimerStatus.PAUSED, itemId = 1), 1))
        assertEquals(Action.SWITCHED, LinkTimer.decide(TimerState(status = TimerStatus.RUNNING, itemId = 2), 1))
        assertEquals(Action.SWITCHED, LinkTimer.decide(TimerState(status = TimerStatus.PAUSED, itemId = null), 1))
    }

    @Test fun startsAStopwatchOnTheItem() = runBlocking {
        timer.discard()
        assertEquals(Action.STARTED, timer.startForLink(7, 3))
        val s = timer.current()
        assertEquals(TimerStatus.RUNNING, s.status)
        assertEquals(TimerMode.STOPWATCH, s.mode)
        assertEquals(7L, s.itemId)
        assertEquals(3L, s.subjectId)
        // Opening the same link again changes nothing.
        assertEquals(Action.ALREADY_TIMING, timer.startForLink(7, 3))
        assertEquals(s, timer.current())
        timer.discard()
    }

    @Test fun resumesAPausedSessionOnTheSameItem() = runBlocking {
        timer.discard()
        timer.start(TimerMode.STOPWATCH, null, 7)
        timer.pause()
        assertEquals(Action.RESUMED, timer.startForLink(7, null))
        assertEquals(TimerStatus.RUNNING, timer.current().status)
        timer.discard()
    }

    @Test fun switchingSavesTheOtherSessionFirst() = runBlocking {
        timer.discard()
        timer.start(TimerMode.STOPWATCH, 1, 5, headStartMs = 20 * 60_000L)
        val before = repo.sessions.first().size
        assertEquals(Action.SWITCHED, timer.startForLink(7, 2))
        val sessions = repo.sessions.first()
        assertEquals(before + 1, sessions.size)
        assertTrue(sessions.any { it.itemId == 5L && it.durationSec >= 20 * 60 })
        assertEquals(7L, timer.current().itemId)
        assertTrue(timer.current().elapsedMs() < 60_000)
        timer.discard()
    }

    @Test fun aSwitchFromUnderAMinuteKeepsNothingAndSaysStarted() = runBlocking {
        timer.discard()
        timer.start(TimerMode.STOPWATCH, null, 5)
        assertEquals(Action.STARTED, timer.startForLink(7, null))
        assertEquals(7L, timer.current().itemId)
        timer.discard()
    }
}
