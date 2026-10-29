package com.rayaurelius.sophinity.ui.stats

import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.ui.components.PageHeader
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.domain.DateRange
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.ui.components.Bar
import com.rayaurelius.sophinity.ui.components.EmptyState
import com.rayaurelius.sophinity.ui.components.LinearMeter
import com.rayaurelius.sophinity.ui.components.SectionCard
import com.rayaurelius.sophinity.ui.components.StatTile
import com.rayaurelius.sophinity.ui.components.StudyBarChart
import com.rayaurelius.sophinity.ui.components.StudyHeatmap
import com.rayaurelius.sophinity.ui.components.SubjectDot
import com.rayaurelius.sophinity.ui.components.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

class StatsViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun delete(s: StudySession) = viewModelScope.launch { c.repo.deleteSession(s) }
}

enum class Period(@androidx.annotation.StringRes val label: Int) { DAY(R.string.period_day), WEEK(R.string.period_week), MONTH(R.string.period_month), YEAR(R.string.period_year) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(contentPadding: PaddingValues, onOpenSessions: () -> Unit) {
    val vm = appViewModel { StatsViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var offset by rememberSaveable(period) { mutableIntStateOf(0) }

    val anchor = when (period) {
        Period.DAY -> snap.today.plusDays(offset.toLong())
        Period.WEEK -> snap.today.plusWeeks(offset.toLong())
        Period.MONTH -> snap.today.plusMonths(offset.toLong())
        Period.YEAR -> snap.today.plusYears(offset.toLong())
    }
    val range = when (period) {
        Period.DAY -> StudyMath.dayRange(anchor)
        Period.WEEK -> StudyMath.weekRange(anchor, snap.settings.weekStart)
        Period.MONTH -> StudyMath.monthRange(anchor)
        Period.YEAR -> StudyMath.yearRange(anchor)
    }
    val prevRange = when (period) {
        Period.DAY -> StudyMath.dayRange(anchor.minusDays(1))
        Period.WEEK -> StudyMath.weekRange(anchor.minusWeeks(1), snap.settings.weekStart)
        Period.MONTH -> StudyMath.monthRange(anchor.minusMonths(1))
        Period.YEAR -> StudyMath.yearRange(anchor.minusYears(1))
    }
    val sessions = snap.sessions.filter { StudyMath.dayOf(it.startedAt, snap.zone) in range }
    val total = sessions.sumOf { it.durationSec }
    // Compare like with like: for the current period, only count the same number of elapsed days
    // from the previous one. Comparing 2 days against a full week would always look like a drop.
    val elapsed = range.days.count { !it.isAfter(snap.today) }
    val prevTotal = StudyMath.secondsIn(
        if (offset == 0) DateRange(prevRange.start, minOf(prevRange.endExclusive, prevRange.start.plusDays(elapsed.toLong()))) else prevRange,
        snap.daily,
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { PageHeader(stringResource(R.string.stats_title)) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(period == p, { period = p }, SegmentedButtonDefaults.itemShape(i, Period.entries.size)) { Text(stringResource(p.label)) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.stats_prev)) }
                Text(rangeLabel(period, range, offset), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.stats_next)) }
            }
        }
        item { Summary(snap, period, range, total, prevTotal, sessions.size) }
        if (period != Period.DAY) item { ChartCard(snap, period, range) }
        item { SubjectBreakdown(snap, sessions, total) }
        item {
            SectionCard(title = stringResource(R.string.stats_consistency)) {
                StudyHeatmap(snap.daily, snap.today, snap.settings.weekStart)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("flame", "${snap.streak.current}", stringResource(R.string.stats_current_streak), Modifier.weight(1f))
                    StatTile("torch", "${snap.streak.best}", stringResource(R.string.stats_best_streak), Modifier.weight(1f))
                    StatTile("sundial", StudyMath.formatDuration(snap.totalSeconds, true), stringResource(R.string.stats_all_time), Modifier.weight(1f))
                }
            }
        }
        if (sessions.isEmpty()) {
            item { EmptyState("sprout", stringResource(R.string.stats_empty_title), stringResource(R.string.stats_empty_body)) }
        }
        item {
            OutlinedButton(onClick = onOpenSessions, modifier = Modifier.fillMaxWidth()) {
                Glyph("hourglass", size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.stats_all_sessions))
            }
        }
    }
}
@Composable
private fun rangeLabel(p: Period, r: DateRange, offset: Int): String = when (p) {
    Period.DAY -> if (offset == 0) stringResource(R.string.stats_today) else if (offset == -1) stringResource(R.string.stats_yesterday) else r.start.format(rememberDateFormat("EEEMMMd"))
    Period.WEEK -> if (offset == 0) stringResource(R.string.stats_this_week) else if (offset == -1) stringResource(R.string.stats_last_week) else
        stringResource(R.string.stats_range, r.start.format(rememberDateFormat("MMMd")), r.endExclusive.minusDays(1).format(rememberDateFormat("MMMd")))
    Period.MONTH -> r.start.format(rememberDateFormat("MMMMyyyy"))
    Period.YEAR -> r.start.year.toString()
}

@Composable
private fun Summary(snap: Snapshot, period: Period, range: DateRange, total: Long, prevTotal: Long, count: Int) {
    val elapsedDays = range.days.count { !it.isAfter(snap.today) }.coerceAtLeast(1)
    val studiedDays = range.days.count { (snap.daily[it] ?: 0) > 0 }
    val delta = if (prevTotal > 0) ((total - prevTotal) * 100 / prevTotal).toInt() else null
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("hourglass", StudyMath.formatDuration(total, true), stringResource(R.string.stats_total), Modifier.weight(1f))
        if (period == Period.DAY) {
            StatTile("tablet", "$count", pluralStringResource(R.plurals.stats_sessions_count, count), Modifier.weight(1f))
            val focusAvg = snap.sessions.filter { StudyMath.dayOf(it.startedAt, snap.zone) in range }.mapNotNull { it.focus }.average()
            StatTile("eye", if (focusAvg.isNaN()) "–" else "%.1f/5".format(focusAvg), stringResource(R.string.stats_avg_focus), Modifier.weight(1f))
        } else {
            StatTile("columns", StudyMath.formatDuration(total / elapsedDays, true), stringResource(R.string.stats_daily_avg), Modifier.weight(1f))
            StatTile(
                if (delta == null) "sun" else "steps",
                when { delta == null -> stringResource(R.string.stats_days_short, studiedDays); delta >= 0 -> "+$delta%"; else -> "$delta%" },
                stringResource(when { delta == null -> R.string.stats_days_learning; period == Period.YEAR -> R.string.stats_vs_last_year; period == Period.MONTH -> R.string.stats_vs_same_month; period == Period.DAY -> R.string.stats_vs_yesterday; else -> R.string.stats_vs_same_week }),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChartCard(snap: Snapshot, period: Period, range: DateRange) {
    val dayFmt = rememberDateFormat("EEEMMMd")
    val monthFmt = rememberDateFormat("MMMM")
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val bars: List<Bar>
    var goalLine: Long? = null
    when (period) {
        Period.WEEK, Period.MONTH -> {
            bars = range.days.map { d ->
                Bar(
                    label = if (period == Period.WEEK) d.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) else d.dayOfMonth.toString(),
                    seconds = snap.daily[d] ?: 0,
                    detail = d.format(dayFmt),
                    highlight = d == snap.today,
                )
            }
            snap.goals.firstOrNull { it.kind == GoalKind.TIME && it.period == GoalPeriod.DAILY && it.subjectId == null }
                ?.let { goalLine = it.target * 60L }
        }
        else -> {
            bars = (0 until 12).map { m ->
                val start = range.start.plusMonths(m.toLong())
                val r = StudyMath.monthRange(start)
                Bar(
                    label = start.month.getDisplayName(TextStyle.NARROW, locale),
                    seconds = StudyMath.secondsIn(r, snap.daily),
                    detail = start.format(monthFmt),
                    highlight = snap.today in r,
                )
            }
        }
    }
    SectionCard(title = stringResource(if (period == Period.YEAR) R.string.stats_by_month else R.string.stats_by_day)) {
        StudyBarChart(bars, goalLineSeconds = goalLine, labelEvery = if (period == Period.MONTH) 5 else 1)
        if (goalLine != null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.stats_goal_line), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SubjectBreakdown(snap: Snapshot, sessions: List<StudySession>, total: Long) {
    if (total <= 0) return
    val bySubject = sessions.groupBy { it.subjectId }.mapValues { (_, v) -> v.sumOf { it.durationSec } }
        .entries.sortedByDescending { it.value }
    SectionCard(title = stringResource(R.string.stats_by_subject)) {
        bySubject.forEachIndexed { i, (sid, sec) ->
            val subject = sid?.let { snap.subjectsById[it] }
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glyph(subject?.icon ?: "meander", size = 20.dp, tint = subject?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Text(subject?.name ?: stringResource(R.string.stats_general), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.stats_share, StudyMath.formatDuration(sec, true), (sec * 100 / total).toInt()), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            LinearMeter(sec.toFloat() / total, Modifier.fillMaxWidth(), color = subject?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline)
        }
    }
}


