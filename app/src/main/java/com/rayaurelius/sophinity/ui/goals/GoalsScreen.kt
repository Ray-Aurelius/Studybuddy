@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rayaurelius.sophinity.ui.goals

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.domain.GoalMath
import com.rayaurelius.sophinity.domain.GoalProgress
import com.rayaurelius.sophinity.ui.components.EmptyState
import com.rayaurelius.sophinity.ui.components.NumberField
import com.rayaurelius.sophinity.ui.components.ProgressRing
import com.rayaurelius.sophinity.ui.components.SubjectPicker
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.theme.Brand
import com.rayaurelius.sophinity.ui.today.goalAmount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GoalsViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun save(g: Goal, fallbackTitle: String) = viewModelScope.launch { c.repo.saveGoal(g.copy(title = g.title.ifBlank { fallbackTitle })) }
    fun delete(g: Goal) = viewModelScope.launch { c.repo.deleteGoal(g) }
}

private data class Preset(val label: String, val goal: () -> Goal)

private fun presets(today: LocalDate) = listOf(
    Preset("30 min a day") { Goal(title = "", kind = GoalKind.TIME, period = GoalPeriod.DAILY, target = 30) },
    Preset("5 hours a week") { Goal(title = "", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = 300) },
    Preset("2 books a month") { Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 2, itemType = ItemType.BOOK) },
    Preset("2 audiobooks a month") { Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 2, itemType = ItemType.AUDIOBOOK) },
    Preset("4 podcasts a month") { Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 4, itemType = ItemType.PODCAST) },
    Preset("1 seminar a month") { Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.MONTHLY, target = 1, itemType = ItemType.SEMINAR) },
    Preset("12 books this year") {
        Goal(title = "", kind = GoalKind.ITEMS, period = GoalPeriod.BY_DATE, target = 12, itemType = ItemType.BOOK,
            deadlineEpochDay = today.withDayOfYear(today.lengthOfYear()).toEpochDay())
    },
)

@Composable
fun GoalsScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { GoalsViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    var editing by remember { mutableStateOf<Goal?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Text("Goals", style = MaterialTheme.typography.headlineMedium) }
            item {
                Column {
                    Text("Quick add", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets(snap.today).forEach { p -> AssistChip(onClick = { editing = p.goal() }, label = { Text(p.label) }) }
                    }
                }
            }
            if (snap.goalProgress.isEmpty()) {
                item {
                    EmptyState("🎯", "No goals yet",
                        "Pick something specific and a little ambitious. Weekly time goals are a great start: they flex around busy days.")
                }
            } else {
                items(snap.goalProgress, key = { it.goal.id }) { p -> GoalCard(snap, p) { editing = p.goal } }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = Goal(title = "", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = 180) },
            icon = { Icon(Icons.Default.Add, null) }, text = { Text("New goal") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { g ->
        GoalEditor(snap, g, onDismiss = { editing = null }, onSave = { g -> vm.save(g, GoalMath.describe(g, snap.categoryOf(g)).replaceFirstChar { it.uppercase() }); editing = null }, onDelete = { vm.delete(it); editing = null })
    }
}

@Composable
private fun GoalCard(snap: Snapshot, p: GoalProgress, onClick: () -> Unit) {
    val g = p.goal
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(p.fraction, Modifier.size(64.dp), stroke = 8.dp, color = if (p.met) Brand.Laurel else MaterialTheme.colorScheme.primary) {
                Text(if (p.met) "✓" else "${(p.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(g.title, style = MaterialTheme.typography.titleMedium)
                val subject = g.subjectId?.let { snap.subjectsById[it] }
                Text(
                    listOfNotNull(goalAmount(p), subject?.let { "${it.emoji} ${it.name}" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                val footer = when {
                    p.met && g.period == GoalPeriod.BY_DATE -> "Achieved! 🎉"
                    p.met -> when (g.period) {
                        GoalPeriod.DAILY -> "Done for today 🎉"
                        GoalPeriod.WEEKLY -> "Done for this week 🎉"
                        else -> "Done for this month 🎉"
                    }
                    p.daysLeft == 1 && g.period != GoalPeriod.DAILY -> "Last day"
                    g.period == GoalPeriod.DAILY -> "Resets tonight"
                    else -> "${p.daysLeft} days left"
                }
                Text(footer, style = MaterialTheme.typography.labelMedium,
                    color = if (p.met) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                if (p.recentPeriods > 0) {
                    Text("Hit ${p.recentHits} of the last ${p.recentPeriods}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GoalEditor(snap: Snapshot, initial: Goal, onDismiss: () -> Unit, onSave: (Goal) -> Unit, onDelete: (Goal) -> Unit) {
    var g by remember { mutableStateOf(initial) }
    var pickDate by remember { mutableStateOf(false) }
    val isNew = initial.id == 0L
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) "New goal" else "Edit goal", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(g.kind == GoalKind.TIME, { g = g.copy(kind = GoalKind.TIME, target = if (g.kind != GoalKind.TIME) 180 else g.target) }, { Text("⏱️ Learning time") })
                FilterChip(g.kind == GoalKind.ITEMS, { g = g.copy(kind = GoalKind.ITEMS, target = if (g.kind != GoalKind.ITEMS) 1 else g.target) }, { Text("✅ Finish things") })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalPeriod.entries.forEach { p ->
                    FilterChip(g.period == p, {
                        g = g.copy(period = p, deadlineEpochDay = if (p == GoalPeriod.BY_DATE) g.deadlineEpochDay ?: snap.today.plusMonths(3).toEpochDay() else null)
                    }, { Text(p.label) })
                }
            }
            if (g.kind == GoalKind.TIME) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Hours", g.target / 60, { g = g.copy(target = (it ?: 0) * 60 + g.target % 60) }, Modifier.weight(1f))
                    NumberField("Minutes", g.target % 60, { g = g.copy(target = (g.target / 60) * 60 + (it ?: 0).coerceAtMost(59)) }, Modifier.weight(1f))
                }
                SubjectPicker(snap.activeSubjects, g.subjectId, { g = g.copy(subjectId = it) }, noneLabel = "Any subject")
            } else {
                NumberField("How many", g.target, { g = g.copy(target = it ?: 0) }, Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(g.itemType == null, { g = g.copy(itemType = null, categoryId = null) }, { Text("Anything") })
                    snap.categories.forEach { c ->
                        FilterChip(c.matchesGoal(g), { g = g.copy(itemType = c.type, categoryId = c.customId) }, { Text("${c.emoji} ${c.plural}") })
                    }
                }
            }
            if (g.period == GoalPeriod.BY_DATE) {
                val d = LocalDate.ofEpochDay(g.deadlineEpochDay ?: snap.today.toEpochDay())
                OutlinedCard(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("📅  Due ${d.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}", Modifier.padding(14.dp))
                }
            }
            OutlinedTextField(
                g.title, { g = g.copy(title = it) }, label = { Text("Name (optional)") },
                placeholder = { Text(GoalMath.describe(g, snap.categoryOf(g)).replaceFirstChar { it.uppercase() }) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) TextButton(onClick = { onDelete(initial) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(enabled = g.target > 0, onClick = { onSave(g) }) { Text("Save") }
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(g.deadlineEpochDay ?: snap.today.toEpochDay()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { g = g.copy(deadlineEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}
