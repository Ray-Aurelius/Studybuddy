package com.rayaurelius.sophinity.domain

import androidx.annotation.StringRes
import com.rayaurelius.sophinity.R
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

enum class MilestoneCategory(@StringRes val labelRes: Int) {
    TIME(R.string.mscat_time), STREAK(R.string.mscat_streak), LIBRARY(R.string.mscat_library), CRAFT(R.string.mscat_craft), GOALS(R.string.mscat_goals)
}

data class Milestone(
    val key: String,
    /** Glyph shown on the laurel medallion. */
    val glyph: String,
    val title: Txt,
    val description: Txt,
    val category: MilestoneCategory,
    val target: Long,
    val measure: (MilestoneContext) -> Long,
) {
    fun progress(ctx: MilestoneContext): Long = measure(ctx).coerceAtMost(target)
    fun achieved(ctx: MilestoneContext): Boolean = measure(ctx) >= target
}

object Milestones {
    private fun t(@StringRes id: Int) = Txt.Res(id)

    private fun hours(h: Int, glyph: String, @StringRes title: Int) = Milestone(
        "hours_$h", glyph, t(title), Txt.Plural(R.plurals.ms_desc_hours, h),
        MilestoneCategory.TIME, h.toLong(),
    ) { it.totalSeconds / 3600 }

    private fun streak(d: Int, glyph: String, @StringRes title: Int) = Milestone(
        "streak_$d", glyph, t(title), Txt.Plural(R.plurals.ms_desc_streak, d), MilestoneCategory.STREAK, d.toLong(),
    ) { it.bestStreak.toLong() }

    private fun done(type: ItemType, n: Int, glyph: String, @StringRes title: Int) = Milestone(
        "${type.name.lowercase()}_$n", glyph, t(title),
        Txt.Res(R.string.ms_desc_finish, listOf(Txt.Plural(type.countRes, n))),
        MilestoneCategory.LIBRARY, n.toLong(),
    ) { (it.doneByType[type] ?: 0).toLong() }

    // Keys stay "book_N" so badges already unlocked keep their dates.
    private fun books(n: Int, glyph: String, @StringRes title: Int) = Milestone(
        "book_$n", glyph, t(title), Txt.Plural(R.plurals.ms_desc_books, n),
        MilestoneCategory.LIBRARY, n.toLong(),
    ) { it.booksFinished.toLong() }

    val all: List<Milestone> = listOf(
        Milestone("first_session", "sprout", t(R.string.ms_first_session), t(R.string.ms_desc_first_session), MilestoneCategory.TIME, 1) { it.sessions.toLong() },
        hours(1, "flame", R.string.ms_hours_1),
        hours(10, "coin", R.string.ms_hours_10),
        hours(25, "steps", R.string.ms_hours_25),
        hours(50, "columns", R.string.ms_hours_50),
        hours(100, "shield", R.string.ms_hours_100),
        hours(250, "mountain", R.string.ms_hours_250),
        hours(500, "club", R.string.ms_hours_500),
        hours(1000, "ship", R.string.ms_hours_1000),
        hours(5000, "stars", R.string.ms_hours_5000),

        streak(3, "torch", R.string.ms_streak_3),
        streak(7, "tripod", R.string.ms_streak_7),
        streak(14, "eye", R.string.ms_streak_14),
        streak(30, "moon_full", R.string.ms_streak_30),
        streak(60, "anvil", R.string.ms_streak_60),
        streak(100, "stadium", R.string.ms_streak_100),
        streak(365, "helios", R.string.ms_streak_365),

        books(1, "scroll", R.string.ms_book_1),
        books(5, "scroll_stack", R.string.ms_book_5),
        books(12, "moon", R.string.ms_book_12),
        books(25, "capsa", R.string.ms_book_25),
        books(50, "key", R.string.ms_book_50),
        books(100, "papyrus", R.string.ms_book_100),
        books(250, "olive_tree", R.string.ms_book_250),
        books(500, "lamp", R.string.ms_book_500),
        books(1000, "lighthouse", R.string.ms_book_1000),
        done(ItemType.AUDIOBOOK, 1, "lyre", R.string.ms_audiobook_1),
        done(ItemType.AUDIOBOOK, 10, "wheel", R.string.ms_audiobook_10),
        done(ItemType.AUDIOBOOK, 50, "theatre", R.string.ms_audiobook_50),
        done(ItemType.PODCAST, 1, "waves", R.string.ms_podcast_1),
        done(ItemType.PODCAST, 10, "stoa", R.string.ms_podcast_10),
        done(ItemType.PODCAST, 50, "masks", R.string.ms_podcast_50),
        done(ItemType.SEMINAR, 1, "kylix", R.string.ms_seminar_1),
        done(ItemType.SEMINAR, 5, "olive", R.string.ms_seminar_5),
        done(ItemType.SEMINAR, 20, "ionic", R.string.ms_seminar_20),
        done(ItemType.COURSE, 1, "tablet", R.string.ms_course_1),
        done(ItemType.COURSE, 5, "alpha_omega", R.string.ms_course_5),
        Milestone("custom_1", "meander", t(R.string.ms_custom_1), t(R.string.ms_desc_custom_1), MilestoneCategory.LIBRARY, 1) {
            it.doneCustom.toLong()
        },
        Milestone("renaissance", "amphora", t(R.string.ms_renaissance), t(R.string.ms_desc_renaissance),
            MilestoneCategory.LIBRARY, 4) { ctx ->
            (listOf(ItemType.PODCAST, ItemType.SEMINAR, ItemType.COURSE).count { (ctx.doneByType[it] ?: 0) > 0 } +
                if (ctx.booksFinished > 0) 1 else 0).toLong()
        },

        Milestone("deep_60", "trident", t(R.string.ms_deep_60), Txt.Plural(R.plurals.ms_desc_session_minutes, 60), MilestoneCategory.CRAFT, 60) { it.longestSessionSec / 60 },
        Milestone("deep_120", "eye", t(R.string.ms_deep_120), Txt.Plural(R.plurals.ms_desc_session_minutes, 120), MilestoneCategory.CRAFT, 120) { it.longestSessionSec / 60 },
        Milestone("reflect_10", "delphi_e", t(R.string.ms_reflect_10), Txt.Plural(R.plurals.ms_desc_reflections, 10), MilestoneCategory.CRAFT, 10) { it.reflections.toLong() },
        Milestone("reflect_50", "herm", t(R.string.ms_reflect_50), Txt.Plural(R.plurals.ms_desc_reflections, 50), MilestoneCategory.CRAFT, 50) { it.reflections.toLong() },
        Milestone("variety_3", "dyad", t(R.string.ms_variety_3), Txt.Plural(R.plurals.ms_desc_subjects, 3), MilestoneCategory.CRAFT, 3) { it.subjectsStudied.toLong() },
        Milestone("variety_6", "mosaic", t(R.string.ms_variety_6), Txt.Plural(R.plurals.ms_desc_subjects, 6), MilestoneCategory.CRAFT, 6) { it.subjectsStudied.toLong() },
        Milestone("sessions_100", "bricks", t(R.string.ms_sessions_100), Txt.Plural(R.plurals.ms_desc_sessions, 100), MilestoneCategory.CRAFT, 100) { it.sessions.toLong() },
        Milestone("early_bird", "sunrise", t(R.string.ms_early_bird), t(R.string.ms_desc_early_bird), MilestoneCategory.CRAFT, 1) { if (it.earlyBird) 1 else 0 },
        Milestone("night_owl", "owl", t(R.string.ms_night_owl), t(R.string.ms_desc_night_owl), MilestoneCategory.CRAFT, 1) { if (it.nightOwl) 1 else 0 },

        Milestone("goal_1", "target", t(R.string.ms_goal_1), t(R.string.ms_desc_goal_1), MilestoneCategory.GOALS, 1) { it.goalWins.toLong() },
        Milestone("goal_10", "bow", t(R.string.ms_goal_10), Txt.Plural(R.plurals.ms_desc_goal_wins, 10), MilestoneCategory.GOALS, 10) { it.goalWins.toLong() },
        Milestone("goal_50", "wreath", t(R.string.ms_goal_50), Txt.Plural(R.plurals.ms_desc_goal_wins, 50), MilestoneCategory.GOALS, 50) { it.goalWins.toLong() },
    )

    val byKey = all.associateBy { it.key }
}
