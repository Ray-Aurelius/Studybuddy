@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.rayaurelius.studybuddy.ui.onboarding

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
import com.rayaurelius.studybuddy.AppContainer
import com.rayaurelius.studybuddy.data.db.Goal
import com.rayaurelius.studybuddy.data.db.GoalKind
import com.rayaurelius.studybuddy.data.db.GoalPeriod
import com.rayaurelius.studybuddy.data.db.Subject
import com.rayaurelius.studybuddy.reminders.ReminderScheduler
import com.rayaurelius.studybuddy.ui.components.ProgressRing
import com.rayaurelius.studybuddy.ui.components.appViewModel
import com.rayaurelius.studybuddy.ui.theme.Brand
import kotlinx.coroutines.launch

private val presetSubjects = listOf(
    "🗣️" to "Languages", "💻" to "Programming", "💼" to "Business", "💰" to "Personal finance",
    "🧪" to "Science", "📜" to "History", "🧠" to "Psychology", "🎨" to "Art & design",
    "🎵" to "Music", "⚕️" to "Health", "✍️" to "Writing", "🌍" to "Philosophy",
)

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    fun finish(subjects: List<Pair<String, String>>, weeklyHours: Int, reminder: Boolean, hour: Int, minute: Int, place: String, done: () -> Unit) =
        viewModelScope.launch {
            subjects.forEachIndexed { i, (emoji, name) ->
                c.repo.addSubject(Subject(name = name, emoji = emoji, color = Brand.subjectPalette[i % Brand.subjectPalette.size]))
            }
            if (weeklyHours > 0) {
                c.repo.saveGoal(Goal(title = "$weeklyHours hours a week", kind = GoalKind.TIME, period = GoalPeriod.WEEKLY, target = weeklyHours * 60))
            }
            c.settingsStore.update {
                it.copy(onboarded = true, reminderEnabled = reminder, reminderHour = hour, reminderMinute = minute, reminderPlace = place.trim())
            }
            ReminderScheduler.apply(c.appContext, c.settingsStore.current())
            done()
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
                        Text("📚✨", style = MaterialTheme.typography.displayLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(24.dp))
                        Text("Learning never stops at graduation.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Studybuddy tracks the books you read, the podcasts you learn from, the seminars you attend, and every minute you invest in yourself — then makes your progress visible.",
                            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    1 -> {
                        Text("What are you learning?", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Pick a few. You can change these anytime.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (presetSubjects + chosen.filter { it !in presetSubjects }).forEach { p ->
                                FilterChip(p in chosen, { if (p in chosen) chosen.remove(p) else chosen.add(p) }, { Text("${p.first} ${p.second}") })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(custom, { custom = it }, label = { Text("Something else") }, singleLine = true, modifier = Modifier.weight(1f))
                            TextButton(enabled = custom.isNotBlank(), onClick = { chosen.add("📘" to custom.trim()); custom = "" }) { Text("Add") }
                        }
                    }
                    2 -> {
                        Text("Set a weekly target", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Weekly goals flex around busy days. Start a bit above what feels easy — specific, challenging goals beat \"I'll do my best.\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(40.dp))
                        Text("${hours.toInt()} hours / week", style = MaterialTheme.typography.displaySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("≈ ${(hours * 60 / 7).toInt()} minutes a day", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(16.dp))
                        Slider(hours, { hours = it }, valueRange = 1f..20f, steps = 18)
                    }
                    else -> {
                        Text("When and where will you study?", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "People who plan exactly when and where they'll act follow through far more often. We'll nudge you then — but only on days you haven't studied yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Daily reminder", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Switch(reminder, { reminder = it })
                        }
                        if (reminder) {
                            Spacer(Modifier.height(12.dp))
                            TimeInput(time)
                            OutlinedTextField(place, { place = it }, label = { Text("Where") }, placeholder = { Text("at the kitchen table") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (step < 3) step++ else {
                        if (reminder && Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
                        vm.finish(chosen.toList(), hours.toInt(), reminder, time.hour, time.minute, place, onDone)
                    }
                },
                modifier = Modifier.height(52.dp),
            ) { Text(when (step) { 0 -> "Let's begin"; 3 -> "Start learning"; else -> "Next" }) }
        }
    }
}
