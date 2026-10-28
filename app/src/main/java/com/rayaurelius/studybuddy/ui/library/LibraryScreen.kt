@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rayaurelius.studybuddy.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.studybuddy.AppContainer
import com.rayaurelius.studybuddy.data.Snapshot
import com.rayaurelius.studybuddy.data.db.ItemStatus
import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.LearningItem
import com.rayaurelius.studybuddy.domain.StudyMath
import com.rayaurelius.studybuddy.ui.components.EmptyState
import com.rayaurelius.studybuddy.ui.components.LinearMeter
import com.rayaurelius.studybuddy.ui.components.NumberField
import com.rayaurelius.studybuddy.ui.components.StarRating
import com.rayaurelius.studybuddy.ui.components.SubjectPicker
import com.rayaurelius.studybuddy.ui.components.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun save(item: LearningItem) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val fixed = item.copy(
            startedAt = item.startedAt ?: if (item.status != ItemStatus.WANT) now else null,
            completedAt = if (item.status == ItemStatus.DONE) item.completedAt ?: now else null,
            progressUnits = if (item.status == ItemStatus.DONE && item.totalUnits != null) item.totalUnits else item.progressUnits,
        )
        c.repo.saveItem(fixed)
    }
    fun delete(item: LearningItem) = viewModelScope.launch { c.repo.deleteItem(item) }
}

@Composable
fun LibraryScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { LibraryViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    var status by rememberSaveable { mutableStateOf(ItemStatus.ACTIVE) }
    var type by rememberSaveable { mutableStateOf<ItemType?>(null) }
    var editing by remember { mutableStateOf<LearningItem?>(null) }

    val list = snap.items.filter { it.status == status && (type == null || it.type == type) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Text("Library", style = MaterialTheme.typography.headlineMedium) }
            item { YearTally(snap) }
            item {
                SecondaryTabRow(selectedTabIndex = ItemStatus.entries.indexOf(status)) {
                    ItemStatus.entries.forEach { st ->
                        val n = snap.items.count { it.status == st }
                        Tab(status == st, { status = st }, text = { Text("${st.label} ($n)") })
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(type == null, { type = null }, { Text("All") }) }
                    items(ItemType.entries) { t -> FilterChip(type == t, { type = if (type == t) null else t }, { Text("${t.emoji} ${t.plural}") }) }
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        "📚",
                        when (status) {
                            ItemStatus.ACTIVE -> "Nothing in progress"
                            ItemStatus.WANT -> "Your want-to list is empty"
                            ItemStatus.DONE -> "Nothing finished yet"
                        },
                        "Add the books you're reading, podcasts you follow, and seminars you attend. Everything you learn from counts.",
                    )
                }
            } else {
                items(list, key = { it.id }) { item -> ItemCard(snap, item) { editing = item } }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = LearningItem(type = type ?: ItemType.BOOK, title = "", status = status) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { item ->
        ItemEditorSheet(
            snap, item,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
        )
    }
}

@Composable
private fun YearTally(snap: Snapshot) {
    val year = snap.today.year
    val done = snap.items.filter { it.status == ItemStatus.DONE && it.completedAt != null && StudyMath.dayOf(it.completedAt, snap.zone).year == year }
    if (done.isEmpty()) return
    val counts = done.groupingBy { it.type }.eachCount()
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Finished in $year", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ItemType.entries.filter { (counts[it] ?: 0) > 0 }.forEach { t ->
                    Text("${t.emoji} ${counts[t]} ${if (counts[t] == 1) t.label.lowercase() else t.plural.lowercase()}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ItemCard(snap: Snapshot, item: LearningItem, onClick: () -> Unit) {
    val subject = item.subjectId?.let { snap.subjectsById[it] }
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Text(item.type.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(item.creator.takeIf { it.isNotBlank() }, subject?.let { "${it.emoji} ${it.name}" })
                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val total = item.totalUnits
                if (item.status == ItemStatus.ACTIVE && total != null && total > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearMeter(item.progressUnits.toFloat() / total, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(2.dp))
                    Text("${item.progressUnits} of $total ${item.type.unitLabel} · ${item.progressUnits * 100 / total}%",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val logged = snap.secondsForItem(item.id)
                if (logged > 0) Text("${StudyMath.formatDuration(logged)} studied", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                if (item.status == ItemStatus.DONE) {
                    Spacer(Modifier.height(4.dp))
                    StarRating(item.rating, size = 16)
                    if (item.takeaway.isNotBlank()) Text("💡 ${item.takeaway}", style = MaterialTheme.typography.bodySmall, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ItemEditorSheet(
    snap: Snapshot,
    initial: LearningItem,
    onDismiss: () -> Unit,
    onSave: (LearningItem) -> Unit,
    onDelete: (LearningItem) -> Unit,
) {
    var item by remember { mutableStateOf(initial) }
    val isNew = initial.id == 0L
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) "Add to library" else "Edit", style = MaterialTheme.typography.headlineSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ItemType.entries.forEach { t -> FilterChip(item.type == t, { item = item.copy(type = t) }, { Text("${t.emoji} ${t.label}") }) }
            }
            OutlinedTextField(item.title, { item = item.copy(title = it) }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                item.creator, { item = item.copy(creator = it) },
                label = { Text(when (item.type) { ItemType.BOOK, ItemType.ARTICLE -> "Author"; ItemType.PODCAST -> "Host / show"; ItemType.SEMINAR -> "Speaker / organizer"; else -> "Creator" }) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemStatus.entries.forEach { st -> FilterChip(item.status == st, { item = item.copy(status = st) }, { Text(st.label) }) }
            }
            SubjectPicker(snap.activeSubjects, item.subjectId, { item = item.copy(subjectId = it) })
            if (item.status != ItemStatus.WANT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Done", item.progressUnits, { item = item.copy(progressUnits = it ?: 0) }, Modifier.weight(1f))
                    NumberField("Total ${item.type.unitLabel}", item.totalUnits, { item = item.copy(totalUnits = it) }, Modifier.weight(1f))
                }
            }
            if (item.status == ItemStatus.DONE) {
                Text("Your rating", style = MaterialTheme.typography.titleSmall)
                StarRating(item.rating, { item = item.copy(rating = it) })
                OutlinedTextField(
                    item.takeaway, { item = item.copy(takeaway = it) },
                    label = { Text("Biggest takeaway") },
                    supportingText = { Text("Summarizing in one line is a powerful way to remember it.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(item.notes, { item = item.copy(notes = it) }, label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) TextButton(onClick = { onDelete(initial) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(enabled = item.title.isNotBlank(), onClick = { onSave(item.copy(title = item.title.trim(), creator = item.creator.trim())) }) {
                    Text("Save")
                }
            }
        }
    }
}
