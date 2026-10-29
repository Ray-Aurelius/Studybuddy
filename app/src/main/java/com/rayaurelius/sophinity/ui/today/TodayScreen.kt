@file:OptIn(ExperimentalMaterial3Api::class)

package com.rayaurelius.sophinity.ui.today

import com.rayaurelius.sophinity.ui.i18n.goalTitle
import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import com.rayaurelius.sophinity.domain.str
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SuggestionChip
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableIntStateOf
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.ui.library.LibraryViewModel
import com.rayaurelius.sophinity.ui.library.ItemEditorSheet
import com.rayaurelius.sophinity.ui.components.LibraryItemPicker
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.GlyphTile
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import com.rayaurelius.sophinity.ui.glyphs.LaurelMedal
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
        item { GoalsGlance(snap, snap.goalProgress, onOpenGoals) }
        if (snap.activeItems.isNotEmpty()) item { ContinueLearning(snap, timer, vm) }
        item { TipCard(snap) }
        item {
            TextButton(onClick = { showManual = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.EditCalendar, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.today_log_forgotten))
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
    val greeting = stringResource(when (hour) { in 4..11 -> R.string.greet_morning; in 12..16 -> R.string.greet_afternoon; else -> R.string.greet_evening })
    // Stacked so it never truncates: narrow screens, side toolbar, large text, long translations.
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(snap.today.format(rememberDateFormat("EEEEMMMMd")), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.common_settings)) }
        }
        Text(greeting, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Surface(onClick = onOpenBadges, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(snap.title.current.glyph, size = 18.dp, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.today_title_chip, snap.title.current.name.str(), snap.level.level),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
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
    /** Minutes the open session will start at, for when you forgot to press Start. */
    var headStartMin by rememberSaveable { mutableIntStateOf(0) }
    var pickHeadStart by remember { mutableStateOf(false) }
    if (pickHeadStart) HeadStartSheet(headStartMin, onDismiss = { pickHeadStart = false }) { headStartMin = it; pickHeadStart = false }
    var subjectId by rememberSaveable { mutableStateOf(snap.settings.lastSubjectId?.takeIf { it in snap.subjectsById }) }
    var itemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addingItem by remember { mutableStateOf(false) }
    if (addingItem) {
        val library = appViewModel { LibraryViewModel(it) }
        ItemEditorSheet(
            snap, LearningItem(type = ItemType.BOOK, title = "", status = ItemStatus.ACTIVE, subjectId = subjectId),
            onDismiss = { addingItem = false },
            onSave = { item ->
                library.save(item) { id -> itemId = id; item.subjectId?.let { subjectId = it } }
                addingItem = false
            },
            onDelete = { addingItem = false },
            onCreateCategory = { cat, picked -> library.saveCategory(cat, picked) },
        )
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (timer.status) {
                TimerStatus.IDLE -> {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(mode == TimerMode.STOPWATCH, { mode = TimerMode.STOPWATCH }, SegmentedButtonDefaults.itemShape(0, 2)) {
                            Text(stringResource(R.string.timer_open))
                        }
                        SegmentedButton(mode == TimerMode.FOCUS, { mode = TimerMode.FOCUS }, SegmentedButtonDefaults.itemShape(1, 2)) {
                            Text(stringResource(R.string.timer_focus, StudyMath.formatDuration(snap.settings.focusMinutes * 60L, true)))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    val stopwatch = mode == TimerMode.STOPWATCH
                    val head = if (stopwatch) headStartMin else 0
                    ProgressRing(
                        0f,
                        Modifier.size(200.dp).clip(CircleShape)
                            .then(if (stopwatch) Modifier.clickable(onClickLabel = stringResource(R.string.timer_start_from)) { pickHeadStart = true } else Modifier),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when {
                                    !stopwatch -> "%02d:00".format(snap.settings.focusMinutes)
                                    else -> StudyMath.formatClock(head * 60_000L)
                                },
                                style = if (head >= 60) MediaDigits else TimerDigits,
                            )
                            Text(
                                when {
                                    !stopwatch -> stringResource(R.string.timer_then_break, StudyMath.formatDuration(snap.settings.breakMinutes * 60L, true))
                                    head > 0 -> stringResource(R.string.timer_counts_from, StudyMath.formatDuration(head * 60L))
                                    else -> stringResource(R.string.timer_count_up)
                                },
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (stopwatch) {
                        TextButton(onClick = { pickHeadStart = true }) {
                            Glyph("hourglass", size = 18.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (head > 0) R.string.timer_change_start else R.string.timer_start_from))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SubjectPicker(snap.activeSubjects, subjectId, { subjectId = it; itemId = null })
                        ItemPicker(snap.items.filter { it.status != ItemStatus.DONE }.sortedBy { it.status != ItemStatus.ACTIVE }, itemId, { snap.categoryOf(it).glyph }) { id ->
                            itemId = id
                            snap.itemsById[id]?.subjectId?.let { subjectId = it }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Add a book, podcast or course right here, without leaving the timer; it's selected once saved.
                    FilledTonalIconButton(onClick = { addingItem = true }, modifier = Modifier.size(40.dp)) {
                        Glyph("scroll", size = 22.dp, contentDescription = stringResource(R.string.timer_add_media))
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { vm.start(mode, subjectId, itemId, if (mode == TimerMode.STOPWATCH) headStartMin * 60_000L else 0); headStartMin = 0 }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.timer_start), style = MaterialTheme.typography.titleMedium)
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
                        listOfNotNull(subject?.name, timer.itemId?.let { snap.itemsById[it]?.title })
                            .joinToString(" · ").ifEmpty { stringResource(R.string.common_learning) },
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(16.dp))
                    ProgressRing(progress, Modifier.size(220.dp), color = subject?.let { androidx.compose.ui.graphics.Color(it.color) } ?: MaterialTheme.colorScheme.primary) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showMediaInRing) {
                                Text(StudyMath.formatClock(media!!.completedSec * 1000), style = MediaDigits)
                                Text(
                                    stringResource(R.string.timer_media_of, StudyMath.formatClock(media.totalSec * 1000), media.percent),
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (timer.status == TimerStatus.PAUSED) stringResource(R.string.common_paused) else stringResource(R.string.timer_completed),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(StudyMath.formatClock(if (focus) timer.remainingMs(now) else elapsed), style = TimerDigits)
                                Text(
                                    when {
                                        timer.status == TimerStatus.PAUSED -> stringResource(R.string.common_paused)
                                        focus -> stringResource(R.string.timer_focus_done, StudyMath.formatDuration(elapsed / 1000))
                                        else -> stringResource(R.string.timer_in_zone)
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
                                (if (media.speed != 1f) stringResource(R.string.timer_session_speed, StudyMath.formatClock(elapsed), MediaTime.formatSpeed(media.speed))
                                else stringResource(R.string.timer_session, StudyMath.formatClock(elapsed))).let { session ->
                                    if (!media.isComplete) stringResource(R.string.timer_to_go, session, StudyMath.formatDuration(media.remainingAtSpeedSec, true))
                                    else stringResource(R.string.timer_finished, session)
                                },
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                LinearMeter(media.fraction, Modifier.fillMaxWidth(), color = if (media.isComplete) Brand.Laurel else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (media.speed != 1f) stringResource(R.string.timer_media_line_speed, StudyMath.formatClock(media.completedSec * 1000), StudyMath.formatClock(media.totalSec * 1000), media.percent, MediaTime.formatSpeed(media.speed))
                                    else stringResource(R.string.timer_media_line, StudyMath.formatClock(media.completedSec * 1000), StudyMath.formatClock(media.totalSec * 1000), media.percent),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    // Labels shrink rather than wrap, so large text never breaks a word across lines.
                    val oneLine = MaterialTheme.typography.labelLarge
                    val autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = oneLine.fontSize)
                    var confirmDiscard by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { confirmDiscard = true }, modifier = Modifier.size(52.dp)) { Icon(Icons.Default.Close, stringResource(R.string.timer_discard)) }
                        val running = timer.status == TimerStatus.RUNNING
                        FilledTonalButton(onClick = { if (running) vm.pause() else vm.resume() }, modifier = Modifier.weight(1f).height(52.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp))
                            Text(stringResource(if (running) R.string.common_pause else R.string.common_resume), maxLines = 1, softWrap = false, autoSize = autoSize)
                        }
                        Button(onClick = { vm.finish() }, modifier = Modifier.weight(1.25f).height(52.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Default.Stop, null); Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.timer_stop_save), maxLines = 1, softWrap = false, autoSize = autoSize)
                        }
                    }
                    if (confirmDiscard) AlertDialog(
                        onDismissRequest = { confirmDiscard = false },
                        title = { Text(stringResource(R.string.timer_discard_title)) },
                        text = { Text(stringResource(R.string.timer_discard_body)) },
                        confirmButton = { TextButton(onClick = { confirmDiscard = false; vm.discard() }) { Text(stringResource(R.string.timer_discard), color = MaterialTheme.colorScheme.error) } },
                        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.common_cancel)) } },
                    )
                }

                TimerStatus.BREAK -> {
                    Text(stringResource(R.string.break_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    val total = snap.settings.breakMinutes * 60_000f
                    val left = (timer.breakEndsAt - now).coerceAtLeast(0)
                    ProgressRing(1f - left / total, Modifier.size(200.dp), color = Brand.Laurel) {
                        Text(StudyMath.formatClock(left), style = TimerDigits)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.break_text), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { vm.skipBreak() }) { Text(stringResource(R.string.break_skip)) }
                }
            }
        }
    }
}

@Composable
private fun ItemPicker(items: List<LearningItem>, selectedId: Long?, glyphOf: (LearningItem) -> String, onSelect: (Long?) -> Unit) {
    if (items.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val sel = items.firstOrNull { it.id == selectedId }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(sel?.title ?: stringResource(R.string.item_pick), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Glyph(sel?.let(glyphOf) ?: "scroll", size = 18.dp, fallback = Glyphs.CATEGORY_DEFAULT) },
            modifier = Modifier.width(170.dp),
        )
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.item_nothing)) }, onClick = { onSelect(null); open = false })
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.title, maxLines = 1) },
                    leadingIcon = { Glyph(glyphOf(item), size = 20.dp, fallback = Glyphs.CATEGORY_DEFAULT) },
                    onClick = { onSelect(item.id); open = false },
                )
            }
        }
    }
}

@Composable
private fun QuickStats(snap: Snapshot) {
    val streak = snap.streak
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("sundial", StudyMath.formatDuration(snap.todaySeconds, true), stringResource(R.string.tile_today), Modifier.weight(1f))
        StatTile(
            "flame", pluralStringResource(R.plurals.tile_days, streak.current, streak.current),
            if (!streak.studiedToday && streak.current > 0) stringResource(R.string.tile_extend) else stringResource(R.string.tile_streak),
            Modifier.weight(1f),
            container = com.rayaurelius.sophinity.ui.theme.LocalGold.current.container,
        )
        StatTile("hourglass", StudyMath.formatDuration(snap.weekSeconds, true), stringResource(R.string.tile_week), Modifier.weight(1f))
    }
}

@Composable
private fun GoalsGlance(snap: Snapshot, progress: List<GoalProgress>, onOpenGoals: () -> Unit) {
    SectionCard(title = stringResource(R.string.goals_title), action = { TextButton(onClick = onOpenGoals) { Text(stringResource(if (progress.isEmpty()) R.string.goals_set_one else R.string.goals_all)) } }) {
        if (progress.isEmpty()) {
            Text(
                stringResource(R.string.goals_empty_hint),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            progress.take(3).forEachIndexed { i, p ->
                if (i > 0) Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goalTitle(snap, p.goal), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (p.met) stringResource(R.string.goal_done) else goalAmount(p),
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

@Composable
fun goalAmount(p: GoalProgress): String = when (p.goal.kind) {
    com.rayaurelius.sophinity.data.db.GoalKind.TIME ->
        stringResource(R.string.amount_of, StudyMath.formatDuration(p.current * 60L, true), StudyMath.formatDuration(p.target * 60L, true))
    com.rayaurelius.sophinity.data.db.GoalKind.ITEMS -> stringResource(R.string.amount_of, p.current.toString(), p.target.toString())
}

@Composable
private fun ContinueLearning(snap: Snapshot, timer: TimerState, vm: TodayViewModel) {
    Column {
        Text(stringResource(R.string.continue_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(snap.activeItems, key = { it.id }) { item ->
                OutlinedCard(Modifier.width(200.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        GlyphTile(snap.categoryOf(item).glyph, size = 36.dp, fallback = Glyphs.CATEGORY_DEFAULT)
                        Spacer(Modifier.height(4.dp))
                        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(40.dp))
                        val total = item.totalUnits
                        val media = snap.mediaProgress(item)
                        if (media != null) {
                            Spacer(Modifier.height(6.dp))
                            MediaMeter(media, compact = true)
                        } else if (total != null && total > 0 && !com.rayaurelius.sophinity.domain.MediaTime.supportsRuntime(item.type)) {
                            Spacer(Modifier.height(6.dp))
                            LinearMeter(item.progressUnits.toFloat() / total, Modifier.fillMaxWidth())
                            Text(stringResource(R.string.units_of, item.progressUnits, total, snap.categoryOf(item).unit.str()), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.logged, StudyMath.formatDuration(snap.secondsForItem(item.id))), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { if (timer.isActive) vm.setSubject(item.subjectId, item.id) else vm.start(TimerMode.STOPWATCH, item.subjectId, item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(if (timer.isActive) R.string.switch_to_this else R.string.learn_this)) }
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
                Text(stringResource(R.string.wisdom_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.wisdom_quote, stringResource(wisdom.text)),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = LocalHeadingFont.current, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.wisdom_by, stringResource(wisdom.who), stringResource(wisdom.source)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(14.dp))
                Row {
                    Glyph(tip.glyph, size = 28.dp, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.tip_try, stringResource(tip.title)), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(stringResource(tip.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
            Text(stringResource(R.string.reflect_nice), style = MaterialTheme.typography.headlineSmall)
            Text(
                subject?.let { stringResource(R.string.reflect_saved_subject, StudyMath.formatDuration(session.durationSec), it.name) }
                    ?: stringResource(R.string.reflect_saved, StudyMath.formatDuration(session.durationSec)),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.reflect_focus_q), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FocusRating(focus) { focus = it }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.reflect_learned), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.reflect_learned_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.reflect_placeholder)) }, minLines = 2)
            val item = session.itemId?.let { snap.itemsById[it] }
            val media = item?.let { snap.mediaProgress(it) }
            if (item != null && media != null && item.status != ItemStatus.DONE) {
                Spacer(Modifier.height(20.dp))
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Glyph(snap.categoryOf(item).glyph, size = 18.dp, fallback = Glyphs.CATEGORY_DEFAULT)
                            Spacer(Modifier.width(6.dp))
                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(6.dp))
                        MediaMeter(media)
                        if (media.isComplete) {
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = { vm.markFinished(item) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.reflect_mark_finished))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (timer.status == TimerStatus.IDLE && session.durationSec >= (snap.settings.focusMinutes * 60L) - 5) {
                    FilledTonalButton(onClick = { vm.saveReflection(session, note, focus); vm.startBreak() }, Modifier.weight(1f)) {
                        Text(stringResource(R.string.reflect_save_break))
                    }
                }
                Button(onClick = { vm.saveReflection(session, note, focus) }, Modifier.weight(1f)) { Text(stringResource(R.string.common_save)) }
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
        title = { Text(stringResource(R.string.manual_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                OutlinedCard(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph("sundial", size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(date.format(rememberDateFormat("EEEMMMdyyyy")))
                    }
                }
                Text(stringResource(R.string.sessions_link_item), style = MaterialTheme.typography.labelLarge)
                LibraryItemPicker(snap, itemId) { id ->
                    itemId = id
                    id?.let { snap.itemsById[it]?.subjectId }?.let { subjectId = it }
                }
                SubjectPicker(snap.activeSubjects, subjectId, { subjectId = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stringResource(R.string.field_hours), hours, { hours = it?.coerceAtMost(16) }, Modifier.weight(1f))
                    NumberField(stringResource(R.string.field_minutes), minutes, { minutes = it?.coerceAtMost(59) }, Modifier.weight(1f))
                }
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.manual_note)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = total > 0, onClick = {
                val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val noon = date.atTime(12, 0).atZone(snap.zone).toInstant().toEpochMilli()
                onSave(subjectId, itemId, noon, total, note)
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
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
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateMillis = it }; pickDate = false }) { Text(stringResource(R.string.common_ok)) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.common_cancel)) } },
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
            icon = { LaurelMedal(fresh.first().glyph, earned = true, size = 88.dp) },
            title = { Text(pluralStringResource(R.plurals.laurels_earned, fresh.size, fresh.size), textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    fresh.take(5).forEach { m ->
                        if (fresh.size > 1) Row(verticalAlignment = Alignment.CenterVertically) {
                            Glyph(m.glyph, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(m.title.str(), style = MaterialTheme.typography.titleMedium)
                        } else Text(m.title.str(), style = MaterialTheme.typography.titleMedium)
                        Text(m.description.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (fresh.size > 5) Text(stringResource(R.string.laurels_more, fresh.size - 5))
                }
            },
            confirmButton = { Button(onClick = { onSeen(fresh.map { it.key }) }) { Text(stringResource(R.string.laurels_keep_going)) } },
        )
        Confetti()
    }
}

@Composable
private fun TitleCelebration(title: com.rayaurelius.sophinity.domain.LearnerTitle, onSeen: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AlertDialog(
            onDismissRequest = onSeen,
            icon = { LaurelMedal(title.glyph, earned = true, size = 96.dp) },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.title_new), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(title.name.str(), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.title_greek_meaning, title.greek, title.meaning.str()), style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalHeadingFont.current),
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (title.rank == com.rayaurelius.sophinity.domain.Titles.highest.rank)
                            stringResource(R.string.title_highest)
                        else stringResource(R.string.title_keep_going),
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = onSeen) { Text(stringResource(R.string.title_onward)) } },
        )
        Confetti()
    }
}

/**
 * Forgot to press Start? Pick how long you've already been learning; the open session's
 * stopwatch then starts from there. Quick picks cover the usual cases, the fields anything else.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun HeadStartSheet(initialMin: Int, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    var hours by remember { mutableStateOf<Int?>(initialMin / 60) }
    var minutes by remember { mutableStateOf<Int?>(initialMin % 60) }
    val total = (hours ?: 0) * 60 + (minutes ?: 0)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.timer_start_from_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.timer_start_from_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(5, 10, 15, 30, 45, 60).forEach { m ->
                    SuggestionChip(onClick = { hours = m / 60; minutes = m % 60 }, label = { Text(StudyMath.formatDuration(m * 60L)) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(stringResource(R.string.field_hours), hours, { hours = it?.coerceAtMost(15) }, Modifier.weight(1f))
                NumberField(stringResource(R.string.field_minutes), minutes, { minutes = it?.coerceAtMost(59) }, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initialMin > 0) TextButton(onClick = { onSet(0) }) { Text(stringResource(R.string.timer_start_from_reset)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = { onSet(total) }, enabled = total > 0) { Text(stringResource(R.string.common_ok)) }
            }
        }
    }
}
