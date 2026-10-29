package com.rayaurelius.sophinity.timer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rayaurelius.sophinity.SophinityApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HeadStartTest {
    private val timer get() = ApplicationProvider.getApplicationContext<SophinityApp>().container.timer
    private val repo get() = ApplicationProvider.getApplicationContext<SophinityApp>().container.repo

    @Test fun stopwatchStartsFromTheHeadStartAndBackdatesTheSession() = runBlocking {
        timer.discard()
        val before = System.currentTimeMillis()
        timer.start(TimerMode.STOPWATCH, null, null, headStartMs = 25 * 60_000L)
        val s = timer.current()
        assertTrue("counts the head start", s.elapsedMs() >= 25 * 60_000L)
        assertTrue("session start moved back", s.startedAt <= before - 25 * 60_000L + 1_000)

        val id = timer.finish()!!
        val saved = repo.getSession(id)!!
        assertTrue(saved.durationSec >= 25 * 60)
        assertEquals(s.startedAt, saved.startedAt)
    }

    @Test fun focusBlocksIgnoreHeadStart() = runBlocking {
        timer.discard()
        timer.start(TimerMode.FOCUS, null, null, headStartMs = 10 * 60_000L)
        assertTrue(timer.current().elapsedMs() < 60_000L)
        timer.discard()
    }

    @Test fun headStartIsCapped() = runBlocking {
        timer.discard()
        timer.start(TimerMode.STOPWATCH, null, null, headStartMs = 30 * 3_600_000L)
        assertTrue(timer.current().elapsedMs() < TimerController.MAX_HEAD_START_MS + 60_000L)
        timer.discard()
    }
}
