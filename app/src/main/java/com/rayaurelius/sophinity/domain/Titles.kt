package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.R
/**
 * Earned titles: a ladder from Neophyte to Philosopher. Each rung requires specific laurels
 * (milestones) and every rung below it, so a title reflects breadth — time, reading,
 * consistency and reflection — not hours alone.
 */
data class LearnerTitle(
    val rank: Int,
    /** Stable id, e.g. "philosopher". */
    val id: String,
    val name: Txt,
    /** The Greek word itself, shown as-is in every language. */
    val greek: String,
    val meaning: Txt,
    /** Milestone keys that must all be unlocked. */
    val requires: List<String>,
) {
    /** Emblem glyph: the learner's tools as they grow, from seedling to the owl of Athena. */
    val glyph: String get() = when (id) {
        "neophyte" -> "sprout"
        "seeker" -> "lamp"
        "pupil" -> "tablet"
        "scribe" -> "quill"
        "rhetor" -> "kerykeion"
        "scholar" -> "scroll_stack"
        "sage" -> "herm"
        else -> "owl"
    }
}

data class TitleStatus(
    val current: LearnerTitle,
    val next: LearnerTitle?,
    /** For [next]: which required laurels are done. */
    val nextChecklist: List<Pair<Milestone, Boolean>>,
    /** When [current] was earned (the latest unlock among its gates and lower rungs), or null for the starting title. */
    val earnedAt: Long?,
)

object Titles {
    val all = listOf(
        LearnerTitle(1, "neophyte", Txt.Res(R.string.title_neophyte), "νεόφυτος", Txt.Res(R.string.title_neophyte_meaning), emptyList()),
        LearnerTitle(2, "seeker", Txt.Res(R.string.title_seeker), "ζητητής", Txt.Res(R.string.title_seeker_meaning), listOf("first_session")),
        LearnerTitle(3, "pupil", Txt.Res(R.string.title_pupil), "μαθητής", Txt.Res(R.string.title_pupil_meaning), listOf("hours_10", "streak_7")),
        LearnerTitle(4, "scribe", Txt.Res(R.string.title_scribe), "γραμματεύς", Txt.Res(R.string.title_scribe_meaning), listOf("book_1", "reflect_10")),
        LearnerTitle(5, "rhetor", Txt.Res(R.string.title_rhetor), "ῥήτωρ", Txt.Res(R.string.title_rhetor_meaning), listOf("hours_50", "goal_10")),
        LearnerTitle(6, "scholar", Txt.Res(R.string.title_scholar), "σχολαστικός", Txt.Res(R.string.title_scholar_meaning), listOf("hours_100", "streak_30", "renaissance")),
        LearnerTitle(7, "sage", Txt.Res(R.string.title_sage), "σοφός", Txt.Res(R.string.title_sage_meaning), listOf("hours_500", "book_50", "streak_100")),
        LearnerTitle(8, "philosopher", Txt.Res(R.string.title_philosopher), "φιλόσοφος", Txt.Res(R.string.title_philosopher_meaning), listOf("hours_1000", "book_100", "streak_365", "reflect_50")),
    )

    val highest get() = all.last()

    fun status(unlockedAt: Map<String, Long>): TitleStatus {
        var current = all.first()
        for (t in all.drop(1)) {
            if (t.requires.all { it in unlockedAt }) current = t else break
        }
        val next = all.getOrNull(current.rank)
        val checklist = next?.requires?.mapNotNull { key -> Milestones.byKey[key]?.let { it to (key in unlockedAt) } }.orEmpty()
        val gates = all.take(current.rank).flatMap { it.requires }
        val earnedAt = gates.mapNotNull { unlockedAt[it] }.maxOrNull()
        return TitleStatus(current, next, checklist, earnedAt)
    }
}
