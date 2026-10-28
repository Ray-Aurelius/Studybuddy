package com.rayaurelius.sophinity.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TitlesTest {
    private fun unlocked(vararg keys: String) = keys.withIndex().associate { (i, k) -> k to (1000L + i) }

    @Test fun everyoneStartsAsNeophyte() {
        val s = Titles.status(emptyMap())
        assertEquals("neophyte", s.current.id)
        assertEquals("seeker", s.next?.id)
        assertNull(s.earnedAt)
    }

    @Test fun philosopherIsTheHighestTitle() {
        assertEquals("philosopher", Titles.highest.id)
        assertEquals(Titles.all.size, Titles.highest.rank)
        assertEquals((1..Titles.all.size).toList(), Titles.all.map { it.rank })
    }

    @Test fun everyGateIsARealMilestone() {
        Titles.all.flatMap { it.requires }.forEach { assertTrue(it, it in Milestones.byKey) }
    }

    @Test fun titlesAreEarnedInOrder() {
        // Has Pupil's gates but not Seeker's (impossible in practice, but the ladder must not skip rungs).
        assertEquals("neophyte", Titles.status(unlocked("hours_10", "streak_7")).current.id)
        assertEquals("pupil", Titles.status(unlocked("first_session", "hours_10", "streak_7")).current.id)
    }

    @Test fun nextChecklistShowsWhatIsMissing() {
        val s = Titles.status(unlocked("first_session", "hours_10"))
        assertEquals("seeker", s.current.id)
        assertEquals("pupil", s.next?.id)
        assertEquals(listOf(true, false), s.nextChecklist.map { it.second })
    }

    @Test fun reachingPhilosopher() {
        val all = Titles.all.flatMap { it.requires }.toTypedArray()
        val s = Titles.status(unlocked(*all))
        assertEquals("philosopher", s.current.id)
        assertNull(s.next)
        assertEquals(1000L + all.size - 1, s.earnedAt)
    }
}
