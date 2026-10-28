package com.rayaurelius.sophinity.domain

/**
 * Earned titles: a ladder from Neophyte to Philosopher. Each rung requires specific laurels
 * (milestones) and every rung below it, so a title reflects breadth — time, reading,
 * consistency and reflection — not hours alone.
 */
data class LearnerTitle(
    val rank: Int,
    val name: String,
    val greek: String,
    val meaning: String,
    /** Milestone keys that must all be unlocked. */
    val requires: List<String>,
)

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
        LearnerTitle(1, "Neophyte", "νεόφυτος", "newly planted", emptyList()),
        LearnerTitle(2, "Seeker", "ζητητής", "one who seeks", listOf("first_session")),
        LearnerTitle(3, "Pupil", "μαθητής", "learner", listOf("hours_10", "streak_7")),
        LearnerTitle(4, "Scribe", "γραμματεύς", "keeper of words", listOf("book_1", "reflect_10")),
        LearnerTitle(5, "Rhetor", "ῥήτωρ", "one who speaks with purpose", listOf("hours_50", "goal_10")),
        LearnerTitle(6, "Scholar", "σχολαστικός", "devoted to learning", listOf("hours_100", "streak_30", "renaissance")),
        LearnerTitle(7, "Sage", "σοφός", "wise one", listOf("hours_500", "book_50", "streak_100")),
        LearnerTitle(8, "Philosopher", "φιλόσοφος", "lover of wisdom", listOf("hours_1000", "book_100", "streak_365", "reflect_50")),
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
