@file:OptIn(ExperimentalMaterial3Api::class)

package com.rayaurelius.sophinity.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.domain.GoalMath
import com.rayaurelius.sophinity.domain.GoalProgress
import com.rayaurelius.sophinity.domain.MediaTime
import com.rayaurelius.sophinity.domain.Milestones
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.timer.TimerMode
import com.rayaurelius.sophinity.timer.TimerState
import com.rayaurelius.sophinity.timer.TimerStatus
import com.rayaurelius.sophinity.ui.components.Confetti
import com.rayaurelius.sophinity.ui.components.FocusRating
import com.rayaurelius.sophinity.ui.components.LinearMeter
import com.rayaurelius.sophinity.ui.components.NumberField
import com.rayaurelius.sophinity.ui.components.ProgressRing
import com.rayaurelius.sophinity.ui.components.SectionCard
import com.rayaurelius.sophinity.ui.components.StatTile
import com.rayaurelius.sophinity.ui.components.SubjectPicker
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.theme.Brand
import com.rayaurelius.sophinity.ui.theme.TimerDigits
import com.rayaurelius.sophinity.ui.theme.LocalHeadingFont
import com.rayaurelius.sophinity.ui.components.GreekKeyBand
import androidx.compose.ui.text.font.FontWeight
import com.rayaurelius.sophinity.ui.theme.MediaDigits
import com.rayaurelius.sophinity.ui.components.MediaMeter
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBadges: () -> Unit,
) {
    val vm = appViewModel { TodayViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value
    val timer = vm.timer.collectAsStateWithLifecycle().value
    val reflect by vm.reflect.collectAsStateWithLifecycle()
    var showManual by rememberSaveable { mutableStateOf(false) }

    if (snap == null || timer == null) return

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Header(snap, onOpenSettings, onOpenBadges) }
        item { TimerCard(snap, timer, vm) }
        item { QuickStats(snap) }
        item { GoalsGlance(snap.goalProgress, onOpenGoals) }
        if (snap.activeItems.isNotEmpty()) item { ContinueLearning(snap, timer, vm) }
        item { TipCard(snap) }
        item {
            TextButton(onClick = { showManual = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.EditCalendar, null)
                Spacer(Modifier.width(8.dp))
                Text("Log a session I forgot to time")
            }
        }
    }

    reflect?.let { session -> ReflectionSheet(session, snap, timer, vm) }
    if (showManual) ManualSessionDialog(snap, onDismiss = { showManual = false }) { subj, item, start, mins, note ->
        vm.addManual(subj, item, start, mins, note); showManual = false
    }
    MilestoneCelebration(snap, onSeen = vm::markSeen)
    // A new title is celebrated after any laurel dialogs are dismissed.
    if (snap.unlocked.none { !it.seen } && snap.title.current.rank > snap.settings.seenTitleRank) {
        TitleCelebration(snap.title.current, onSeen = { vm.markTitleSeen(snap.title.current.rank) })
    }
}

@Composable
private fun Header(snap: Snapshot, onOpenSettings: () -> Unit, onOpenBadges: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) { in 4..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    // Stacked so it never truncates: narrow screens, side toolbar, large text, long translations.
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(snap.today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "Settings") }
        }
        Text(greeting, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Surface(onClick = onOpenBadges, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                "🏛️ ${snap.title.current.name} · Lv ${snap.level.level}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun TimerCard(snap: Snapshot, timer: TimerState, vm: TodayViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer.status) {
        while (timer.status == TimerStatus.RUNNING || timer.status == TimerStatus.BREAK) {
            now = System.currentTimeMillis()
            if (timer.focusDue(now)) vm.completeIfDue()
            if (timer.status == TimerStatus.BREAK && now >= timer.breakEndsAt) vm.skipBreak()
            delay(250)
        }
        now = System.currentTimeMillis()
    }

    var mode by rememberSaveable { mutableStateOf(TimerMode.STOPWATCH) }
    var subjectId by rememberSaveable { mutableStateOf(snap.settings.lastSubjectId?.takeIf { it in snap.subjectsById }) }
    var itemId by rememberSaveable { mutableStateOf<Long?>(null) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (timer.status) {
                TimerStatus.IDLE -> {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(mode == TimerMode.STOPWATCH, { mode = TimerMode.STOPWATCH }, SegmentedButtonDefaults.itemShape(0, 2)) {
                            Text("Open session")
                        }
                        SegmentedButton(mode == TimerMode.FOCUS, { mode = TimerMode.FOCUS }, SegmentedButtonDefaults.itemShape(1, 2)) {
                            Text("Focus ${snap.settings.focusMinutes}m")
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    ProgressRing(0f, Modifier.size(200.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (mode == TimerMode.FOCUS) "%02d:00".format(snap.settings.focusMinutes) else "00:00",
                                style = TimerDigits,
                            )
                            Text(
                                if (mode == TimerMode.FOCUS) "then a ${snap.settings.breakMinutes}-min break" else "count up, stop anytime",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SubjectPicker(snap.activeSubjects, subjectId, { subjectId = it; itemId = null })
                        ItemPicker(snap.activeItems, itemId, { snap.categoryOf(it).emoji }) { id ->
                            itemId = id
                            snap.itemsById[id]?.subjectId?.let { subjectId = it }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.start(mode, subjectId, itemId) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start learning", style = MaterialTheme.typography.titleMedium)
                    }
                }

                TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                    val focus = timer.mode == TimerMode.FOCUS
                    val elapsed = timer.elapsedMs(now)
                    val subject = timer.subjectId?.let { snap.subjectsById[it] }
                    // Media being studied, with this session's time added live (× playback speed).
                    val media = timer.itemId?.let { snap.itemsById[it] }?.let { snap.mediaProgress(it, liveSec = elapsed / 1000) }
                    // Open sessions on timed media show your place in the media; focus blocks keep the countdown.
                    val showMediaInRing = media != null && !focus
                    val progress = when {
                        focus -> elapsed.toFloat() / timer.targetMs
                        showMediaInRing -> media!!.fraction
                        else -> (elapsed % 3_600_000L) / 3_600_000f
                    }
                    Text(
                        listOfNotNull(subject?.let { "${it.emoji} ${it.name}" }, timer.itemId?.let { snap.itemsById[it]?.title })
                            .joinToString(" · ").ifEmpty { "Learning" },
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(16.dp))
                    ProgressRing(progress, Modifier.size(220.dp), color = subject?.let { androidx.compose.ui.graphics.Color(it.color) } ?: MaterialTheme.colorScheme.primary) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showMediaInRing) {
                                Text(StudyMath.formatClock(media!!.completedSec * 1000), style = MediaDigits)
                                Text(
                                    "of ${StudyMath.formatClock(media.totalSec * 1000)} · ${media.percent}%",
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (timer.status == TimerStatus.PAUSED) "Paused" else "completed",
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(StudyMath.formatClock(if (focus) timer.remainingMs(now) else elapsed), style = TimerDigits)
                                Text(
                                    when {
                                        timer.status == TimerStatus.PAUSED -> "Paused"
                                        focus -> "focus · ${StudyMath.formatDuration(elapsed / 1000)} done"
                                        else -> "in the zone"
                                    },
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (media != null) {
                        Spacer(Modifier.height(12.dp))
                        if (showMediaInRing) {
                            Text(
                                "This session ${StudyMath.formatClock(elapsed)}" +
                                    (if (media.speed != 1f) " at ${MediaTime.formatSpeed(media.speed)}" else "") +
                                    if (!media.isComplete) " · ${StudyMath.formatDuration(media.remainingAtSpeedSec, true)} to go" else " · finished! 🎉",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                LinearMeter(media.fraction, Modifier.fillMaxWidth(), color = if (media.isComplete) Brand.Laurel else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${StudyMath.formatClock(media.completedSec * 1000)} of ${StudyMath.formatClock(media.totalSec * 1000)} · ${media.percent}%" +
                                        if (media.speed != 1f) " · ${MediaTime.formatSpeed(media.speed)}" else "",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { vm.discard() }, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.Close, "Discard session") }
                        if (timer.status == TimerStatus.RUNNING) {
                            FilledTonalButton(onClick = { vm.pause() }, modifier = Modifier.height(52.dp)) {
                                Icon(Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause")
                            }
                        } else {
                            FilledTonalButton(onClick = { vm.resume() }, modifier = Modifier.height(52.dp)) {
                                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume")
                            }
                        }
                        Button(onClick = { vm.finish() }, modifier = Modifier.height(52.dp)) {
                            Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp)); Text("Finish")
                        }
                    }
                }

                TimerStatus.BREAK -> {
                    Text("Break time ☕", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    val total = snap.settings.breakMinutes * 60_000f
                    val left = (timer.breakEndsAt - now).coerceAtLeast(0)
                    ProgressRing(1f - left / total, Modifier.size(200.dp), color = Brand.Laurel) {
                        Text(StudyMath.formatClock(left), style = TimerDigits)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Stand up, stretch, look at something far away.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { vm.skipBreak() }) { Text("Skip break") }
                }
            }
        }
    }
}

@Composable
private fun ItemPicker(items: List<LearningItem>, selectedId: Long?, emojiOf: (LearningItem) -> String, onSelect: (Long?) -> Unit) {
    if (items.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val sel = items.firstOrNull { it.id == selectedId }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(sel?.let { "${emojiOf(it)} ${it.title}" } ?: "＋ What are you learning?", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            modifier = Modifier.width(170.dp),
        )
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("Nothing specific") }, onClick = { onSelect(null); open = false })
            items.forEach { item ->
                DropdownMenuItem(text = { Text("${emojiOf(item)}  ${item.title}", maxLines = 1) }, onClick = { onSelect(item.id); open = false })
            }
        }
    }
}

@Composable
private fun QuickStats(snap: Snapshot) {
    val streak = snap.streak
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("⏱️", StudyMath.formatDuration(snap.todaySeconds, true), "today", Modifier.weight(1f))
        StatTile(
            "🔥", "${streak.current} day${if (streak.current == 1) "" else "s"}",
            if (!streak.studiedToday && streak.current > 0) "learn to extend" else "streak",
            Modifier.weight(1f),
            container = com.rayaurelius.sophinity.ui.theme.LocalGold.current.container,
        )
        StatTile("📅", StudyMath.formatDuration(snap.weekSeconds, true), "this week", Modifier.weight(1f))
    }
}

@Composable
private fun GoalsGlance(progress: List<GoalProgress>, onOpenGoals: () -> Unit) {
    SectionCard(title = "Goals", action = { TextButton(onClick = onOpenGoals) { Text(if (progress.isEmpty()) "Set one" else "All goals") } }) {
        if (progress.isEmpty()) {
            Text(
                "People who set a specific, challenging goal reliably outperform those told to \"do their best.\" Try 3 hours a week to start.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            progress.take(3).forEachIndexed { i, p ->
                if (i > 0) Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.goal.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (p.met) "Done ✓" else goalAmount(p),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (p.met) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearMeter(p.fraction, Modifier.fillMaxWidth(), color = if (p.met) Brand.Laurel else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun goalAmount(p: GoalProgress): String = when (p.goal.kind) {
    com.rayaurelius.sophinity.data.db.GoalKind.TIME ->
        "${StudyMath.formatDuration(p.current * 60L, true)} / ${StudyMath.formatDuration(p.target * 60L, true)}"
    com.rayaurelius.sophinity.data.db.GoalKind.ITEMS -> "${p.current} / ${p.target}"
}

@Composable
private fun ContinueLearning(snap: Snapshot, timer: TimerState, vm: TodayViewModel) {
    Column {
        Text("Continue learning", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(snap.activeItems, key = { it.id }) { item ->
                OutlinedCard(Modifier.width(200.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(snap.categoryOf(item).emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(40.dp))
                        val total = item.totalUnits
                        val media = snap.mediaProgress(item)
                        if (media != null) {
                            Spacer(Modifier.height(6.dp))
                            MediaMeter(media, compact = true)
                        } else if (total != null && total > 0) {
                            Spacer(Modifier.height(6.dp))
                            LinearMeter(item.progressUnits.toFloat() / total, Modifier.fillMaxWidth())
                            Text("${item.progressUnits}/$total ${snap.categoryOf(item).unit}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(StudyMath.formatDuration(snap.secondsForItem(item.id)) + " logged", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { if (timer.isActive) vm.setSubject(item.subjectId, item.id) else vm.start(TimerMode.STOPWATCH, item.subjectId, item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (timer.isActive) "Switch to this" else "Learn this") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipCard(snap: Snapshot) {
    val wisdom = Wisdoms.forDay(snap.today)
    val tip = LearningTips.forDay(snap.today)
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            GreekKeyBand(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text("Today's wisdom", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "“${wisdom.text}”",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = LocalHeadingFont.current, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "— ${wisdom.who}, ${wisdom.source}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(14.dp))
                Row {
                    Text(tip.emoji, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Try this · ${tip.title}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(tip.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionSheet(session: StudySession, snap: Snapshot, timer: TimerState, vm: TodayViewModel) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by rememberSaveable(session.id) { mutableStateOf(session.note) }
    var focus by rememberSaveable(session.id) { mutableStateOf(session.focus) }
    val subject = session.subjectId?.let { snap.subjectsById[it] }
    ModalBottomSheet(onDismissRequest = { vm.saveReflection(session, note, focus) }, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Nice work! 🎉", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${StudyMath.formatDuration(session.durationSec)}${subject?.let { " of ${it.name}" } ?: ""} saved.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text("How focused were you?", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FocusRating(focus) { focus = it }
            Spacer(Modifier.height(20.dp))
            Text("One thing you learned", style = MaterialTheme.typography.titleSmall)
            Text("Recalling it now, without looking, helps it stick.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text("e.g. Spaced repetition beats cramming") }, minLines = 2)
            val item = session.itemId?.let { snap.itemsById[it] }
            val media = item?.let { snap.mediaProgress(it) }
            if (item != null && media != null && item.status != ItemStatus.DONE) {
                Spacer(Modifier.height(20.dp))
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${snap.categoryOf(item).emoji} ${item.title}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        MediaMeter(media)
                        if (media.isComplete) {
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = { vm.markFinished(item) }, modifier = Modifier.fillMaxWidth()) {
                                Text("You've reached the end — mark as finished 🎉")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (timer.status == TimerStatus.IDLE && session.durationSec >= (snap.settings.focusMinutes * 60L) - 5) {
                    FilledTonalButton(onClick = { vm.saveReflection(session, note, focus); vm.startBreak() }, Modifier.weight(1f)) {
                        Text("Save + break")
                    }
                }
                Button(onClick = { vm.saveReflection(session, note, focus) }, Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}

@Composable
fun ManualSessionDialog(
    snap: Snapshot,
    onDismiss: () -> Unit,
    onSave: (subjectId: Long?, itemId: Long?, startedAt: Long, minutes: Int, note: String) -> Unit,
) {
    var subjectId by remember { mutableStateOf<Long?>(snap.settings.lastSubjectId) }
    var itemId by remember { mutableStateOf<Long?>(null) }
    var hours by remember { mutableStateOf<Int?>(0) }
    var minutes by remember { mutableStateOf<Int?>(30) }
    var note by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(snap.today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()) }
    var pickDate by remember { mutableStateOf(false) }
    val total = (hours ?: 0) * 60 + (minutes ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a past session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                OutlinedCard(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("📅  ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))}", Modifier.padding(14.dp))
                }
                SubjectPicker(snap.activeSubjects, subjectId, { subjectId = it })
                ItemPicker(snap.activeItems, itemId, { snap.categoryOf(it).emoji }) { id ->
                    itemId = id
                    snap.itemsById[id]?.subjectId?.let { subjectId = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Hours", hours, { hours = it?.coerceAtMost(16) }, Modifier.weight(1f))
                    NumberField("Minutes", minutes, { minutes = it?.coerceAtMost(59) }, Modifier.weight(1f))
                }
                OutlinedTextField(note, { note = it }, label = { Text("What did you learn? (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = total > 0, onClick = {
                val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val noon = date.atTime(12, 0).atZone(snap.zone).toInstant().toEpochMilli()
                onSave(subjectId, itemId, noon, total, note)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= snap.today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateMillis = it }; pickDate = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun MilestoneCelebration(snap: Snapshot, onSeen: (List<String>) -> Unit) {
    val fresh = snap.unlocked.filter { !it.seen }.mapNotNull { Milestones.byKey[it.key] }
    if (fresh.isEmpty()) return
    Box(Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = { onSeen(fresh.map { it.key }) },
            icon = { Text(fresh.first().emoji, style = MaterialTheme.typography.displayMedium) },
            title = { Text(if (fresh.size == 1) "Laurel earned!" else "${fresh.size} laurels earned!", textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    fresh.take(5).forEach { m ->
                        Text("${m.emoji}  ${m.title}", style = MaterialTheme.typography.titleMedium)
                        Text(m.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (fresh.size > 5) Text("…and ${fresh.size - 5} more on the Laurels tab")
                }
            },
            confirmButton = { Button(onClick = { onSeen(fresh.map { it.key }) }) { Text("Keep going") } },
        )
        Confetti()
    }
}

@Composable
private fun TitleCelebration(title: com.rayaurelius.sophinity.domain.LearnerTitle, onSeen: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = onSeen,
            icon = { Text("🏛️", style = MaterialTheme.typography.displayMedium) },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("A new title", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(title.name, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${title.greek} · “${title.meaning}”", style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalHeadingFont.current),
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (title.rank == com.rayaurelius.sophinity.domain.Titles.highest.rank)
                            "The highest title of all. A true lover of wisdom — and the journey still never ends."
                        else "Earned through your laurels. Keep going — Philosopher awaits.",
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = onSeen) { Text("Onward") } },
        )
        Confetti()
    }
}
