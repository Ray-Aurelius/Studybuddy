@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rayaurelius.sophinity.ui.library

import com.rayaurelius.sophinity.domain.str
import com.rayaurelius.sophinity.R
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import com.rayaurelius.sophinity.ui.components.Choices
import com.rayaurelius.sophinity.ui.components.PageHeader
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.GlyphTile
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.AssistChip
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
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.domain.CategoryInfo
import com.rayaurelius.sophinity.ui.components.CategoryDialog
import com.rayaurelius.sophinity.domain.MediaTime
import com.rayaurelius.sophinity.ui.components.DurationFields
import com.rayaurelius.sophinity.ui.components.EmptyState
import com.rayaurelius.sophinity.ui.components.MediaMeter
import com.rayaurelius.sophinity.ui.components.LinearMeter
import com.rayaurelius.sophinity.ui.components.NumberField
import com.rayaurelius.sophinity.ui.components.StarRating
import com.rayaurelius.sophinity.ui.components.SubjectPicker
import com.rayaurelius.sophinity.ui.components.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun save(item: LearningItem, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val fixed = item.copy(
            startedAt = item.startedAt ?: if (item.status != ItemStatus.WANT) now else null,
            completedAt = if (item.status == ItemStatus.DONE) item.completedAt ?: now else null,
            progressUnits = if (item.status == ItemStatus.DONE && item.totalUnits != null) item.totalUnits else item.progressUnits,
        )
        onSaved(c.repo.saveItem(fixed))
    }
    fun delete(item: LearningItem) = viewModelScope.launch { c.repo.deleteItem(item) }
    fun saveCategory(cat: CustomCategory, onSaved: (Long) -> Unit = {}) = viewModelScope.launch { onSaved(c.repo.saveCategory(cat)) }
}

@Composable
fun LibraryScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { LibraryViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    var status by rememberSaveable { mutableStateOf(ItemStatus.ACTIVE) }
    var filterKey by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<LearningItem?>(null) }
    var newCategory by remember { mutableStateOf(false) }
    val filter = snap.categories.firstOrNull { it.key == filterKey }

    val list = snap.items.filter { it.status == status && (filter == null || filter.matches(it)) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { PageHeader(stringResource(R.string.lib_title)) }
            item { YearTally(snap) }
            item {
                SecondaryTabRow(selectedTabIndex = ItemStatus.entries.indexOf(status)) {
                    ItemStatus.entries.forEach { st ->
                        val n = snap.items.count { it.status == st }
                        Tab(status == st, { status = st }, text = {
                            Text(
                                stringResource(R.string.lib_tab, stringResource(st.labelRes), n), maxLines = 1, softWrap = false,
                                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = MaterialTheme.typography.titleSmall.fontSize),
                            )
                        })
                    }
                }
            }
            item {
                // Only offer categories that have something in them, plus the built-ins people use most.
                val used = snap.items.map { snap.categoryOf(it).key }.toSet()
                val shown = listOf<CategoryInfo?>(null) + snap.categories.filter { it.key in used || it.type in pinned || it.key == filterKey }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choices(
                        shown, filter, { filterKey = it?.key },
                        label = { it?.plural?.str() ?: stringResource(R.string.lib_all) },
                        icon = { Glyph(it?.glyph ?: "scroll_stack", size = 20.dp, fallback = Glyphs.CATEGORY_DEFAULT) },
                        title = stringResource(R.string.choice_show), scroll = true, titleOverBubbles = false,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { newCategory = true }) { Icon(Icons.Default.Add, stringResource(R.string.lib_new_category)) }
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        "scroll_stack",
                        when (status) {
                            ItemStatus.ACTIVE -> stringResource(R.string.lib_empty_active)
                            ItemStatus.WANT -> stringResource(R.string.lib_empty_want)
                            ItemStatus.DONE -> stringResource(R.string.lib_empty_done)
                        },
                        stringResource(R.string.lib_empty_body),
                    )
                }
            } else {
                items(list, key = { it.id }) { item -> ItemCard(snap, item) { editing = item } }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = LearningItem(type = filter?.type ?: ItemType.BOOK, categoryId = filter?.customId, title = "", status = status) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(stringResource(R.string.common_add)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { item ->
        ItemEditorSheet(
            snap, item,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
            onCreateCategory = { cat, picked -> vm.saveCategory(cat, picked) },
        )
    }
    if (newCategory) {
        CategoryDialog(
            CustomCategory(name = ""),
            onDismiss = { newCategory = false },
            onSave = { cat -> vm.saveCategory(cat) { id -> filterKey = "c$id" }; newCategory = false },
        )
    }
}

private val pinned = setOf(ItemType.BOOK, ItemType.AUDIOBOOK, ItemType.PODCAST, ItemType.SEMINAR, ItemType.COURSE)

@Composable
private fun YearTally(snap: Snapshot) {
    val year = snap.today.year
    val done = snap.items.filter { it.status == ItemStatus.DONE && it.completedAt != null && StudyMath.dayOf(it.completedAt, snap.zone).year == year }
    if (done.isEmpty()) return
    val counts = done.groupingBy { snap.categoryOf(it) }.eachCount()
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.lib_finished_in, year), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                counts.entries.sortedByDescending { it.value }.forEach { (cat, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Glyph(cat.glyph, size = 22.dp, tint = MaterialTheme.colorScheme.primary, fallback = Glyphs.CATEGORY_DEFAULT)
                        Spacer(Modifier.width(6.dp))
                        Text(cat.count(n).str(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(snap: Snapshot, item: LearningItem, onClick: () -> Unit) {
    val subject = item.subjectId?.let { snap.subjectsById[it] }
    val cat = snap.categoryOf(item)
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            GlyphTile(cat.glyph, size = 44.dp, fallback = Glyphs.CATEGORY_DEFAULT)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(item.creator.takeIf { it.isNotBlank() }, subject?.name)
                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val total = item.totalUnits
                val media = snap.mediaProgress(item)
                if (item.status == ItemStatus.ACTIVE && media != null) {
                    Spacer(Modifier.height(8.dp))
                    MediaMeter(media)
                } else if (item.status == ItemStatus.ACTIVE && total != null && total > 0 && !MediaTime.supportsRuntime(item.type)) {
                    Spacer(Modifier.height(8.dp))
                    LinearMeter(item.progressUnits.toFloat() / total, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.lib_units_progress, item.progressUnits, total, cat.unit.str(), item.progressUnits * 100 / total),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val logged = snap.secondsForItem(item.id)
                if (logged > 0) Text(stringResource(R.string.logged, StudyMath.formatDuration(logged)), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                if (item.status == ItemStatus.DONE) {
                    Spacer(Modifier.height(4.dp))
                    StarRating(item.rating, size = 16)
                    if (item.takeaway.isNotBlank()) Text("“${item.takeaway}”", style = MaterialTheme.typography.bodySmall, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ItemEditorSheet(
    snap: Snapshot,
    initial: LearningItem,
    onDismiss: () -> Unit,
    onSave: (LearningItem) -> Unit,
    onDelete: (LearningItem) -> Unit,
    onCreateCategory: (CustomCategory, (Long) -> Unit) -> Unit,
) {
    var item by remember { mutableStateOf(initial) }
    var newCategory by remember { mutableStateOf(false) }
    val isNew = initial.id == 0L
    val cat = snap.categoryOf(item)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(if (isNew) R.string.lib_add_title else R.string.lib_edit_title), style = MaterialTheme.typography.headlineSmall)
            val catOptions = snap.categories + listOfNotNull(cat.takeIf { it == com.rayaurelius.sophinity.domain.Categories.OTHER })
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choices(
                    catOptions, cat, { c -> if (c != com.rayaurelius.sophinity.domain.Categories.OTHER) item = item.copy(type = c.type, categoryId = c.customId) },
                    label = { it.label.str() }, icon = { Glyph(it.glyph, size = 20.dp, fallback = Glyphs.CATEGORY_DEFAULT) },
                    title = stringResource(R.string.choice_type), modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { newCategory = true }) { Icon(Icons.Default.Add, stringResource(R.string.lib_new_category)) }
            }
            OutlinedTextField(item.title, { item = item.copy(title = it) }, label = { Text(stringResource(R.string.lib_field_title)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                item.creator, { item = item.copy(creator = it) },
                label = { Text(stringResource(when (item.type) { ItemType.BOOK, ItemType.ARTICLE -> R.string.lib_author; ItemType.AUDIOBOOK -> R.string.lib_author_narrator; ItemType.PODCAST -> R.string.lib_host; ItemType.SEMINAR -> R.string.lib_speaker; else -> R.string.lib_creator })) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Choices(
                ItemStatus.entries, item.status, { item = item.copy(status = it) }, label = { stringResource(it.labelRes) },
                title = stringResource(R.string.choice_status), modifier = Modifier.fillMaxWidth(),
            )
            SubjectPicker(snap.activeSubjects, item.subjectId, { item = item.copy(subjectId = it) })
            if (MediaTime.supportsRuntime(item.type)) {
                Text(stringResource(R.string.lib_runtime), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.lib_runtime_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DurationFields(stringResource(R.string.field_hours), item.runtimeMin?.times(60L), { sec -> item = item.copy(runtimeMin = sec?.let { (it / 60).toInt() }?.takeIf { it > 0 }) })
                if (item.runtimeMin != null && item.status == ItemStatus.ACTIVE) {
                    Text(stringResource(R.string.lib_prior), style = MaterialTheme.typography.labelLarge)
                    DurationFields(stringResource(R.string.field_hours), item.priorSec.takeIf { it > 0 }, { sec -> item = item.copy(priorSec = sec ?: 0) })
                }
                if (item.runtimeMin != null && MediaTime.supportsSpeed(item.type)) {
                    Choices(
                        MediaTime.SPEEDS, item.playbackSpeed, { item = item.copy(playbackSpeed = it) }, label = { MediaTime.formatSpeed(it) },
                        title = stringResource(R.string.lib_speed), modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!isNew) snap.mediaProgress(item)?.let { MediaMeter(it) }
            }
            if (item.status != ItemStatus.WANT) {
                // Audiobooks, podcasts, courses, videos and seminars track progress by run time,
                // so they only keep an optional size (e.g. 12 chapters), not a "done" count.
                if (MediaTime.supportsRuntime(item.type)) {
                    NumberField(stringResource(R.string.lib_total_units, cat.unit.str()), item.totalUnits, { item = item.copy(totalUnits = it) }, Modifier.fillMaxWidth())
                } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stringResource(R.string.field_done), item.progressUnits, { item = item.copy(progressUnits = it ?: 0) }, Modifier.weight(1f))
                    NumberField(stringResource(R.string.lib_total_units, cat.unit.str()), item.totalUnits, { item = item.copy(totalUnits = it) }, Modifier.weight(1f))
                }
            }
            if (item.status == ItemStatus.DONE) {
                Text(stringResource(R.string.lib_rating), style = MaterialTheme.typography.titleSmall)
                StarRating(item.rating, { item = item.copy(rating = it) })
                OutlinedTextField(
                    item.takeaway, { item = item.copy(takeaway = it) },
                    label = { Text(stringResource(R.string.lib_takeaway)) },
                    supportingText = { Text(stringResource(R.string.lib_takeaway_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(item.notes, { item = item.copy(notes = it) }, label = { Text(stringResource(R.string.lib_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) TextButton(onClick = { onDelete(initial) }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(enabled = item.title.isNotBlank(), onClick = { onSave(item.copy(title = item.title.trim(), creator = item.creator.trim())) }) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }

    if (newCategory) {
        CategoryDialog(
            CustomCategory(name = ""),
            onDismiss = { newCategory = false },
            onSave = { c ->
                onCreateCategory(c) { id -> item = item.copy(type = ItemType.CUSTOM, categoryId = id) }
                newCategory = false
            },
        )
    }
}
