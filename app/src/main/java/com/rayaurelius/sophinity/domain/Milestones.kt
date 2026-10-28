package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import java.time.Instant
import java.time.ZoneId

/** Everything a milestone can be judged on, computed once per data change. */
data class MilestoneContext(
    val totalSeconds: Long,
    val sessions: Int,
    val longestSessionSec: Long,
    val bestStreak: Int,
    val doneByType: Map<ItemType, Int>,
    /** Finished custom-category items. */
    val doneCustom: Int,
    val reflections: Int,
    val subjectsStudied: Int,
    val earlyBird: Boolean,
    val nightOwl: Boolean,
    val goalWins: Int,
) {
    /** Books in any format: print, e-book and audiobook all count as reading a book. */
    val booksFinished: Int get() = (doneByType[ItemType.BOOK] ?: 0) + (doneByType[ItemType.AUDIOBOOK] ?: 0)

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
                doneCustom = items.count { it.status == ItemStatus.DONE && it.type == ItemType.CUSTOM },
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
    TIME("Time invested"), STREAK("Consistency"), LIBRARY("Library"), CRAFT("The learner's craft"), GOALS("Goals")
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
        "hours_$h", emoji, title, "Learn for $h hour${if (h == 1) "" else "s"} in total",
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

    // Keys stay "book_N" so badges already unlocked keep their dates.
    private fun books(n: Int, emoji: String, title: String) = Milestone(
        "book_$n", emoji, title,
        if (n == 1) "Finish your first book (print, e-book or audio)" else "Finish $n books (print, e-book or audio)",
        MilestoneCategory.LIBRARY, n.toLong(),
    ) { it.booksFinished.toLong() }

    val all: List<Milestone> = listOf(
        Milestone("first_session", "🌱", "First Step", "Log your first learning session", MilestoneCategory.TIME, 1) { it.sessions.toLong() },
        hours(1, "🔥", "Spark Kindled"),
        hours(10, "🏺", "Agora Regular"),
        hours(25, "🪜", "Steady Ascent"),
        hours(50, "🧠", "Half-Century"),
        hours(100, "🛡️", "Hundred-Hour Hero"),
        hours(250, "🏔️", "Olympian Climb"),
        hours(500, "💪", "Herculean"),
        hours(1000, "⛵", "Odyssey"),
        hours(5000, "🌌", "Elysian"),

        streak(3, "🕯️", "First Flame"),
        streak(7, "📜", "Seven Sages"),
        streak(14, "⚡", "Fortnight Focus"),
        streak(30, "🌙", "Full Moon"),
        streak(60, "🌟", "Ethos Formed"),
        streak(100, "🏃", "Marathon"),
        streak(365, "☀️", "Circuit of Helios"),

        books(1, "📖", "First Scroll"),
        books(5, "📚", "Bookworm"),
        books(12, "🗓️", "A Book a Month"),
        books(25, "🗃️", "Personal Library"),
        books(50, "🗝️", "Bibliophile"),
        books(100, "🏰", "Century of Stories"),
        books(250, "🌳", "Tree of Knowledge"),
        books(500, "🔭", "Lifelong Reader"),
        books(1000, "🏛️", "Library of Alexandria"),
        done(ItemType.AUDIOBOOK, 1, "🎧", "Homeric Listener"),
        done(ItemType.AUDIOBOOK, 10, "🚗", "Commute Scholar"),
        done(ItemType.AUDIOBOOK, 50, "🎼", "Rhapsode"),
        done(ItemType.PODCAST, 1, "📡", "Tuned In"),
        done(ItemType.PODCAST, 10, "🚶", "Peripatetic Listener"),
        done(ItemType.PODCAST, 50, "📻", "Audio Scholar"),
        done(ItemType.SEMINAR, 1, "🍇", "Symposium Guest"),
        done(ItemType.SEMINAR, 5, "🤝", "Academy Regular"),
        done(ItemType.SEMINAR, 20, "🏛️", "Lyceum Fixture"),
        done(ItemType.COURSE, 1, "🎓", "Course Complete"),
        done(ItemType.COURSE, 5, "📜", "Paideia"),
        Milestone("custom_1", "🧭", "Your Own Path", "Finish something in a category you created", MilestoneCategory.LIBRARY, 1) {
            it.doneCustom.toLong()
        },
        Milestone("renaissance", "🎨", "Polymath", "Finish a book, a podcast, a seminar and a course",
            MilestoneCategory.LIBRARY, 4) { ctx ->
            (listOf(ItemType.PODCAST, ItemType.SEMINAR, ItemType.COURSE).count { (ctx.doneByType[it] ?: 0) > 0 } +
                if (ctx.booksFinished > 0) 1 else 0).toLong()
        },

        Milestone("deep_60", "🌊", "Deep Work", "Complete a single 60-minute session", MilestoneCategory.CRAFT, 60) { it.longestSessionSec / 60 },
        Milestone("deep_120", "🤿", "Contemplation", "Complete a single 2-hour session", MilestoneCategory.CRAFT, 120) { it.longestSessionSec / 60 },
        Milestone("reflect_10", "✍️", "Know Thyself", "Write 10 session reflections", MilestoneCategory.CRAFT, 10) { it.reflections.toLong() },
        Milestone("reflect_50", "🪞", "Socratic Mind", "Write 50 session reflections", MilestoneCategory.CRAFT, 50) { it.reflections.toLong() },
        Milestone("variety_3", "🧩", "Cross-Trainer", "Learn across 3 different subjects", MilestoneCategory.CRAFT, 3) { it.subjectsStudied.toLong() },
        Milestone("variety_6", "🌈", "Many-Sided Mind", "Learn across 6 different subjects", MilestoneCategory.CRAFT, 6) { it.subjectsStudied.toLong() },
        Milestone("sessions_100", "🧱", "Stone by Stone", "Log 100 sessions", MilestoneCategory.CRAFT, 100) { it.sessions.toLong() },
        Milestone("early_bird", "🌅", "Rosy-Fingered Dawn", "Start a session between 4 and 7 am", MilestoneCategory.CRAFT, 1) { if (it.earlyBird) 1 else 0 },
        Milestone("night_owl", "🦉", "Owl of Athena", "Start a session after 10 pm", MilestoneCategory.CRAFT, 1) { if (it.nightOwl) 1 else 0 },

        Milestone("goal_1", "🎯", "On Target", "Hit a goal for the first time", MilestoneCategory.GOALS, 1) { it.goalWins.toLong() },
        Milestone("goal_10", "🏹", "Artemis' Aim", "Hit your goals 10 times", MilestoneCategory.GOALS, 10) { it.goalWins.toLong() },
        Milestone("goal_50", "🏅", "Arete", "Hit your goals 50 times", MilestoneCategory.GOALS, 50) { it.goalWins.toLong() },
    )

    val byKey = all.associateBy { it.key }
}
