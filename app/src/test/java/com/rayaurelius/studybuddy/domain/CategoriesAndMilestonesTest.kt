package com.rayaurelius.studybuddy.domain

import com.rayaurelius.studybuddy.data.db.CustomCategory
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

class CategoriesAndMilestonesTest {
    private val zone = ZoneId.of("America/Denver")
    private val today = LocalDate.of(2026, 10, 28)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val workshop = CustomCategory(id = 7, name = "Workshop", emoji = "🛠️", unitLabel = "sessions")

    private fun done(type: ItemType, n: Int, categoryId: Long? = null) =
        List(n) { LearningItem(type = type, title = "x$it", status = ItemStatus.DONE, completedAt = now, categoryId = categoryId) }

    private fun ctx(items: List<LearningItem> = emptyList(), hours: Long = 0) = MilestoneContext.from(
        if (hours > 0) listOf(StudySession(startedAt = now, durationSec = hours * 3600)) else emptyList(),
        items, bestStreak = 0, goalWins = 0, zone = zone,
    )

    private fun unlocked(c: MilestoneContext) = Milestones.all.filter { it.achieved(c) }.map { it.key }.toSet()

    @Test fun hourMilestonesIncludeAllRequestedTiers() {
        listOf(100, 250, 500, 1000, 5000).forEach { h -> assertTrue("hours_$h", "hours_$h" in Milestones.byKey) }
        val got = unlocked(ctx(hours = 5000))
        assertTrue("hours_5000" in got)
        assertFalse("hours_5000" in unlocked(ctx(hours = 4999)))
    }

    @Test fun bookMilestonesGoToAThousand() {
        listOf(1, 5, 12, 25, 50, 100, 250, 500, 1000).forEach { n -> assertTrue("book_$n", "book_$n" in Milestones.byKey) }
        val got = unlocked(ctx(done(ItemType.BOOK, 250)))
        assertTrue("book_250" in got)
        assertFalse("book_500" in got)
        assertTrue("book_1000" in unlocked(ctx(done(ItemType.BOOK, 1000))))
    }

    @Test fun audiobooksCountTowardBookMilestones() {
        val c = ctx(done(ItemType.BOOK, 60) + done(ItemType.AUDIOBOOK, 40))
        assertEquals(100, c.booksFinished)
        val got = unlocked(c)
        assertTrue("book_100" in got)
        assertTrue("audiobook_10" in got)
        assertFalse("audiobook_50" in got)
    }

    @Test fun customCategoryMilestone() {
        assertTrue("custom_1" in unlocked(ctx(done(ItemType.CUSTOM, 1, workshop.id))))
    }

    @Test fun categoryLookupAndFallback() {
        val byId = mapOf(workshop.id to workshop)
        val item = LearningItem(type = ItemType.CUSTOM, title = "Pottery", categoryId = 7)
        val info = Categories.of(item, byId)
        assertEquals("Workshop", info.label)
        assertEquals("sessions", info.unit)
        assertEquals("3 workshops", info.count(3))
        // Deleted category: item shows under "Other" instead of vanishing.
        assertEquals(Categories.OTHER, Categories.of(item.copy(categoryId = null), byId))
        assertEquals("AUDIOBOOK", Categories.all(listOf(workshop))[1].key)
        assertEquals("c7", Categories.all(listOf(workshop)).last().key)
    }

    @Test fun pluralize() {
        assertEquals("Workshops", Categories.pluralize("Workshop"))
        assertEquals("Classes", Categories.pluralize("Class"))
        assertEquals("Documentaries", Categories.pluralize("Documentary"))
        assertEquals("Essays", Categories.pluralize("Essay"))
        assertEquals("Lectures", Categories.pluralize("Lectures"))
    }

    @Test fun customCategoryGoalCountsOnlyThatCategory() {
        val goal = Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 2,
            itemType = ItemType.CUSTOM, categoryId = 7, createdAt = now)
        val items = done(ItemType.CUSTOM, 1, 7) + done(ItemType.CUSTOM, 3, 8) + done(ItemType.BOOK, 2)
        val p = GoalMath.progress(goal, emptyList(), items, today, DayOfWeek.MONDAY, zone)
        assertEquals(1, p.current)
        assertEquals("2 workshops a month", GoalMath.describe(goal, Categories.custom(workshop)))
    }

    @Test fun levelsKeepTitlesAt5000Hours() {
        val l = Levels.forSeconds(5000L * 3600)
        assertTrue(l.level >= 53)
        assertEquals("Legend", l.title)
    }
}
