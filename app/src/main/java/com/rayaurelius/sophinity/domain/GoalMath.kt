package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class GoalProgress(
    val goal: Goal,
    val current: Int,
    val target: Int,
    val range: DateRange,
    /** Periodic goals: how many of the recent periods were met (e.g. 5 of last 8 weeks). */
    val recentHits: Int,
    val recentPeriods: Int,
    val daysLeft: Int,
) {
    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    val met: Boolean get() = current >= target
}

object GoalMath {

    fun rangeFor(goal: Goal, today: LocalDate, weekStart: DayOfWeek, zone: ZoneId): DateRange = when (goal.period) {
        GoalPeriod.DAILY -> StudyMath.dayRange(today)
        GoalPeriod.WEEKLY -> StudyMath.weekRange(today, weekStart)
        GoalPeriod.MONTHLY -> StudyMath.monthRange(today)
        GoalPeriod.BY_DATE -> DateRange(
            StudyMath.dayOf(goal.createdAt, zone),
            LocalDate.ofEpochDay(goal.deadlineEpochDay ?: today.toEpochDay()).plusDays(1),
        )
    }

    private fun previousRange(r: DateRange, period: GoalPeriod): DateRange = when (period) {
        GoalPeriod.DAILY -> StudyMath.dayRange(r.start.minusDays(1))
        GoalPeriod.WEEKLY -> DateRange(r.start.minusDays(7), r.start)
        GoalPeriod.MONTHLY -> DateRange(r.start.minusMonths(1), r.start)
        GoalPeriod.BY_DATE -> r
    }

    private fun amountIn(
        goal: Goal,
        range: DateRange,
        sessions: List<StudySession>,
        items: List<LearningItem>,
        zone: ZoneId,
    ): Int = when (goal.kind) {
        GoalKind.TIME -> (sessions.asSequence()
            .filter { goal.subjectId == null || it.subjectId == goal.subjectId }
            .filter { StudyMath.dayOf(it.startedAt, zone) in range }
            .sumOf { it.durationSec } / 60).toInt()
        GoalKind.ITEMS -> items.count {
            it.status == ItemStatus.DONE && it.completedAt != null &&
                (goal.itemType == null || (it.type == goal.itemType &&
                    (goal.itemType != com.rayaurelius.sophinity.data.db.ItemType.CUSTOM || it.categoryId == goal.categoryId))) &&
                StudyMath.dayOf(it.completedAt, zone) in range
        }
    }

    fun progress(
        goal: Goal,
        sessions: List<StudySession>,
        items: List<LearningItem>,
        today: LocalDate,
        weekStart: DayOfWeek,
        zone: ZoneId,
        lookback: Int = 8,
    ): GoalProgress {
        val range = rangeFor(goal, today, weekStart, zone)
        val current = amountIn(goal, range, sessions, items, zone)
        var hits = 0
        var periods = 0
        if (goal.period != GoalPeriod.BY_DATE) {
            val created = StudyMath.dayOf(goal.createdAt, zone)
            var r = previousRange(range, goal.period)
            while (periods < lookback && r.endExclusive.isAfter(created)) {
                periods++
                if (amountIn(goal, r, sessions, items, zone) >= goal.target) hits++
                r = previousRange(r, goal.period)
            }
        }
        val daysLeft = (range.endExclusive.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
        return GoalProgress(goal, current, goal.target, range, hits, periods, daysLeft)
    }

    /** Total number of goal "wins" in history — used for milestones. */
    fun totalWins(
        goals: List<Goal>,
        sessions: List<StudySession>,
        items: List<LearningItem>,
        today: LocalDate,
        weekStart: DayOfWeek,
        zone: ZoneId,
    ): Int = goals.sumOf { goal ->
        if (goal.period == GoalPeriod.BY_DATE) {
            if (amountIn(goal, rangeFor(goal, today, weekStart, zone), sessions, items, zone) >= goal.target) 1 else 0
        } else {
            val created = StudyMath.dayOf(goal.createdAt, zone)
            var r = rangeFor(goal, today, weekStart, zone)
            var wins = 0
            var guard = 0
            while (r.endExclusive.isAfter(created) && guard++ < 3700) {
                if (amountIn(goal, r, sessions, items, zone) >= goal.target) wins++
                r = previousRange(r, goal.period)
            }
            wins
        }
    }

    fun describe(goal: Goal, category: CategoryInfo? = goal.itemType?.let { Categories.builtIn(it) }): Txt {
        val what: Any = when (goal.kind) {
            GoalKind.TIME -> StudyMath.formatDuration(goal.target * 60L, short = true)
            GoalKind.ITEMS -> category?.count(goal.target) ?: Txt.Plural(R.plurals.count_item, goal.target)
        }
        return when (goal.period) {
            GoalPeriod.DAILY -> Txt.Res(R.string.goal_desc_daily, listOf(what))
            GoalPeriod.WEEKLY -> Txt.Res(R.string.goal_desc_weekly, listOf(what))
            GoalPeriod.MONTHLY -> Txt.Res(R.string.goal_desc_monthly, listOf(what))
            GoalPeriod.BY_DATE -> Txt.Res(R.string.goal_desc_by_date, listOf(what, LocalDate.ofEpochDay(goal.deadlineEpochDay ?: 0)))
        }
    }
}
