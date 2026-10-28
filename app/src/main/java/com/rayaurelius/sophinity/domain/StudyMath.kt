package com.rayaurelius.sophinity.domain

import com.rayaurelius.sophinity.data.db.StudySession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** A half-open date range [start, endExclusive). */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && d.isBefore(endExclusive)
    val days: List<LocalDate>
        get() = generateSequence(start) { it.plusDays(1) }.takeWhile { it.isBefore(endExclusive) }.toList()
    fun startMillis(zone: ZoneId) = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMillis(zone: ZoneId) = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
}

data class StreakInfo(
    val current: Int,
    val best: Int,
    val restDaysUsedThisWeek: Int,
    val studiedToday: Boolean,
)

object StudyMath {

    fun dayOf(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun dayRange(d: LocalDate) = DateRange(d, d.plusDays(1))

    fun weekRange(d: LocalDate, weekStart: DayOfWeek): DateRange {
        val start = d.with(TemporalAdjusters.previousOrSame(weekStart))
        return DateRange(start, start.plusDays(7))
    }

    fun monthRange(d: LocalDate): DateRange {
        val start = d.withDayOfMonth(1)
        return DateRange(start, start.plusMonths(1))
    }

    fun yearRange(d: LocalDate): DateRange {
        val start = d.withDayOfYear(1)
        return DateRange(start, start.plusYears(1))
    }

    /** Seconds studied per calendar day. A session counts toward the day it started. */
    fun dailySeconds(sessions: List<StudySession>, zone: ZoneId): Map<LocalDate, Long> =
        sessions.groupBy { dayOf(it.startedAt, zone) }.mapValues { (_, s) -> s.sumOf { it.durationSec } }

    fun secondsIn(range: DateRange, daily: Map<LocalDate, Long>): Long =
        daily.entries.sumOf { (d, s) -> if (d in range) s else 0L }

    /**
     * Streak with built-in rest days. Research on habit formation (Lally et al., 2010) found that
     * missing a single day does not materially derail habit automaticity, while "all-or-nothing"
     * streaks invite the what-the-hell effect after one slip. So each week allows [restPerWeek]
     * missed days that pause — rather than break — the streak. Streak length counts studied days.
     */
    fun streak(
        daily: Map<LocalDate, Long>,
        today: LocalDate,
        minSecondsPerDay: Long,
        restPerWeek: Int,
        weekStart: DayOfWeek,
    ): StreakInfo {
        val active = daily.filterValues { it >= minSecondsPerDay }.keys
        if (active.isEmpty()) return StreakInfo(0, 0, 0, false)
        val first = active.min()
        fun weekKey(d: LocalDate) = d.with(TemporalAdjusters.previousOrSame(weekStart))

        // Current streak: walk back from today. Today doesn't count as a miss until it's over.
        val studiedToday = today in active
        var current = if (studiedToday) 1 else 0
        val restUsed = mutableMapOf<LocalDate, Int>()
        var d = today.minusDays(1)
        while (!d.isBefore(first)) {
            if (d in active) {
                current++
            } else {
                val wk = weekKey(d)
                val used = restUsed.getOrDefault(wk, 0)
                if (used < restPerWeek) restUsed[wk] = used + 1 else break
            }
            d = d.minusDays(1)
        }

        // Best streak: same rules, walking forward through all history.
        var best = 0
        var run = 0
        val fwdRest = mutableMapOf<LocalDate, Int>()
        d = first
        while (!d.isAfter(today)) {
            if (d in active) {
                run++
                best = maxOf(best, run)
            } else if (d != today) {
                val wk = weekKey(d)
                val used = fwdRest.getOrDefault(wk, 0)
                if (used < restPerWeek) fwdRest[wk] = used + 1 else {
                    run = 0
                    fwdRest.clear()
                }
            }
            d = d.plusDays(1)
        }
        val thisWeek = weekKey(today)
        return StreakInfo(
            current = current,
            best = maxOf(best, current),
            restDaysUsedThisWeek = if (current > 0) restUsed.getOrDefault(thisWeek, 0) else 0,
            studiedToday = studiedToday,
        )
    }

    /** Set by the app to a localized formatter; the built-in English one is used in plain JVM tests. */
    @Volatile var durationFormatter: ((Long, Boolean) -> String)? = null

    fun formatDuration(seconds: Long, short: Boolean = false): String {
        durationFormatter?.let { return it(seconds, short) }
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 && short -> if (m > 0) "${h}h ${m}m" else "${h}h"
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m"
            seconds > 0 -> "<1m"
            else -> "0m"
        }
    }

    fun formatClock(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
