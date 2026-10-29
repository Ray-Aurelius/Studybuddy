@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.rayaurelius.sophinity.ui.onboarding

import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import com.rayaurelius.sophinity.ui.glyphs.LaurelMedal
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.data.db.Goal
import com.rayaurelius.sophinity.data.db.GoalKind
import com.rayaurelius.sophinity.data.db.GoalPeriod
import com.rayaurelius.sophinity.data.db.Subject
import com.rayaurelius.sophinity.reminders.ReminderScheduler
import com.rayaurelius.sophinity.ui.components.ProgressRing
import com.rayaurelius.sophinity.ui.components.appViewModel
import com.rayaurelius.sophinity.ui.theme.Brand
import kotlinx.coroutines.launch

private val presetSubjects = listOf(
    "alpha_omega" to R.string.subj_languages, "gear" to R.string.subj_programming, "coin" to R.string.subj_business, "scales" to R.string.subj_finance,
    "armillary" to R.string.subj_science, "broken_column" to R.string.subj_history, "butterfly" to R.string.subj_psychology, "amphora" to R.string.subj_art,
    "lyre" to R.string.subj_music, "asclepius" to R.string.subj_health, "quill" to R.string.subj_writing, "owl" to R.string.subj_philosophy,
)

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    fun finish(subjects: List<Pair<String, String>>, weeklyHours: Int, reminder: Boolean, hour: Int, minute: Int, place: String, done: () -> Unit) =
        viewModelScope.launch {
            subjects.forEachIndexed { i, (glyph, name) ->
                c.repo.addSubject(Subject(name = name, icon = glyph, color = Brand.subjectPalette[i % Brand.subjectPalette.size]))
            }
            if (weeklyHours > 0) {
                c.repo.saveGoal(Goal(title = "", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = weeklyHours * 60))
            }
            c.settingsStore.update {
                it.copy(onboarded = true, reminderEnabled = reminder, reminderHour = hour, reminderMinute = minute, reminderPlace = place.trim())
            }
            ReminderScheduler.apply(c.appContext, c.settingsStore.current())
            done()
        }

    /** Restoring marks onboarding done, so the app opens straight onto the restored history. */
    fun restore(uri: android.net.Uri, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                c.appContext.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            c.repo.importData(com.rayaurelius.sophinity.data.BackupData.decode(text))
        }.onFailure { e ->
            val ctx = com.rayaurelius.sophinity.ui.i18n.AppLocale.localized(c.appContext)
            val why = when ((e as? com.rayaurelius.sophinity.data.BackupException)?.reason) {
                com.rayaurelius.sophinity.data.BackupException.Reason.NOT_A_BACKUP -> ctx.getString(R.string.set_not_backup)
                com.rayaurelius.sophinity.data.BackupException.Reason.TOO_NEW -> ctx.getString(R.string.set_backup_too_new)
                null -> e.message ?: ""
            }
            onError(ctx.getString(R.string.set_restore_failed, why))
        }
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm = appViewModel { OnboardingViewModel(it) }
    var step by remember { mutableIntStateOf(0) }
    val chosen = remember { mutableStateListOf<Pair<String, String>>() }
    var custom by remember { mutableStateOf("") }
    var hours by remember { mutableFloatStateOf(3f) }
    var reminder by remember { mutableStateOf(true) }
    var place by remember { mutableStateOf("") }
    val time = rememberTimePickerState(19, 0)
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.restore(it) { msg -> android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show() } }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                ProgressRing(if (i <= step) 1f else 0f, Modifier.size(10.dp), stroke = 5.dp) {}
            }
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(step, modifier = Modifier.weight(1f), label = "step") { s ->
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (s) {
                    0 -> {
                        Spacer(Modifier.height(40.dp))
                        LaurelMedal("owl", earned = true, size = 128.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.onb_welcome), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.onb_intro),
                            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    1 -> {
                        Text(stringResource(R.string.onb_what), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.onb_pick), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val presets = presetSubjects.map { it.first to stringResource(it.second) }
                            (presets + chosen.filter { it !in presets }).forEach { p ->
                                FilterChip(p in chosen, { if (p in chosen) chosen.remove(p) else chosen.add(p) }, { Text(p.second) }, leadingIcon = { Glyph(p.first, size = 18.dp) })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(custom, { custom = it }, label = { Text(stringResource(R.string.onb_something_else)) }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(enabled = custom.isNotBlank(), onClick = { chosen.add(Glyphs.SUBJECT_DEFAULT to custom.trim()); custom = "" }) { Text(stringResource(R.string.common_add)) }
                        }
                    }
                    2 -> {
                        Text(stringResource(R.string.onb_weekly), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.onb_weekly_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(40.dp))
                        Text(pluralStringResource(R.plurals.onb_hours_week, hours.toInt(), hours.toInt()), style = MaterialTheme.typography.displaySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(stringResource(R.string.onb_minutes_day, StudyMath.formatDuration((hours * 3600 / 7).toLong())), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(16.dp))
                        Slider(hours, { hours = it }, valueRange = 1f..20f, steps = 18)
                    }
                    else -> {
                        Text(stringResource(R.string.onb_when), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.onb_when_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.onb_daily_reminder), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Switch(reminder, { reminder = it })
                        }
                        if (reminder) {
                            Spacer(Modifier.height(12.dp))
                            TimeInput(time)
                            OutlinedTextField(place, { place = it }, label = { Text(stringResource(R.string.onb_where)) }, placeholder = { Text(stringResource(R.string.onb_where_hint)) },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.common_back)) }
            else TextButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) }) { Text(stringResource(R.string.onb_restore)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (step < 3) step++ else {
                        if (reminder && Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
                        vm.finish(chosen.toList(), hours.toInt(), reminder, time.hour, time.minute, place, onDone)
                    }
                },
                modifier = Modifier.height(52.dp),
            ) { Text(stringResource(when (step) { 0 -> R.string.onb_begin; 3 -> R.string.onb_start; else -> R.string.common_next })) }
        }
    }
}
