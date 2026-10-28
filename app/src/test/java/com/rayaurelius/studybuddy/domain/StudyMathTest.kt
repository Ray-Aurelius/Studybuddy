package com.rayaurelius.studybuddy.domain

import com.rayaurelius.studybuddy.data.db.Goal
import com.rayaurelius.studybuddy.data.db.GoalKind
import com.rayaurelius.studybuddy.data.db.GoalPeriod
import com.rayaurelius.studybuddy.data.db.ItemStatus
import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem
import com.rayaurelius.studybuddy.data.db.StudySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class StudyMathTest {
    private val zone = ZoneId.of("America/Denver")
    private val today = LocalDate.of(2026, 10, 28) // a Wednesday
    private fun days(vararg offsets: Int, minutes: Long = 30) = offsets.associate { today.minusDays(it.toLong()) to minutes * 60 }
    private fun streak(daily: Map<LocalDate, Long>, rest: Int = 1) =
        StudyMath.streak(daily, today, 10 * 60, rest, DayOfWeek.MONDAY)

    @Test fun emptyHistoryHasNoStreak() {
        assertEquals(0, streak(emptyMap()).current)
    }

    @Test fun consecutiveDaysCount() {
        val s = streak(days(0, 1, 2, 3))
        assertEquals(4, s.current)
        assertTrue(s.studiedToday)
    }

    @Test fun notYetStudiedTodayKeepsStreak() {
        val s = streak(days(1, 2, 3))
        assertEquals(3, s.current)
        assertFalse(s.studiedToday)
    }

    @Test fun oneRestDayPerWeekIsForgiven() {
        // Studied Wed(today), Mon, Sun, Sat; skipped Tue (1 day ago).
        val s = streak(days(0, 2, 3, 4))
        assertEquals(4, s.current)
        assertEquals(1, s.restDaysUsedThisWeek)
    }

    @Test fun twoMissesInOneWeekBreakWithOneRestDay() {
        // Skipped Tue and Mon of this week.
        assertEquals(1, streak(days(0, 3, 4)).current)
    }

    @Test fun zeroRestDaysIsStrict() {
        assertEquals(1, streak(days(0, 2, 3), rest = 0).current)
    }

    @Test fun shortSessionsBelowMinimumDontCount() {
        assertEquals(0, streak(days(0, 1, minutes = 5)).current)
    }

    @Test fun bestStreakSurvivesABreak() {
        val daily = days(0, 1) + days(10, 11, 12, 13, 14, 15)
        val s = streak(daily, rest = 0)
        assertEquals(2, s.current)
        assertEquals(6, s.best)
    }

    @Test fun weekRangeRespectsWeekStart() {
        assertEquals(LocalDate.of(2026, 10, 26), StudyMath.weekRange(today, DayOfWeek.MONDAY).start)
        assertEquals(LocalDate.of(2026, 10, 25), StudyMath.weekRange(today, DayOfWeek.SUNDAY).start)
    }

    @Test fun formatting() {
        assertEquals("1h 5m", StudyMath.formatDuration(3900))
        assertEquals("2h", StudyMath.formatDuration(7200, short = true))
        assertEquals("25:00", StudyMath.formatClock(25 * 60_000L))
        assertEquals("1:00:05", StudyMath.formatClock(3_605_000L))
    }

    @Test fun levelsProgress() {
        assertEquals(1, Levels.forSeconds(0).level)
        assertEquals(2, Levels.forSeconds(3600).level)
        val l = Levels.forSeconds(2 * 3600)
        assertEquals(2, l.level)
        assertEquals(0.5f, l.progress, 0.01f)
        assertTrue(Levels.forSeconds(5000L * 3600).level > 27) // keeps growing past the table
    }

    private fun millis(d: LocalDate, hour: Int = 12) = d.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun weeklyTimeGoalProgress() {
        val goal = Goal(id = 1, title = "", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = 120,
            createdAt = millis(today.minusWeeks(3)))
        val sessions = listOf(
            StudySession(startedAt = millis(today), durationSec = 3600),
            StudySession(startedAt = millis(today.minusDays(2)), durationSec = 1800), // Monday, same week
            StudySession(startedAt = millis(today.minusDays(7)), durationSec = 7200), // last week: met
        )
        val p = GoalMath.progress(goal, sessions, emptyList(), today, DayOfWeek.MONDAY, zone)
        assertEquals(90, p.current)
        assertFalse(p.met)
        assertEquals(1, p.recentHits)
        assertEquals(3, p.recentPeriods)
    }

    @Test fun itemGoalCountsFinishedOfType() {
        val goal = Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 2, itemType = ItemType.BOOK,
            createdAt = millis(today.minusDays(40)))
        val items = listOf(
            LearningItem(type = ItemType.BOOK, title = "A", status = ItemStatus.DONE, completedAt = millis(today)),
            LearningItem(type = ItemType.PODCAST, title = "B", status = ItemStatus.DONE, completedAt = millis(today)),
            LearningItem(type = ItemType.BOOK, title = "C", status = ItemStatus.ACTIVE),
        )
        assertEquals(1, GoalMath.progress(goal, emptyList(), items, today, DayOfWeek.MONDAY, zone).current)
    }

    @Test fun milestonesUnlockFromContext() {
        val sessions = List(12) { StudySession(startedAt = millis(today.minusDays(it.toLong()), hour = 23), durationSec = 3600, note = "x", subjectId = (it % 3).toLong()) }
        val ctx = MilestoneContext.from(sessions, emptyList(), bestStreak = 12, goalWins = 0, zone = zone)
        val got = Milestones.all.filter { it.achieved(ctx) }.map { it.key }.toSet()
        assertTrue("first_session" in got)
        assertTrue("hours_10" in got)
        assertTrue("streak_7" in got)
        assertTrue("deep_60" in got)
        assertTrue("reflect_10" in got)
        assertTrue("variety_3" in got)
        assertTrue("night_owl" in got)
        assertFalse("hours_25" in got)
        assertFalse("early_bird" in got)
    }

    @Test fun milestoneKeysAreUnique() {
        assertEquals(Milestones.all.size, Milestones.all.map { it.key }.toSet().size)
    }
}
