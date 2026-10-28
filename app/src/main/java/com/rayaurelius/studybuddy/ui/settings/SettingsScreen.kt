@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rayaurelius.studybuddy.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.studybuddy.AppContainer
import com.rayaurelius.studybuddy.BuildConfigInfo
import com.rayaurelius.studybuddy.data.AppSettings
import com.rayaurelius.studybuddy.data.BackupData
import com.rayaurelius.studybuddy.data.ThemeMode
import com.rayaurelius.studybuddy.data.db.CustomCategory
import com.rayaurelius.studybuddy.data.db.ItemType
import com.rayaurelius.studybuddy.data.db.Subject
import com.rayaurelius.studybuddy.domain.Categories
import com.rayaurelius.studybuddy.ui.components.CategoryDialog
import com.rayaurelius.studybuddy.reminders.ReminderScheduler
import com.rayaurelius.studybuddy.timer.TimerAlarms
import com.rayaurelius.studybuddy.ui.components.SubjectDot
import com.rayaurelius.studybuddy.ui.components.appViewModel
import com.rayaurelius.studybuddy.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settingsStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val subjects = c.repo.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = c.repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items = c.repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun update(t: (AppSettings) -> AppSettings) = viewModelScope.launch {
        c.settingsStore.update(t)
        ReminderScheduler.apply(c.appContext, c.settingsStore.current())
    }

    fun saveSubject(s: Subject) = viewModelScope.launch {
        if (s.id == 0L) c.repo.addSubject(s) else c.repo.updateSubject(s)
    }

    fun saveCategory(cat: CustomCategory) = viewModelScope.launch { c.repo.saveCategory(cat) }
    fun deleteCategory(cat: CustomCategory) = viewModelScope.launch { c.repo.deleteCategory(cat) }

    fun export(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = BackupData.encode(c.repo.exportData())
            withContext(Dispatchers.IO) {
                c.appContext.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }
        }.onSuccess { messages.emit("Backup saved") }.onFailure { messages.emit("Couldn't save backup: ${it.message}") }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                c.appContext.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            c.repo.importData(BackupData.decode(text))
        }.onSuccess { messages.emit("Backup restored") }.onFailure { messages.emit("Couldn't restore: ${it.message}") }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    var editCategory by remember { mutableStateOf<CustomCategory?>(null) }
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editSubject by remember { mutableStateOf<Subject?>(null) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> confirmImport = uri }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(contentPadding = pad, modifier = Modifier.fillMaxSize()) {
            item { Header("Subjects") }
            subjects.forEach { sub ->
                item(key = "s${sub.id}") {
                    ListItem(
                        headlineContent = { Text("${sub.emoji}  ${sub.name}") },
                        supportingContent = if (sub.archived) ({ Text("Archived") }) else null,
                        leadingContent = { SubjectDot(sub.color, size = 14) },
                        modifier = Modifier.clickable { editSubject = sub },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Add subject") },
                    leadingContent = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.clickable { editSubject = Subject(name = "", color = Brand.subjectPalette[subjects.size % Brand.subjectPalette.size]) },
                )
            }
            item { HorizontalDivider() }

            item { Header("Library categories") }
            item {
                Text(
                    "Built in: " + ItemType.entries.filter { it != ItemType.CUSTOM }.joinToString { "${it.emoji} ${it.label}" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            categories.forEach { cat ->
                item(key = "c${cat.id}") {
                    val n = items.count { it.type == ItemType.CUSTOM && it.categoryId == cat.id }
                    ListItem(
                        headlineContent = { Text("${cat.emoji}  ${Categories.pluralize(cat.name)}") },
                        supportingContent = { Text("$n item${if (n == 1) "" else "s"} · progress in ${cat.unitLabel}") },
                        modifier = Modifier.clickable { editCategory = cat },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Add your own category") },
                    supportingContent = { Text("e.g. Workshops, Documentaries, Lectures") },
                    leadingContent = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.clickable { editCategory = CustomCategory(name = "") },
                )
            }
            item { HorizontalDivider() }

            item { Header("Streak") }
            item {
                Stepper("Minimum per day", "${s.streakMinutes} min counts as a study day", s.streakMinutes, 1, 120, step = 5) { v ->
                    vm.update { it.copy(streakMinutes = v) }
                }
            }
            item {
                Stepper("Rest days per week", "Missing these won't break your streak", s.restDaysPerWeek, 0, 3) { v ->
                    vm.update { it.copy(restDaysPerWeek = v) }
                }
            }
            item {
                ListItem(
                    headlineContent = { Text("Week starts on") },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(DayOfWeek.MONDAY to "Mon", DayOfWeek.SUNDAY to "Sun", DayOfWeek.SATURDAY to "Sat").forEach { (d, l) ->
                                FilterChip(s.weekStart == d, { vm.update { it.copy(weekStart = d) } }, { Text(l) })
                            }
                        }
                    },
                )
            }
            item { HorizontalDivider() }

            item { Header("Timer") }
            item { Stepper("Focus block", "${s.focusMinutes} minutes", s.focusMinutes, 5, 120, step = 5) { v -> vm.update { it.copy(focusMinutes = v) } } }
            item { Stepper("Break", "${s.breakMinutes} minutes", s.breakMinutes, 1, 30) { v -> vm.update { it.copy(breakMinutes = v) } } }
            if (!TimerAlarms.canBeExact(ctx) && Build.VERSION.SDK_INT >= 31) {
                item {
                    ListItem(
                        headlineContent = { Text("Precise timer alerts") },
                        supportingContent = { Text("Allow \"Alarms & reminders\" so focus and break alerts ring right on time") },
                        modifier = Modifier.clickable {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        },
                    )
                }
            }
            item { HorizontalDivider() }

            item { Header("Daily reminder") }
            item {
                ListItem(
                    headlineContent = { Text("Remind me to study") },
                    supportingContent = { Text("Skipped automatically on days you've already studied") },
                    trailingContent = {
                        Switch(s.reminderEnabled, { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            vm.update { it.copy(reminderEnabled = on) }
                        })
                    },
                )
            }
            if (s.reminderEnabled) {
                item {
                    ListItem(
                        headlineContent = { Text("When") },
                        trailingContent = { Text("%d:%02d %s".format(((s.reminderHour + 11) % 12) + 1, s.reminderMinute, if (s.reminderHour < 12) "AM" else "PM")) },
                        modifier = Modifier.clickable { pickTime = true },
                    )
                }
                item {
                    var place by remember(s.reminderPlace) { mutableStateOf(s.reminderPlace) }
                    OutlinedTextField(
                        place, { place = it; vm.update { st -> st.copy(reminderPlace = it) } },
                        label = { Text("Where (optional)") },
                        placeholder = { Text("e.g. at the kitchen table") },
                        supportingText = { Text("Planning when and where makes you far more likely to follow through.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }

            item { Header("Appearance") }
            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ThemeMode.entries.forEach { m ->
                                FilterChip(s.themeMode == m, { vm.update { it.copy(themeMode = m) } }, { Text(m.name.lowercase().replaceFirstChar { c -> c.uppercase() }) })
                            }
                        }
                    },
                )
            }
            if (Build.VERSION.SDK_INT >= 31) {
                item {
                    ListItem(
                        headlineContent = { Text("Match my wallpaper") },
                        supportingContent = { Text("Use Material You colors") },
                        trailingContent = { Switch(s.dynamicColor, { on -> vm.update { it.copy(dynamicColor = on) } }) },
                    )
                }
            }
            item { HorizontalDivider() }

            item { Header("Your data") }
            item {
                ListItem(
                    headlineContent = { Text("Export backup") },
                    supportingContent = { Text("Save everything as a JSON file you own") },
                    modifier = Modifier.clickable { exportLauncher.launch("studybuddy-${LocalDate.now()}.json") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Restore from backup") },
                    supportingContent = { Text("Replaces what's on this device") },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "*/*")) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Privacy") },
                    supportingContent = { Text("Studybuddy has no accounts, ads or trackers. Your data stays on this device and in your own Android backup.") },
                )
            }
            item {
                ListItem(headlineContent = { Text("Version") }, trailingContent = { Text(BuildConfigInfo.versionName(ctx)) })
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    editCategory?.let { cat ->
        CategoryDialog(
            cat,
            onDismiss = { editCategory = null },
            onSave = { vm.saveCategory(it); editCategory = null },
            onDelete = { vm.deleteCategory(it); editCategory = null },
            itemsUsing = items.count { it.type == ItemType.CUSTOM && it.categoryId == cat.id },
        )
    }

    editSubject?.let { sub -> SubjectDialog(sub, onDismiss = { editSubject = null }) { vm.saveSubject(it); editSubject = null } }

    if (pickTime) {
        val state = rememberTimePickerState(s.reminderHour, s.reminderMinute)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = { vm.update { it.copy(reminderHour = state.hour, reminderMinute = state.minute) }; pickTime = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
            text = { TimePicker(state) },
        )
    }

    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text("Restore this backup?") },
            text = { Text("Everything currently in Studybuddy on this device will be replaced by the backup.") },
            confirmButton = { TextButton(onClick = { vm.import(uri); confirmImport = null }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
}

@Composable
private fun Stepper(title: String, subtitle: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = value > min, onClick = { onChange((value - step).coerceAtLeast(min)) }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text("$value", style = MaterialTheme.typography.titleMedium)
                TextButton(enabled = value < max, onClick = { onChange((value + step).coerceAtMost(max)) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        },
    )
}

private val subjectEmojis = listOf("📘", "🧪", "🧮", "💻", "🌍", "🎨", "🎵", "🗣️", "💼", "🧠", "📜", "⚕️", "🏋️", "🍳", "📷", "✍️", "🔧", "💰", "🌱", "🧘")

@Composable
fun SubjectDialog(initial: Subject, onDismiss: () -> Unit, onSave: (Subject) -> Unit) {
    var s by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "New subject" else "Edit subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(s.name, { s = s.copy(name = it) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    subjectEmojis.forEach { e ->
                        Text(
                            e, style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                                .then(if (s.emoji == e) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                                .clickable { s = s.copy(emoji = e) }.padding(6.dp),
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Brand.subjectPalette.forEach { c ->
                        SubjectDot(
                            c, size = 30,
                            modifier = Modifier.clip(CircleShape).clickable { s = s.copy(color = c) }
                                .then(if (s.color == c) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                        )
                    }
                }
                if (initial.id != 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Archived", Modifier.weight(1f))
                        Switch(s.archived, { s = s.copy(archived = it) })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = s.name.isNotBlank(), onClick = { onSave(s.copy(name = s.name.trim())) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Suppress("unused")
private val spacer = Modifier.width(0.dp)
