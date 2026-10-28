package com.rayaurelius.sophinity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TitlesTest {
    private fun unlocked(vararg keys: String) = keys.withIndex().associate { (i, k) -> k to (1000L + i) }

    @Test fun everyoneStartsAsNeophyte() {
        val s = Titles.status(emptyMap())
        assertEquals("Neophyte", s.current.name)
        assertEquals("Seeker", s.next?.name)
        assertNull(s.earnedAt)
    }

    @Test fun philosopherIsTheHighestTitle() {
        assertEquals("Philosopher", Titles.highest.name)
        assertEquals(Titles.all.size, Titles.highest.rank)
        assertEquals((1..Titles.all.size).toList(), Titles.all.map { it.rank })
    }

    @Test fun everyGateIsARealMilestone() {
        Titles.all.flatMap { it.requires }.forEach { assertTrue(it, it in Milestones.byKey) }
    }

    @Test fun titlesAreEarnedInOrder() {
        // Has Pupil's gates but not Seeker's (impossible in practice, but the ladder must not skip rungs).
        assertEquals("Neophyte", Titles.status(unlocked("hours_10", "streak_7")).current.name)
        assertEquals("Pupil", Titles.status(unlocked("first_session", "hours_10", "streak_7")).current.name)
    }

    @Test fun nextChecklistShowsWhatIsMissing() {
        val s = Titles.status(unlocked("first_session", "hours_10"))
        assertEquals("Seeker", s.current.name)
        assertEquals("Pupil", s.next?.name)
        assertEquals(listOf(true, false), s.nextChecklist.map { it.second })
    }

    @Test fun reachingPhilosopher() {
        val all = Titles.all.flatMap { it.requires }.toTypedArray()
        val s = Titles.status(unlocked(*all))
        assertEquals("Philosopher", s.current.name)
        assertNull(s.next)
        assertEquals(1000L + all.size - 1, s.earnedAt)
    }
}
