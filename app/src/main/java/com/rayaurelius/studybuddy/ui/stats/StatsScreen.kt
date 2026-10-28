package com.rayaurelius.studybuddy.ui.stats

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
import com.rayaurelius.studybuddy.AppContainer
import com.rayaurelius.studybuddy.data.Snapshot
import com.rayaurelius.studybuddy.data.db.GoalKind
import com.rayaurelius.studybuddy.data.db.GoalPeriod
import com.rayaurelius.studybuddy.data.db.StudySession
import com.rayaurelius.studybuddy.domain.DateRange
import com.rayaurelius.studybuddy.domain.StudyMath
import com.rayaurelius.studybuddy.ui.components.Bar
import com.rayaurelius.studybuddy.ui.components.EmptyState
import com.rayaurelius.studybuddy.ui.components.LinearMeter
import com.rayaurelius.studybuddy.ui.components.SectionCard
import com.rayaurelius.studybuddy.ui.components.StatTile
import com.rayaurelius.studybuddy.ui.components.StudyBarChart
import com.rayaurelius.studybuddy.ui.components.StudyHeatmap
import com.rayaurelius.studybuddy.ui.components.SubjectDot
import com.rayaurelius.studybuddy.ui.components.appViewModel
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

enum class Period(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { StatsViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var offset by rememberSaveable(period) { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf<StudySession?>(null) }

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
        item { Text("Your progress", style = MaterialTheme.typography.headlineMedium) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(period == p, { period = p }, SegmentedButtonDefaults.itemShape(i, Period.entries.size)) { Text(p.label) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
                Text(rangeLabel(period, range, offset), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
            }
        }
        item { Summary(snap, period, range, total, prevTotal, sessions.size) }
        if (period != Period.DAY) item { ChartCard(snap, period, range) }
        item { SubjectBreakdown(snap, sessions, total) }
        item {
            SectionCard(title = "Consistency") {
                StudyHeatmap(snap.daily, snap.today, snap.settings.weekStart)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("🔥", "${snap.streak.current}", "current streak", Modifier.weight(1f))
                    StatTile("🏆", "${snap.streak.best}", "best streak", Modifier.weight(1f))
                    StatTile("📚", StudyMath.formatDuration(snap.totalSeconds, true), "all time", Modifier.weight(1f))
                }
            }
        }
        item { Text("Sessions", style = MaterialTheme.typography.titleMedium) }
        if (sessions.isEmpty()) {
            item { EmptyState("🌱", "No sessions here yet", "Start the timer on Today, or log a session you forgot to time.") }
        } else {
            items(sessions.take(100), key = { it.id }) { s -> SessionRow(snap, s, onDelete = { confirmDelete = s }) }
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this session?") },
            text = { Text("${StudyMath.formatDuration(s.durationSec)} will be removed from your stats.") },
            confirmButton = { TextButton(onClick = { vm.delete(s); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun rangeLabel(p: Period, r: DateRange, offset: Int): String = when (p) {
    Period.DAY -> if (offset == 0) "Today" else if (offset == -1) "Yesterday" else r.start.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    Period.WEEK -> if (offset == 0) "This week" else if (offset == -1) "Last week" else
        "${r.start.format(DateTimeFormatter.ofPattern("MMM d"))} – ${r.endExclusive.minusDays(1).format(DateTimeFormatter.ofPattern("MMM d"))}"
    Period.MONTH -> r.start.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    Period.YEAR -> r.start.year.toString()
}

@Composable
private fun Summary(snap: Snapshot, period: Period, range: DateRange, total: Long, prevTotal: Long, count: Int) {
    val elapsedDays = range.days.count { !it.isAfter(snap.today) }.coerceAtLeast(1)
    val studiedDays = range.days.count { (snap.daily[it] ?: 0) > 0 }
    val delta = if (prevTotal > 0) ((total - prevTotal) * 100 / prevTotal).toInt() else null
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("⏱️", StudyMath.formatDuration(total, true), "total", Modifier.weight(1f))
        if (period == Period.DAY) {
            StatTile("🧩", "$count", if (count == 1) "session" else "sessions", Modifier.weight(1f))
            val focusAvg = snap.sessions.filter { StudyMath.dayOf(it.startedAt, snap.zone) in range }.mapNotNull { it.focus }.average()
            StatTile("🎯", if (focusAvg.isNaN()) "–" else "%.1f/5".format(focusAvg), "avg focus", Modifier.weight(1f))
        } else {
            StatTile("📊", StudyMath.formatDuration(total / elapsedDays, true), "daily avg", Modifier.weight(1f))
            StatTile(
                if (delta == null) "✨" else if (delta >= 0) "📈" else "📉",
                when { delta == null -> "$studiedDays d"; delta >= 0 -> "+$delta%"; else -> "$delta%" },
                if (delta == null) "days studied" else if (period == Period.YEAR) "vs last year" else "vs same point last ${period.label.lowercase()}",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChartCard(snap: Snapshot, period: Period, range: DateRange) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val bars: List<Bar>
    var goalLine: Long? = null
    when (period) {
        Period.WEEK, Period.MONTH -> {
            bars = range.days.map { d ->
                Bar(
                    label = if (period == Period.WEEK) d.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) else d.dayOfMonth.toString(),
                    seconds = snap.daily[d] ?: 0,
                    detail = d.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
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
                    detail = start.format(DateTimeFormatter.ofPattern("MMMM")),
                    highlight = snap.today in r,
                )
            }
        }
    }
    SectionCard(title = if (period == Period.YEAR) "By month" else "By day") {
        StudyBarChart(bars, goalLineSeconds = goalLine, labelEvery = if (period == Period.MONTH) 5 else 1)
        if (goalLine != null) {
            Spacer(Modifier.height(8.dp))
            Text("Dashed line: your daily goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SubjectBreakdown(snap: Snapshot, sessions: List<StudySession>, total: Long) {
    if (total <= 0) return
    val bySubject = sessions.groupBy { it.subjectId }.mapValues { (_, v) -> v.sumOf { it.durationSec } }
        .entries.sortedByDescending { it.value }
    SectionCard(title = "By subject") {
        bySubject.forEachIndexed { i, (sid, sec) ->
            val subject = sid?.let { snap.subjectsById[it] }
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectDot(subject?.color ?: 0xFF9AA0A6.toInt())
                Spacer(Modifier.width(8.dp))
                Text(subject?.let { "${it.emoji} ${it.name}" } ?: "General", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${StudyMath.formatDuration(sec, true)} · ${sec * 100 / total}%", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            LinearMeter(sec.toFloat() / total, Modifier.fillMaxWidth(), color = subject?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SessionRow(snap: Snapshot, s: StudySession, onDelete: () -> Unit) {
    val subject = s.subjectId?.let { snap.subjectsById[it] }
    val item = s.itemId?.let { snap.itemsById[it] }
    val time = Instant.ofEpochMilli(s.startedAt).atZone(snap.zone)
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text(subject?.emoji ?: "📘", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(subject?.name, item?.title).joinToString(" · ").ifEmpty { "Study session" },
                style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${time.format(DateTimeFormatter.ofPattern("EEE MMM d · h:mm a"))} · ${StudyMath.formatDuration(s.durationSec)}" +
                    (s.focus?.let { " · focus $it/5" } ?: ""),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (s.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("“${s.note}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete session", tint = MaterialTheme.colorScheme.outline) }
    }
}

@Suppress("unused")
private fun LocalDate.short() = format(DateTimeFormatter.ofPattern("MMM d"))
