package com.rayaurelius.sophinity

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.domain.Milestones
import com.rayaurelius.sophinity.ui.theme.Brand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

object DemoData {
    /** Seeds a realistic learner history; returns the in-progress audiobook's id. */
    suspend fun seed(): Long {
        val app = ApplicationProvider.getApplicationContext<SophinityApp>()
        val repo = app.container.repo
        // Settings live in a process-wide DataStore, so reset fully: earlier tests may have changed them.
        app.container.settingsStore.update { com.rayaurelius.sophinity.data.AppSettings(onboarded = true, seenTitleRank = 8) }
        // The timer is process-wide too; a test that started it must not leave it running for the next.
        app.container.timer.discard()
        // Spanish keeps an emoji, as saved by versions before 1.5, to show it maps to a glyph.
        val subjects = listOf("🗣️" to "Spanish", "gear" to "Programming", "scales" to "Personal finance", "butterfly" to "Psychology")
            .mapIndexed { i, (e, n) -> repo.addSubject(Subject(name = n, icon = e, color = Brand.subjectPalette[i])) }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val rnd = Random(7)
        val notes = listOf("Subjunctive after 'ojalá'", "Big-O of hash map lookups", "Index funds vs. active funds",
            "Spacing effect beats cramming", "", "", "Recursion = base case + smaller problem")
        for (d in 0..120) {
            if (d > 14 && rnd.nextFloat() < 0.3f) continue
            if (d in 1..14 && d == 5) continue
            val sessions = 1 + rnd.nextInt(2)
            repeat(sessions) {
                val start = today.minusDays(d.toLong()).atTime(7 + rnd.nextInt(14), rnd.nextInt(60)).atZone(zone).toInstant().toEpochMilli()
                repo.addSession(StudySession(subjectId = subjects[rnd.nextInt(subjects.size)], startedAt = start,
                    durationSec = (15 + rnd.nextInt(55)) * 60L, note = notes.random(rnd), focus = 2 + rnd.nextInt(4)))
            }
        }
        val now = System.currentTimeMillis()
        val handbookId = repo.saveItem(LearningItem(type = ItemType.BOOK, title = "The Curious Learner's Handbook", creator = "M. Alvarez", subjectId = subjects[3], totalUnits = 320, progressUnits = 212))
        repo.saveItem(LearningItem(type = ItemType.COURSE, title = "Intro to Data Structures", creator = "Open University", subjectId = subjects[1], totalUnits = 12, progressUnits = 5))
        val workshop = repo.saveCategory(CustomCategory(name = "Workshop", icon = "anvil", unitLabel = "sessions"))
        val audiobookId = repo.saveItem(LearningItem(type = ItemType.AUDIOBOOK, title = "Habits of Curious People", creator = "L. Park", url = "https://audiobooks.example.com/habits-of-curious-people", subjectId = subjects[3],
            totalUnits = 24, progressUnits = 9, runtimeMin = 9 * 60 + 30, priorSec = 2 * 3600L, playbackSpeed = 1.5f))
        repeat(3) { d ->
            repo.addSession(StudySession(subjectId = subjects[3], itemId = audiobookId,
                startedAt = today.minusDays(d + 1L).atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), durationSec = 40 * 60L))
        }
        repo.saveItem(LearningItem(type = ItemType.CUSTOM, categoryId = workshop, title = "Weekend Pottery Workshop", creator = "Clay Studio", totalUnits = 6, progressUnits = 4))
        repo.saveItem(LearningItem(type = ItemType.AUDIOBOOK, title = "A Short History of Numbers", status = ItemStatus.DONE, rating = 4, completedAt = now - 86_400_000L * 6))
        repo.saveItem(LearningItem(type = ItemType.PODCAST, title = "Money Matters Weekly", creator = "J. Chen", url = "https://podcasts.example.org/money-matters", subjectId = subjects[2], totalUnits = 40, progressUnits = 18))
        repo.saveItem(LearningItem(type = ItemType.BOOK, title = "Spanish in 90 Days", creator = "R. Ortega", status = ItemStatus.DONE, rating = 5, takeaway = "Speak from day one.", completedAt = now - 86_400_000L * 3))
        repo.saveItem(LearningItem(type = ItemType.SEMINAR, title = "Local Investing Meetup", status = ItemStatus.DONE, rating = 4, completedAt = now - 86_400_000L * 10))
        repo.saveItem(LearningItem(type = ItemType.BOOK, title = "Thinking in Systems Primer", status = ItemStatus.WANT))
        val day = 86_400_000L
        repo.saveNote(com.rayaurelius.sophinity.data.db.Note(title = "Questions to try on myself", itemId = audiobookId,
            body = "What did I wonder about today?\nWhich question did I let go unanswered?\nWho could I ask?"), now - day / 2)
        repo.saveNote(com.rayaurelius.sophinity.data.db.Note(itemId = handbookId,
            body = "Spacing beats cramming\nReview after 1 day, 3 days, then a week. Chapter 7 has the forgetting curve."), now - day * 2)
        repo.saveNote(com.rayaurelius.sophinity.data.db.Note(title = "Ideas for the pottery class",
            body = "Bring photos of the bowls I liked. Ask about glaze temperatures."), now - day * 5)
        repo.saveNote(com.rayaurelius.sophinity.data.db.Note(title = "Ch. 2: fixed vs. growth", itemId = handbookId,
            body = "Praise the process, not the person."), now - day * 9)
        val created = today.minusWeeks(10).atStartOfDay(zone).toInstant().toEpochMilli()
        repo.saveGoal(Goal(title = "5 hours a week", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = 300, createdAt = created))
        repo.saveGoal(Goal(title = "30 minutes a day", kind = GoalKind.TIME, period = GoalPeriod.DAILY, target = 30, createdAt = created))
        repo.saveGoal(Goal(title = "2 books a month", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 2, itemType = ItemType.BOOK, createdAt = created))
        val snap = repo.snapshot.first()
        val got = Milestones.all.filter { it.achieved(snap.milestoneContext) }.map { it.key }
        repo.unlock(got)
        repo.markMilestonesSeen(got)
            return audiobookId
    }
}
