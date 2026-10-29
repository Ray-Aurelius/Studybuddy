@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rayaurelius.sophinity.ui.settings

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.outlined.Language
import androidx.core.os.LocaleListCompat
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.data.BackupException
import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import com.rayaurelius.sophinity.ui.i18n.AppLocale
import com.rayaurelius.sophinity.R
import androidx.compose.ui.graphics.Color
import com.rayaurelius.sophinity.ui.glyphs.GlyphPicker
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ViewSidebar
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
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.BuildConfigInfo
import com.rayaurelius.sophinity.data.AppSettings
import com.rayaurelius.sophinity.data.BackupData
import com.rayaurelius.sophinity.data.ThemeMode
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.data.db.ItemType
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.ui.components.CategoryDialog
import com.rayaurelius.sophinity.reminders.ReminderScheduler
import com.rayaurelius.sophinity.timer.TimerAlarms
import com.rayaurelius.sophinity.ui.components.SubjectDot
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.theme.Brand
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
    private fun str(id: Int, vararg args: Any) = AppLocale.localized(c.appContext).getString(id, *args)

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
        }.onSuccess { messages.emit(str(R.string.set_backup_saved)) }.onFailure { messages.emit(str(R.string.set_backup_save_failed, it.message ?: "")) }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                c.appContext.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            c.repo.importData(BackupData.decode(text))
        }.onSuccess { messages.emit(str(R.string.set_backup_restored)) }.onFailure { e ->
            val why = when ((e as? BackupException)?.reason) {
                BackupException.Reason.NOT_A_BACKUP -> str(R.string.set_not_backup)
                BackupException.Reason.TOO_NEW -> str(R.string.set_backup_too_new)
                null -> e.message ?: ""
            }
            messages.emit(str(R.string.set_restore_failed, why))
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {},
    onOpenToolbar: () -> Unit = {},
) {
    val vm = appViewModel { SettingsViewModel(it) }
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    var editCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var pickLanguage by remember { mutableStateOf(false) }
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
                title = { Text(stringResource(R.string.set_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        LazyColumn(contentPadding = pad, modifier = Modifier.fillMaxSize()) {
            item { Header(stringResource(R.string.set_make_yours)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_appearance)) },
                    supportingContent = { Text(stringResource(R.string.set_appearance_sub)) },
                    leadingContent = { Icon(Icons.Outlined.Palette, null) },
                    modifier = Modifier.clickable(onClick = onOpenAppearance),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_accessibility)) },
                    supportingContent = { Text(stringResource(R.string.set_accessibility_sub)) },
                    leadingContent = { Icon(Icons.Outlined.AccessibilityNew, null) },
                    modifier = Modifier.clickable(onClick = onOpenAccessibility),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_toolbar)) },
                    supportingContent = { Text(stringResource(R.string.set_toolbar_sub)) },
                    leadingContent = { Icon(Icons.Outlined.ViewSidebar, null) },
                    modifier = Modifier.clickable(onClick = onOpenToolbar),
                )
            }
            item {
                val chosen = AppLocale.chosenTag()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_language)) },
                    supportingContent = {
                        Text(chosen?.let { AppLocale.nativeName(it) }
                            ?: stringResource(R.string.set_language_follow, AppLocale.nativeName(LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag() ?: "en")))
                    },
                    leadingContent = { Icon(Icons.Outlined.Language, null) },
                    modifier = Modifier.clickable { pickLanguage = true },
                )
            }
            item { HorizontalDivider() }

            item { Header(stringResource(R.string.set_subjects)) }
            subjects.forEach { sub ->
                item(key = "s${sub.id}") {
                    ListItem(
                        headlineContent = { Text(sub.name) },
                        supportingContent = if (sub.archived) ({ Text(stringResource(R.string.set_archived)) }) else null,
                        leadingContent = { Glyph(sub.icon, size = 24.dp, tint = Color(sub.color)) },
                        modifier = Modifier.clickable { editSubject = sub },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_add_subject)) },
                    leadingContent = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.clickable { editSubject = Subject(name = "", color = Brand.subjectPalette[subjects.size % Brand.subjectPalette.size]) },
                )
            }
            item { HorizontalDivider() }

            item { Header(stringResource(R.string.set_categories)) }
            item {
                Text(
                    stringResource(R.string.set_built_in, ItemType.entries.filter { it != ItemType.CUSTOM }.map { stringResource(it.labelRes) }.joinToString()),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            categories.forEach { cat ->
                item(key = "c${cat.id}") {
                    val n = items.count { it.type == ItemType.CUSTOM && it.categoryId == cat.id }
                    ListItem(
                        headlineContent = { Text(cat.name) },
                        leadingContent = { Glyph(cat.icon, size = 24.dp, tint = MaterialTheme.colorScheme.primary, fallback = Glyphs.CATEGORY_DEFAULT) },
                        supportingContent = { Text(pluralStringResource(R.plurals.set_cat_items, n, n, cat.unitLabel)) },
                        modifier = Modifier.clickable { editCategory = cat },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_add_category)) },
                    supportingContent = { Text(stringResource(R.string.set_add_category_sub)) },
                    leadingContent = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.clickable { editCategory = CustomCategory(name = "") },
                )
            }
            item { HorizontalDivider() }

            item { Header(stringResource(R.string.set_streak)) }
            item {
                Stepper(stringResource(R.string.set_min_day), stringResource(R.string.set_min_day_sub, StudyMath.formatDuration(s.streakMinutes * 60L)), s.streakMinutes, 1, 120, step = 5) { v ->
                    vm.update { it.copy(streakMinutes = v) }
                }
            }
            item {
                Stepper(stringResource(R.string.set_rest_days), stringResource(R.string.set_rest_days_sub), s.restDaysPerWeek, 0, 3) { v ->
                    vm.update { it.copy(restDaysPerWeek = v) }
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_week_starts)) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val loc = LocalConfiguration.current.locales[0]
                            listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).map { it to it.getDisplayName(java.time.format.TextStyle.SHORT, loc) }.forEach { (d, l) ->
                                FilterChip(s.weekStart == d, { vm.update { it.copy(weekStart = d) } }, { Text(l) })
                            }
                        }
                    },
                )
            }
            item { HorizontalDivider() }

            item { Header(stringResource(R.string.set_timer)) }
            item { Stepper(stringResource(R.string.set_focus_block), StudyMath.formatDuration(s.focusMinutes * 60L), s.focusMinutes, 5, 120, step = 5) { v -> vm.update { it.copy(focusMinutes = v) } } }
            item { Stepper(stringResource(R.string.set_break), StudyMath.formatDuration(s.breakMinutes * 60L), s.breakMinutes, 1, 30) { v -> vm.update { it.copy(breakMinutes = v) } } }
            if (!TimerAlarms.canBeExact(ctx) && Build.VERSION.SDK_INT >= 31) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.set_precise)) },
                        supportingContent = { Text(stringResource(R.string.set_precise_sub)) },
                        modifier = Modifier.clickable {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        },
                    )
                }
            }
            item { HorizontalDivider() }

            item { Header(stringResource(R.string.set_reminder)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_reminder_toggle)) },
                    supportingContent = { Text(stringResource(R.string.set_reminder_sub)) },
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
                        headlineContent = { Text(stringResource(R.string.set_when)) },
                        trailingContent = { Text(java.time.LocalTime.of(s.reminderHour, s.reminderMinute).format(rememberDateFormat("jmm"))) },
                        modifier = Modifier.clickable { pickTime = true },
                    )
                }
                item {
                    var place by remember(s.reminderPlace) { mutableStateOf(s.reminderPlace) }
                    OutlinedTextField(
                        place, { place = it; vm.update { st -> st.copy(reminderPlace = it) } },
                        label = { Text(stringResource(R.string.set_where)) },
                        placeholder = { Text(stringResource(R.string.set_where_hint)) },
                        supportingText = { Text(stringResource(R.string.set_where_sub)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }

            item { Header(stringResource(R.string.set_data)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_export)) },
                    supportingContent = { Text(stringResource(R.string.set_export_sub)) },
                    modifier = Modifier.clickable { exportLauncher.launch("sophinity-${LocalDate.now()}.json") },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_restore)) },
                    supportingContent = { Text(stringResource(R.string.set_restore_sub)) },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "*/*")) },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.set_privacy)) },
                    supportingContent = { Text(stringResource(R.string.set_privacy_sub)) },
                )
            }
            item {
                ListItem(headlineContent = { Text(stringResource(R.string.set_version)) }, trailingContent = { Text(BuildConfigInfo.versionName(ctx)) })
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (pickLanguage) {
        LanguageDialog(onDismiss = { pickLanguage = false }) { tag -> pickLanguage = false; AppLocale.set(tag) }
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
                TextButton(onClick = { vm.update { it.copy(reminderHour = state.hour, reminderMinute = state.minute) }; pickTime = false }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.common_cancel)) } },
            text = { TimePicker(state) },
        )
    }

    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text(stringResource(R.string.set_restore_title)) },
            text = { Text(stringResource(R.string.set_restore_body)) },
            confirmButton = { TextButton(onClick = { vm.import(uri); confirmImport = null }) { Text(stringResource(R.string.set_restore_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
internal fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
}

@Composable
internal fun Stepper(title: String, subtitle: String, value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
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

@Composable
fun SubjectDialog(initial: Subject, onDismiss: () -> Unit, onSave: (Subject) -> Unit) {
    var s by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.subject_new else R.string.subject_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(s.name, { s = s.copy(name = it) }, label = { Text(stringResource(R.string.cat_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                GlyphPicker(Glyphs.subjectChoices, s.icon, { s = s.copy(icon = it) }, tint = Color(s.color))
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
                        Text(stringResource(R.string.set_archived), Modifier.weight(1f))
                        Switch(s.archived, { s = s.copy(archived = it) })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = s.name.isNotBlank(), onClick = { onSave(s.copy(name = s.name.trim())) }) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Suppress("unused")
private val spacer = Modifier.width(0.dp)

@Composable
private fun LanguageDialog(onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    val chosen = AppLocale.chosenTag()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_language)) },
        text = {
            LazyColumn {
                item {
                    val phone = AppLocale.nativeName(LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag() ?: "en")
                    LanguageRow(stringResource(R.string.set_language_follow, phone), null, chosen == null) { onPick(null) }
                }
                AppLocale.supported.forEach { tag ->
                    item(key = tag) {
                        val english = java.util.Locale.forLanguageTag(tag).getDisplayName(java.util.Locale.ENGLISH)
                        LanguageRow(AppLocale.nativeName(tag), english, chosen == tag) { onPick(tag) }
                    }
                }
                item {
                    Text(stringResource(R.string.set_language_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun LanguageRow(name: String, english: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected, null)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            if (english != null && english != name) Text(english, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
