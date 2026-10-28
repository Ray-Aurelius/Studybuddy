package com.rayaurelius.studybuddy

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
import com.rayaurelius.studybuddy.data.db.CustomCategory
import com.rayaurelius.studybuddy.data.db.Goal
import com.rayaurelius.studybuddy.data.db.GoalKind
import com.rayaurelius.studybuddy.data.db.GoalPeriod
import com.rayaurelius.studybuddy.data.db.ItemStatus
import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem
import com.rayaurelius.studybuddy.data.db.StudySession
import com.rayaurelius.studybuddy.data.db.Subject
import com.rayaurelius.studybuddy.domain.Milestones
import com.rayaurelius.studybuddy.ui.theme.Brand
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

/**
 * Renders real app screens with seeded demo data to app/build/screenshots/.
 * Run: ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {

    private var audiobookId = 0L

    init { seed() }

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun seed() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<StudyBuddyApp>()
        val repo = app.container.repo
        app.container.settingsStore.update { it.copy(onboarded = true) }
        val subjects = listOf("🗣️" to "Spanish", "💻" to "Programming", "💰" to "Personal finance", "🧠" to "Psychology")
            .mapIndexed { i, (e, n) -> repo.addSubject(Subject(name = n, emoji = e, color = Brand.subjectPalette[i])) }
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
        repo.saveItem(LearningItem(type = ItemType.BOOK, title = "The Curious Learner's Handbook", creator = "M. Alvarez", subjectId = subjects[3], totalUnits = 320, progressUnits = 212))
        repo.saveItem(LearningItem(type = ItemType.COURSE, title = "Intro to Data Structures", creator = "Open University", subjectId = subjects[1], totalUnits = 12, progressUnits = 5))
        val workshop = repo.saveCategory(CustomCategory(name = "Workshop", emoji = "🛠️", unitLabel = "sessions"))
        audiobookId = repo.saveItem(LearningItem(type = ItemType.AUDIOBOOK, title = "Habits of Curious People", creator = "L. Park", subjectId = subjects[3],
            totalUnits = 24, progressUnits = 9, runtimeMin = 9 * 60 + 30, priorSec = 2 * 3600L, playbackSpeed = 1.5f))
        repeat(3) { d ->
            repo.addSession(StudySession(subjectId = subjects[3], itemId = audiobookId,
                startedAt = today.minusDays(d + 1L).atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), durationSec = 40 * 60L))
        }
        repo.saveItem(LearningItem(type = ItemType.CUSTOM, categoryId = workshop, title = "Weekend Pottery Workshop", creator = "Clay Studio", totalUnits = 6, progressUnits = 4))
        repo.saveItem(LearningItem(type = ItemType.AUDIOBOOK, title = "A Short History of Numbers", status = ItemStatus.DONE, rating = 4, completedAt = now - 86_400_000L * 6))
        repo.saveItem(LearningItem(type = ItemType.PODCAST, title = "Money Matters Weekly", creator = "J. Chen", subjectId = subjects[2], totalUnits = 40, progressUnits = 18))
        repo.saveItem(LearningItem(type = ItemType.BOOK, title = "Spanish in 90 Days", creator = "R. Ortega", status = ItemStatus.DONE, rating = 5, takeaway = "Speak from day one.", completedAt = now - 86_400_000L * 3))
        repo.saveItem(LearningItem(type = ItemType.SEMINAR, title = "Local Investing Meetup", status = ItemStatus.DONE, rating = 4, completedAt = now - 86_400_000L * 10))
        repo.saveItem(LearningItem(type = ItemType.BOOK, title = "Thinking in Systems Primer", status = ItemStatus.WANT))
        val created = today.minusWeeks(10).atStartOfDay(zone).toInstant().toEpochMilli()
        repo.saveGoal(Goal(title = "5 hours a week", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = 300, createdAt = created))
        repo.saveGoal(Goal(title = "30 minutes a day", kind = GoalKind.TIME, period = GoalPeriod.DAILY, target = 30, createdAt = created))
        repo.saveGoal(Goal(title = "2 books a month", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 2, itemType = ItemType.BOOK, createdAt = created))
        val snap = repo.snapshot.first()
        val got = Milestones.all.filter { it.achieved(snap.milestoneContext) }.map { it.key }
        repo.unlock(got)
        repo.markMilestonesSeen(got)
    }

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    ).performClick()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun screens() {
        shot("1_today")
        tab("Stats"); shot("2_stats")
        tab("Library"); shot("3_library")
        tab("Goals"); shot("4_goals")
        tab("Badges"); shot("5_badges")
    }

    @Test fun timerOnAudiobook() {
        val app = ApplicationProvider.getApplicationContext<StudyBuddyApp>()
        runBlocking { app.container.timer.start(com.rayaurelius.studybuddy.timer.TimerMode.STOPWATCH, null, audiobookId) }
        compose.mainClock.advanceTimeBy(1_000)
        shot("7_timer_audiobook")
    }

    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    @Test fun darkToday() {
        shot("6_today_dark")
    }
}
