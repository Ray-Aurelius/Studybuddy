package com.rayaurelius.studybuddy.domain

import com.rayaurelius.studybuddy.data.db.ItemStatus
import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem
import com.rayaurelius.studybuddy.data.db.StudySession
import java.time.Instant
import java.time.ZoneId

/** Everything a milestone can be judged on, computed once per data change. */
data class MilestoneContext(
    val totalSeconds: Long,
    val sessions: Int,
    val longestSessionSec: Long,
    val bestStreak: Int,
    val doneByType: Map<ItemType, Int>,
    val reflections: Int,
    val subjectsStudied: Int,
    val earlyBird: Boolean,
    val nightOwl: Boolean,
    val goalWins: Int,
) {
    companion object {
        fun from(
            sessions: List<StudySession>,
            items: List<LearningItem>,
            bestStreak: Int,
            goalWins: Int,
            zone: ZoneId,
        ): MilestoneContext {
            val hours = sessions.map { Instant.ofEpochMilli(it.startedAt).atZone(zone).hour }
            return MilestoneContext(
                totalSeconds = sessions.sumOf { it.durationSec },
                sessions = sessions.size,
                longestSessionSec = sessions.maxOfOrNull { it.durationSec } ?: 0,
                bestStreak = bestStreak,
                doneByType = items.filter { it.status == ItemStatus.DONE }.groupingBy { it.type }.eachCount(),
                reflections = sessions.count { it.note.isNotBlank() },
                subjectsStudied = sessions.mapNotNull { it.subjectId }.distinct().size,
                earlyBird = hours.any { it in 4..6 },
                nightOwl = hours.any { it >= 22 || it < 2 },
                goalWins = goalWins,
            )
        }
    }
}

enum class MilestoneCategory(val label: String) {
    TIME("Time invested"), STREAK("Consistency"), LIBRARY("Library"), CRAFT("Learning craft"), GOALS("Goals")
}

data class Milestone(
    val key: String,
    val emoji: String,
    val title: String,
    val description: String,
    val category: MilestoneCategory,
    val target: Long,
    val measure: (MilestoneContext) -> Long,
) {
    fun progress(ctx: MilestoneContext): Long = measure(ctx).coerceAtMost(target)
    fun achieved(ctx: MilestoneContext): Boolean = measure(ctx) >= target
}

object Milestones {
    private fun hours(h: Int, emoji: String, title: String) = Milestone(
        "hours_$h", emoji, title, "Study for $h hour${if (h == 1) "" else "s"} in total",
        MilestoneCategory.TIME, h.toLong(),
    ) { it.totalSeconds / 3600 }

    private fun streak(d: Int, emoji: String, title: String) = Milestone(
        "streak_$d", emoji, title, "Reach a $d-day streak", MilestoneCategory.STREAK, d.toLong(),
    ) { it.bestStreak.toLong() }

    private fun done(type: ItemType, n: Int, emoji: String, title: String) = Milestone(
        "${type.name.lowercase()}_$n", emoji, title,
        "Finish $n ${if (n == 1) type.label.lowercase() else type.plural.lowercase()}",
        MilestoneCategory.LIBRARY, n.toLong(),
    ) { (it.doneByType[type] ?: 0).toLong() }

    val all: List<Milestone> = listOf(
        Milestone("first_session", "🌱", "First Step", "Log your first study session", MilestoneCategory.TIME, 1) { it.sessions.toLong() },
        hours(1, "⏱️", "Warming Up"),
        hours(10, "🔟", "Ten-Hour Club"),
        hours(25, "📈", "Building Momentum"),
        hours(50, "🧠", "Half-Century"),
        hours(100, "💯", "Centurion"),
        hours(250, "🏔️", "Summit Seeker"),
        hours(500, "🌋", "Unstoppable"),
        hours(1000, "👑", "Thousand-Hour Mind"),

        streak(3, "🔥", "Spark"),
        streak(7, "📅", "One Full Week"),
        streak(14, "⚡", "Fortnight Focus"),
        streak(30, "🌙", "Month of Mastery"),
        streak(60, "🌟", "Habit Formed"),
        streak(100, "🏆", "Hundred Days"),
        streak(365, "🌍", "Year-Round Learner"),

        done(ItemType.BOOK, 1, "📖", "First Chapter Closed"),
        done(ItemType.BOOK, 5, "📚", "Bookworm"),
        done(ItemType.BOOK, 12, "🗓️", "A Book a Month"),
        done(ItemType.BOOK, 25, "🏛️", "Personal Library"),
        done(ItemType.BOOK, 50, "🗝️", "Bibliophile"),
        done(ItemType.PODCAST, 1, "🎧", "Tuned In"),
        done(ItemType.PODCAST, 10, "🎙️", "Ear Learner"),
        done(ItemType.PODCAST, 50, "📻", "Audio Scholar"),
        done(ItemType.SEMINAR, 1, "🎤", "In the Room"),
        done(ItemType.SEMINAR, 5, "🤝", "Networked Mind"),
        done(ItemType.SEMINAR, 20, "🎟️", "Front Row Regular"),
        done(ItemType.COURSE, 1, "🎓", "Course Complete"),
        done(ItemType.COURSE, 5, "📜", "Self-Made Curriculum"),
        Milestone("renaissance", "🎨", "Renaissance Learner", "Finish a book, a podcast, a seminar and a course",
            MilestoneCategory.LIBRARY, 4) { ctx ->
            listOf(ItemType.BOOK, ItemType.PODCAST, ItemType.SEMINAR, ItemType.COURSE).count { (ctx.doneByType[it] ?: 0) > 0 }.toLong()
        },

        Milestone("deep_60", "🌊", "Deep Work", "Complete a single 60-minute session", MilestoneCategory.CRAFT, 60) { it.longestSessionSec / 60 },
        Milestone("deep_120", "🤿", "Flow State", "Complete a single 2-hour session", MilestoneCategory.CRAFT, 120) { it.longestSessionSec / 60 },
        Milestone("reflect_10", "✍️", "Reflective Learner", "Write 10 session reflections", MilestoneCategory.CRAFT, 10) { it.reflections.toLong() },
        Milestone("reflect_50", "🪞", "Metacognition Master", "Write 50 session reflections", MilestoneCategory.CRAFT, 50) { it.reflections.toLong() },
        Milestone("variety_3", "🧩", "Cross-Trainer", "Study 3 different subjects", MilestoneCategory.CRAFT, 3) { it.subjectsStudied.toLong() },
        Milestone("variety_6", "🌈", "Polymath Path", "Study 6 different subjects", MilestoneCategory.CRAFT, 6) { it.subjectsStudied.toLong() },
        Milestone("sessions_100", "🧱", "Brick by Brick", "Log 100 sessions", MilestoneCategory.CRAFT, 100) { it.sessions.toLong() },
        Milestone("early_bird", "🌅", "Early Bird", "Start a session between 4 and 7 am", MilestoneCategory.CRAFT, 1) { if (it.earlyBird) 1 else 0 },
        Milestone("night_owl", "🦉", "Night Owl", "Start a session after 10 pm", MilestoneCategory.CRAFT, 1) { if (it.nightOwl) 1 else 0 },

        Milestone("goal_1", "🎯", "On Target", "Hit a goal for the first time", MilestoneCategory.GOALS, 1) { it.goalWins.toLong() },
        Milestone("goal_10", "🏹", "Sharpshooter", "Hit your goals 10 times", MilestoneCategory.GOALS, 10) { it.goalWins.toLong() },
        Milestone("goal_50", "🚀", "Goal Machine", "Hit your goals 50 times", MilestoneCategory.GOALS, 50) { it.goalWins.toLong() },
    )

    val byKey = all.associateBy { it.key }
}
