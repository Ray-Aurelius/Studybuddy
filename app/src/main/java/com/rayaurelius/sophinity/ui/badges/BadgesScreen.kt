package com.rayaurelius.sophinity.ui.badges

import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import com.rayaurelius.sophinity.domain.str
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.GlyphTile
import com.rayaurelius.sophinity.ui.glyphs.LaurelMedal
import androidx.compose.ui.res.stringResource
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
import com.rayaurelius.sophinity.ui.theme.LocalHeadingFont
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
        item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.laurels_title), style = MaterialTheme.typography.headlineMedium) }
        item(span = { GridItemSpan(maxLineSpan) }) { TitleCard(snap) }
        item(span = { GridItemSpan(maxLineSpan) }) { TitleLadder(snap) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.laurels_count, unlocked.size, Milestones.all.size), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MilestoneCategory.entries.forEach { cat ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(cat.labelRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.your_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(t.current.name.str(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            stringResource(R.string.title_greek_meaning, t.current.greek, t.current.meaning.str()),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalHeadingFont.current),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    LaurelMedal(t.current.glyph, earned = true, size = 84.dp)
                }
                val next = t.next
                if (next != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.to_become, next.name.str()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    t.nextChecklist.forEach { (m, done) ->
                        val p = m.progress(snap.milestoneContext)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Box(Modifier.width(32.dp)) {
                                Glyph(if (done) "check" else m.glyph, size = 22.dp, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (done) 1f else 0.8f))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(m.title.str(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(m.description.str(), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                            if (!done) Text("$p / ${m.target}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.highest_reached),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.level_n, l.level), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                LinearMeter(l.progress, Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                val left = ((l.hoursForLevel - l.hoursIntoLevel) * 3600).toLong()
                Text(
                    stringResource(R.string.level_progress, StudyMath.formatDuration(left, true), l.level + 1, StudyMath.formatDuration(snap.totalSeconds, true)),
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
        Text(stringResource(R.string.path_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Titles.all.forEach { t ->
            val earned = t.rank <= rank
            val isCurrent = t.rank == rank
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).alpha(if (earned) 1f else 0.5f),
            ) {
                GlyphTile(
                    t.glyph, size = 40.dp,
                    container = if (earned) LocalGold.current.container else MaterialTheme.colorScheme.surfaceContainerHigh,
                    tint = if (earned) LocalGold.current.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isCurrent) stringResource(R.string.you_are_here, t.name.str()) else t.name.str(),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = LocalHeadingFont.current),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(stringResource(R.string.greek_dot_meaning, t.greek, t.meaning.str()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().height(196.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            LaurelMedal(m.glyph, earned = done, size = 68.dp)
            Spacer(Modifier.height(6.dp))
            Text(m.title.str(), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 2)
            Text(m.description.str(), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            if (done) {
                Text(
                    Instant.ofEpochMilli(unlockedAt!!).atZone(snap.zone).format(rememberDateFormat("yMMMd")),
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
