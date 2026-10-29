package com.rayaurelius.sophinity.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.Note
import com.rayaurelius.sophinity.ui.components.Choices
import com.rayaurelius.sophinity.ui.components.EmptyState
import com.rayaurelius.sophinity.ui.components.LibraryItemPicker
import com.rayaurelius.sophinity.ui.components.PageHeader
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class NotesViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val notes = c.repo.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun save(note: Note) = viewModelScope.launch { c.repo.saveNote(note) }
    fun delete(note: Note) = viewModelScope.launch { c.repo.deleteNote(note) }
}

/** Filter value meaning "notes not linked to anything". `null` means every note. */
private const val UNLINKED = -1L

/** Pure filtering, shared by the screen and its tests. */
object NoteFilter {
    fun apply(notes: List<Note>, itemFilter: Long?, query: String, itemTitle: (Long) -> String?): List<Note> {
        val q = query.trim()
        return notes
            .filter { n ->
                when (itemFilter) {
                    null -> true
                    UNLINKED -> n.itemId == null
                    else -> n.itemId == itemFilter
                }
            }
            .filter { n ->
                q.isEmpty() || n.title.contains(q, true) || n.body.contains(q, true) ||
                    (n.itemId?.let(itemTitle)?.contains(q, true) == true)
            }
            .sortedByDescending { it.updatedAt }
    }
}

/** Every note, newest first; filter by what it's linked to or search the text. */
@Composable
fun NotesScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { NotesViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    val notes = vm.notes.collectAsStateWithLifecycle().value ?: return
    var itemFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    var confirmDelete by remember { mutableStateOf<Note?>(null) }

    // Only items that have notes are worth filtering by; a stale filter falls back to "all".
    val linkedIds = notes.mapNotNull { it.itemId }.toSet()
    val linkedItems = snap.items.filter { it.id in linkedIds }.sortedBy { it.title.lowercase() }
    val hasUnlinked = notes.any { it.itemId == null }
    if (itemFilter != null && itemFilter != UNLINKED && itemFilter !in linkedIds) itemFilter = null
    val shown = NoteFilter.apply(notes, itemFilter, query) { snap.itemsById[it]?.title }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { PageHeader(stringResource(R.string.notes_title)) }
            if (notes.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        query, { query = it }, singleLine = true,
                        placeholder = { Text(stringResource(R.string.notes_search)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (query.isNotEmpty()) ({
                            IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, stringResource(R.string.notes_clear_search)) }
                        }) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (linkedItems.isNotEmpty()) item {
                    val options = listOf<Long?>(null) + listOfNotNull(UNLINKED.takeIf { hasUnlinked }) + linkedItems.map { it.id }
                    Choices(
                        options, itemFilter, { itemFilter = it },
                        label = { id ->
                            when (id) {
                                null -> stringResource(R.string.notes_all)
                                UNLINKED -> stringResource(R.string.notes_unlinked)
                                else -> snap.itemsById[id]?.title.orEmpty()
                            }
                        },
                        icon = { id ->
                            val item = id?.takeIf { it != UNLINKED }?.let { snap.itemsById[it] }
                            Glyph(item?.let { snap.categoryOf(it).glyph } ?: "tablet", size = 18.dp, fallback = Glyphs.CATEGORY_DEFAULT)
                        },
                        title = stringResource(R.string.notes_linked_to), scroll = true, titleOverBubbles = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        pluralStringResource(R.plurals.notes_count, shown.size, shown.size),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                notes.isEmpty() -> item {
                    EmptyState("tablet", stringResource(R.string.notes_empty_title), stringResource(R.string.notes_empty_body))
                }
                shown.isEmpty() -> item {
                    EmptyState("tablet", stringResource(R.string.notes_no_match_title), stringResource(R.string.notes_no_match_body))
                }
            }
            items(shown, key = { it.id }) { n -> NoteCard(snap, n, onOpen = { editing = n }) }
        }
        ExtendedFloatingActionButton(
            onClick = {
                // A new note starts linked to whatever item the list is filtered to.
                editing = Note(itemId = itemFilter?.takeIf { it != UNLINKED })
            },
            icon = { Glyph("quill", size = 22.dp) }, text = { Text(stringResource(R.string.notes_new)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { n ->
        NoteEditorSheet(
            snap, n, onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = { editing = null; confirmDelete = n },
        )
    }
    confirmDelete?.let { n -> DeleteNoteDialog(n, onConfirm = { vm.delete(n); confirmDelete = null }, onDismiss = { confirmDelete = null }) }
}

/** A note's title (or first line), a few lines of text, what it's linked to and when it was edited. */
@Composable
fun NoteCard(snap: Snapshot, note: Note, onOpen: () -> Unit, showItem: Boolean = true, modifier: Modifier = Modifier) {
    val item = note.itemId?.let { snap.itemsById[it] }
    val edited = Instant.ofEpochMilli(note.updatedAt).atZone(snap.zone)
    val sameYear = edited.year == snap.today.year
    // Without a title the first line is the heading, so don't repeat it in the preview.
    val preview = if (note.title.isBlank()) note.body.trim().lineSequence().drop(1).joinToString("\n").trim() else note.body.trim()
    ElevatedCard(onClick = onOpen, modifier = modifier.fillMaxWidth().testTag("note")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                note.heading.ifBlank { stringResource(R.string.notes_untitled) },
                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (preview.isNotEmpty()) {
                Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showItem && item != null) {
                    Glyph(snap.categoryOf(item).glyph, size = 16.dp, fallback = Glyphs.CATEGORY_DEFAULT, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(10.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    edited.format(rememberDateFormat(if (sameYear) "MMMd" else "yMMMd")),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline, maxLines = 1,
                )
            }
        }
    }
}

/** Write or edit a note and choose what in the library it's about. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(snap: Snapshot, initial: Note, onDismiss: () -> Unit, onSave: (Note) -> Unit, onDelete: () -> Unit) {
    var n by remember { mutableStateOf(initial) }
    val isNew = initial.id == 0L
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(if (isNew) R.string.notes_new else R.string.notes_edit), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                n.title, { n = n.copy(title = it) }, singleLine = true,
                label = { Text(stringResource(R.string.notes_field_title)) }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                n.body, { n = n.copy(body = it) }, minLines = 6,
                label = { Text(stringResource(R.string.notes_field_body)) }, modifier = Modifier.fillMaxWidth().testTag("note_body"),
            )
            Text(stringResource(R.string.notes_linked_to), style = MaterialTheme.typography.titleSmall)
            LibraryItemPicker(snap, n.itemId) { n = n.copy(itemId = it) }
            Text(stringResource(R.string.notes_link_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Button(enabled = !n.isEmpty, onClick = { onSave(n) }) { Text(stringResource(R.string.common_save)) }
            }
        }
    }
}

@Composable
fun DeleteNoteDialog(note: Note, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, null) },
        title = { Text(stringResource(R.string.notes_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(note.heading.ifBlank { stringResource(R.string.notes_untitled) }, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.notes_delete_body), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/**
 * The notes linked to one library item, shown inside that item's editor, with a button to write
 * another. New items have no id yet, so they get a hint to save first.
 */
@Composable
fun ItemNotesSection(snap: Snapshot, itemId: Long) {
    val vm = appViewModel { NotesViewModel(it) }
    val all = vm.notes.collectAsStateWithLifecycle().value.orEmpty()
    val notes = all.filter { it.itemId == itemId }
    var editing by remember { mutableStateOf<Note?>(null) }
    var confirmDelete by remember { mutableStateOf<Note?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph("tablet", size = 20.dp, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.lib_notes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (notes.isNotEmpty()) Text(pluralStringResource(R.plurals.notes_count, notes.size, notes.size),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (itemId == 0L) {
            Text(stringResource(R.string.notes_save_item_first), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            notes.forEach { n ->
                OutlinedCard(onClick = { editing = n }, modifier = Modifier.fillMaxWidth().testTag("item_note")) {
                    Column(Modifier.padding(12.dp)) {
                        Text(n.heading.ifBlank { stringResource(R.string.notes_untitled) }, style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val rest = if (n.title.isBlank()) n.body.trim().lineSequence().drop(1).joinToString(" ").trim() else n.body.trim()
                        if (rest.isNotEmpty()) Text(rest, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            OutlinedButton(onClick = { editing = Note(itemId = itemId) }, modifier = Modifier.fillMaxWidth()) {
                Glyph("quill", size = 18.dp); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.notes_write))
            }
            Spacer(Modifier.height(2.dp))
        }
    }

    editing?.let { n ->
        NoteEditorSheet(snap, n, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null }, onDelete = { editing = null; confirmDelete = n })
    }
    confirmDelete?.let { n -> DeleteNoteDialog(n, onConfirm = { vm.delete(n); confirmDelete = null }, onDismiss = { confirmDelete = null }) }
}
