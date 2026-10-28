package com.rayaurelius.studybuddy.ui.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rayaurelius.studybuddy.AppContainer
import com.rayaurelius.studybuddy.data.Snapshot
import com.rayaurelius.studybuddy.domain.Levels
import com.rayaurelius.studybuddy.domain.Milestone
import com.rayaurelius.studybuddy.domain.MilestoneCategory
import com.rayaurelius.studybuddy.domain.Milestones
import com.rayaurelius.studybuddy.domain.StudyMath
import com.rayaurelius.studybuddy.ui.components.LinearMeter
import com.rayaurelius.studybuddy.ui.components.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.format.DateTimeFormatter

class BadgesViewModel(c: AppContainer) : ViewModel() {
    val snapshot = c.repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun BadgesScreen(contentPadding: PaddingValues) {
    val vm = appViewModel { BadgesViewModel(it) }
    val snap = vm.snapshot.collectAsStateWithLifecycle().value ?: return
    val unlocked = snap.unlocked.associateBy { it.key }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 8.dp, 16.dp, contentPadding.calculateBottomPadding() + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Text("Badges", style = MaterialTheme.typography.headlineMedium) }
        item(span = { GridItemSpan(maxLineSpan) }) { LevelCard(snap) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("${unlocked.size} of ${Milestones.all.size} unlocked", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MilestoneCategory.entries.forEach { cat ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(cat.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(Milestones.all.filter { it.category == cat }, key = { it.key }) { m ->
                BadgeTile(snap, m, unlocked[m.key]?.unlockedAt)
            }
        }
    }
}

@Composable
private fun LevelCard(snap: Snapshot) {
    val l = snap.level
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Level ${l.level}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(l.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(12.dp))
            LinearMeter(l.progress, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            val left = ((l.hoursForLevel - l.hoursIntoLevel) * 3600).toLong()
            Text(
                "${StudyMath.formatDuration(left, true)} to Level ${l.level + 1} · ${Levels.titleFor(l.level + 1)}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "${StudyMath.formatDuration(snap.totalSeconds, true)} invested in yourself so far",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun BadgeTile(snap: Snapshot, m: Milestone, unlockedAt: Long?) {
    val done = unlockedAt != null
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().height(168.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(m.emoji, style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(if (done) 1f else 0.35f))
            Spacer(Modifier.height(6.dp))
            Text(m.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
            Text(m.description, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            if (done) {
                Text(
                    Instant.ofEpochMilli(unlockedAt!!).atZone(snap.zone).format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                val p = m.progress(snap.milestoneContext)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearMeter(p.toFloat() / m.target, Modifier.weight(1f))
                }
                Text("$p / ${m.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
