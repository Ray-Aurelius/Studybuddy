package com.rayaurelius.sophinity.ui.badges

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
import com.rayaurelius.sophinity.AppContainer
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.domain.Titles
import com.rayaurelius.sophinity.ui.components.GreekKeyBand
import com.rayaurelius.sophinity.ui.theme.Classical
import com.rayaurelius.sophinity.ui.theme.LocalGold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.rayaurelius.sophinity.domain.Milestone
import com.rayaurelius.sophinity.domain.MilestoneCategory
import com.rayaurelius.sophinity.domain.Milestones
import com.rayaurelius.sophinity.domain.StudyMath
import com.rayaurelius.sophinity.ui.components.LinearMeter
import com.rayaurelius.sophinity.ui.components.appViewModel
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
        item(span = { GridItemSpan(maxLineSpan) }) { Text("Laurels", style = MaterialTheme.typography.headlineMedium) }
        item(span = { GridItemSpan(maxLineSpan) }) { TitleCard(snap) }
        item(span = { GridItemSpan(maxLineSpan) }) { TitleLadder(snap) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("${unlocked.size} of ${Milestones.all.size} laurels earned", style = MaterialTheme.typography.labelLarge,
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
private fun TitleCard(snap: Snapshot) {
    val t = snap.title
    val l = snap.level
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            GreekKeyBand(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            Column(Modifier.padding(20.dp)) {
                Text("Your title", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(t.current.name, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${t.current.greek} · “${t.current.meaning}”",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Classical),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                val next = t.next
                if (next != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("To become ${next.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    t.nextChecklist.forEach { (m, done) ->
                        val p = m.progress(snap.milestoneContext)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(if (done) "✅" else m.emoji, modifier = Modifier.width(30.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(m.description, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                            if (!done) Text("$p / ${m.target}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text("You've reached the highest title. The love of wisdom has no finish line.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(18.dp))
                Text("Level ${l.level}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                LinearMeter(l.progress, Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                val left = ((l.hoursForLevel - l.hoursIntoLevel) * 3600).toLong()
                Text(
                    "${StudyMath.formatDuration(left, true)} to Level ${l.level + 1} · ${StudyMath.formatDuration(snap.totalSeconds, true)} invested in yourself",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/** All eight titles, earned ones lit up, so the path to Philosopher is always visible. */
@Composable
private fun TitleLadder(snap: Snapshot) {
    val rank = snap.title.current.rank
    Column {
        Text("The path to Philosopher", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Titles.all.forEach { t ->
            val earned = t.rank <= rank
            val isCurrent = t.rank == rank
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).alpha(if (earned) 1f else 0.5f),
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (earned) LocalGold.current.container else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (earned) "✓" else "${t.rank}", style = MaterialTheme.typography.labelLarge,
                            color = LocalGold.current.onContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.name + if (isCurrent) "  · you are here" else "",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = Classical),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text("${t.greek} · ${t.meaning}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(snap: Snapshot, m: Milestone, unlockedAt: Long?) {
    val done = unlockedAt != null
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (done) LocalGold.current.container else MaterialTheme.colorScheme.surfaceContainerLow,
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
