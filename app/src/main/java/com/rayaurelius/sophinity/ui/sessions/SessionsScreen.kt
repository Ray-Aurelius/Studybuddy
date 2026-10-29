package com.rayaurelius.sophinity.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.ui.components.FocusRating
import com.rayaurelius.sophinity.ui.components.SubjectPicker
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.SessionSource
import com.rayaurelius.sophinity.data.db.StudySession
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.ui.components.EmptyState
import com.rayaurelius.sophinity.ui.components.FocusColumns
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.GlyphTile
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import com.rayaurelius.sophinity.ui.today.ManualSessionDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class SessionsViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun delete(s: StudySession) = viewModelScope.launch { c.repo.deleteSession(s) }
    fun update(s: StudySession) = viewModelScope.launch { c.repo.updateSession(s) }
    fun addManual(subjectId: Long?, itemId: Long?, startedAt: Long, minutes: Int, note: String) = viewModelScope.launch {
        if (minutes <= 0) return@launch
        c.repo.addSession(
            StudySession(
                subjectId = subjectId, itemId = itemId, startedAt = startedAt,
                durationSec = minutes * 60L, note = note.trim(), source = SessionSource.MANUAL,
            )
        )
    }
}

private enum class Span(val label: Int) {
    ALL(R.string.lib_all), WEEK(R.string.stats_this_week), MONTH(R.string.sessions_this_month), YEAR(R.string.sessions_this_year)
}

/** Every learning session, newest first, grouped by day. Deleting one always asks first. */
@Composable
fun SessionsScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { SessionsViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    var span by rememberSaveable { mutableStateOf(Span.ALL) }
    var subjectFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf<StudySession?>(null) }
    var editing by remember { mutableStateOf<StudySession?>(null) }
    var showManual by remember { mutableStateOf(false) }

    val from: LocalDate? = when (span) {
        Span.ALL -> null
        Span.WEEK -> StudyMath.weekRange(snap.today, snap.settings.weekStart).start
        Span.MONTH -> StudyMath.monthRange(snap.today).start
        Span.YEAR -> StudyMath.yearRange(snap.today).start
    }
    val shown = snap.sessions
        .filter { from == null || !StudyMath.dayOf(it.startedAt, snap.zone).isBefore(from) }
        .filter { subjectFilter == null || it.subjectId == subjectFilter }
        .sortedByDescending { it.startedAt }
    val byDay = shown.groupBy { StudyMath.dayOf(it.startedAt, snap.zone) }
    val usedSubjects = snap.sessions.mapNotNull { it.subjectId }.toSet()
    val subjects = snap.subjects.filter { it.id in usedSubjects }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Text(stringResource(R.string.sessions_title), style = MaterialTheme.typography.headlineMedium) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Span.entries) { s -> FilterChip(span == s, { span = s }, { Text(stringResource(s.label)) }) }
                    items(subjects, key = { it.id }) { sub ->
                        FilterChip(
                            subjectFilter == sub.id, { subjectFilter = if (subjectFilter == sub.id) null else sub.id }, { Text(sub.name) },
                            leadingIcon = { Glyph(sub.icon, size = 18.dp, tint = Color(sub.color)) },
                        )
                    }
                }
            }
            item {
                val total = shown.sumOf { it.durationSec }
                Text(
                    pluralStringResource(R.plurals.sessions_summary, shown.size, shown.size, StudyMath.formatDuration(total, true)),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (shown.isEmpty()) {
                item { EmptyState("hourglass", stringResource(R.string.stats_empty_title), stringResource(R.string.stats_empty_body)) }
            }
            byDay.forEach { (day, list) ->
                item(key = "d$day") { DayHeader(snap, day, list.sumOf { it.durationSec }) }
                items(list, key = { it.id }) { s -> SessionCard(snap, s, onOpen = { editing = s }, onDelete = { confirmDelete = s }) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showManual = true },
            icon = { Glyph("quill", size = 22.dp) }, text = { Text(stringResource(R.string.manual_title)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { s ->
        SessionEditor(
            snap, s, onDismiss = { editing = null },
            onSave = { vm.update(it); editing = null },
            onDelete = { editing = null; confirmDelete = s },
        )
    }
    confirmDelete?.let { s -> DeleteDialog(snap, s, onConfirm = { vm.delete(s); confirmDelete = null }, onDismiss = { confirmDelete = null }) }
    if (showManual) ManualSessionDialog(snap, onDismiss = { showManual = false }) { subj, item, start, mins, note ->
        vm.addManual(subj, item, start, mins, note); showManual = false
    }
}

@Composable
private fun DayHeader(snap: Snapshot, day: LocalDate, seconds: Long) {
    val label = when (day) {
        snap.today -> stringResource(R.string.stats_today)
        snap.today.minusDays(1) -> stringResource(R.string.stats_yesterday)
        else -> day.format(rememberDateFormat(if (day.year == snap.today.year) "EEEEMMMMd" else "yMMMMEEEEd"))
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(StudyMath.formatDuration(seconds, true), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** What was studied: the item's title, else the subject, else a plain "Learning session". */
@Composable
private fun sessionTitle(snap: Snapshot, s: StudySession): String {
    val item = s.itemId?.let { snap.itemsById[it] }
    val subject = s.subjectId?.let { snap.subjectsById[it] }
    return item?.title ?: subject?.name ?: stringResource(R.string.stats_learning_session)
}

private fun sessionGlyph(snap: Snapshot, s: StudySession): String {
    val item = s.itemId?.let { snap.itemsById[it] }
    val subject = s.subjectId?.let { snap.subjectsById[it] }
    return item?.let { snap.categoryOf(it).glyph } ?: subject?.icon ?: "hourglass"
}

@Composable
private fun SessionCard(snap: Snapshot, s: StudySession, onOpen: () -> Unit, onDelete: () -> Unit) {
    val item = s.itemId?.let { snap.itemsById[it] }
    val subject = s.subjectId?.let { snap.subjectsById[it] }
    val time = Instant.ofEpochMilli(s.startedAt).atZone(snap.zone)
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("session")) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            GlyphTile(
                sessionGlyph(snap, s), size = 40.dp, fallback = Glyphs.CATEGORY_DEFAULT,
                tint = subject?.let { Color(it.color) } ?: MaterialTheme.colorScheme.onSecondaryContainer,
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sessionTitle(snap, s), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // The subject is shown here only when the title is an item's.
                if (item != null && subject != null) {
                    Text(subject.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(time.format(rememberDateFormat("jmm")), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (s.source == SessionSource.MANUAL) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(stringResource(R.string.sessions_manual), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    s.focus?.let { f ->
                        Spacer(Modifier.width(10.dp))
                        val focusLabel = stringResource(R.string.sessions_focus, f)
                        Box(Modifier.semantics { contentDescription = focusLabel }) {
                            FocusColumns(f, MaterialTheme.colorScheme.primary, Modifier.width(24.dp).height(14.dp))
                        }
                    }
                }
                if (s.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("“${s.note}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(StudyMath.formatDuration(s.durationSec), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 10.dp, top = 2.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.stats_delete_session), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/** Names the exact session (what, when, how long) so nobody deletes the wrong one. */
@Composable
private fun DeleteDialog(snap: Snapshot, s: StudySession, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val time = Instant.ofEpochMilli(s.startedAt).atZone(snap.zone)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, null) },
        title = { Text(stringResource(R.string.stats_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(sessionTitle(snap, s), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${time.format(rememberDateFormat("yMMMEd"))} · ${time.format(rememberDateFormat("jmm"))}",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(StudyMath.formatDuration(s.durationSec), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(stringResource(R.string.sessions_delete_warning), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/**
 * Edit a logged session: link it to something in the library (so its time counts toward that
 * item's progress), change the subject, the reflection or the focus rating.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionEditor(snap: Snapshot, initial: StudySession, onDismiss: () -> Unit, onSave: (StudySession) -> Unit, onDelete: () -> Unit) {
    var s by remember { mutableStateOf(initial) }
    val time = Instant.ofEpochMilli(s.startedAt).atZone(snap.zone)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.sessions_edit), style = MaterialTheme.typography.headlineSmall)
            Text(
                "${time.format(rememberDateFormat("yMMMEd"))} · ${time.format(rememberDateFormat("jmm"))} · ${StudyMath.formatDuration(s.durationSec)}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.sessions_link_item), style = MaterialTheme.typography.titleSmall)
            LibraryItemPicker(snap, s.itemId) { id ->
                val item = id?.let { snap.itemsById[it] }
                // Linking to an item also files the session under that item's subject, unless one is already set.
                s = s.copy(itemId = id, subjectId = s.subjectId ?: item?.subjectId)
            }
            Text(stringResource(R.string.sessions_link_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SubjectPicker(snap.activeSubjects, s.subjectId, { s = s.copy(subjectId = it) })
            Text(stringResource(R.string.reflect_focus_q), style = MaterialTheme.typography.titleSmall)
            FocusRating(s.focus) { s = s.copy(focus = it) }
            OutlinedTextField(
                s.note, { s = s.copy(note = it) }, label = { Text(stringResource(R.string.reflect_learned)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(onClick = { onSave(s.copy(note = s.note.trim())) }) { Text(stringResource(R.string.common_save)) }
            }
        }
    }
}

/** Every library item (in progress first, then want-to, then finished), or none. */
@Composable
private fun LibraryItemPicker(snap: Snapshot, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val order = listOf(ItemStatus.ACTIVE, ItemStatus.WANT, ItemStatus.DONE)
    val items = snap.items.sortedWith(compareBy({ order.indexOf(it.status) }, { it.title.lowercase() }))
    val sel = selectedId?.let { snap.itemsById[it] }
    Box {
        OutlinedCard(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(sel?.let { snap.categoryOf(it).glyph } ?: "scroll", size = 22.dp, fallback = Glyphs.CATEGORY_DEFAULT,
                    tint = if (sel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(sel?.title ?: stringResource(R.string.sessions_not_linked), Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (sel != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(open, { open = false }, Modifier.heightIn(max = 360.dp)) {
            DropdownMenuItem(text = { Text(stringResource(R.string.sessions_not_linked)) }, onClick = { onSelect(null); open = false })
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(item.status.labelRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Glyph(snap.categoryOf(item).glyph, size = 20.dp, fallback = Glyphs.CATEGORY_DEFAULT) },
                    onClick = { onSelect(item.id); open = false },
                )
            }
        }
    }
}
