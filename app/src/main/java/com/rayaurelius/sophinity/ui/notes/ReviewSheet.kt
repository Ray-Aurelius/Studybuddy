package com.rayaurelius.sophinity.ui.notes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.Note
import com.rayaurelius.sophinity.domain.Review
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.Glyphs
import com.rayaurelius.sophinity.ui.i18n.rememberDateFormat
import java.time.LocalDate

/** "Nov 2", or "Nov 2, 2027" when it's not this year. */
@Composable
fun reviewDay(snap: Snapshot, epochDay: Long): String {
    val day = LocalDate.ofEpochDay(epochDay)
    return when (day) {
        snap.today -> stringResource(R.string.stats_today)
        snap.today.plusDays(1) -> stringResource(R.string.review_tomorrow)
        else -> day.format(rememberDateFormat(if (day.year == snap.today.year) "MMMd" else "yMMMd"))
    }
}

/**
 * One short review session: each due note shows only its cue (the title, or the item it's about),
 * the learner tries to recall the rest, then reveals it and says whether they remembered. The
 * queue is fixed when the sheet opens, so answering doesn't reshuffle what's left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSheet(snap: Snapshot, queue: List<Note>, nextAfter: Long?, onAnswer: (Note, Boolean) -> Unit, onDismiss: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var remembered by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (index >= queue.size) {
                // Done.
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    com.rayaurelius.sophinity.ui.glyphs.LaurelMedal("owl", earned = true, size = 96.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.review_done_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.review_done_body, remembered, queue.size),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                    nextAfter?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.review_next_on, reviewDay(snap, it)), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.common_ok)) }
                }
                return@Column
            }
            val note = queue[index]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.review_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.review_progress, index + 1, queue.size), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { index.toFloat() / queue.size }, modifier = Modifier.fillMaxWidth())
            AnimatedContent(note, label = "card") { shown ->
                val item = shown.itemId?.let { snap.itemsById[it] }
                val (cue, answer) = Review.cueAndAnswer(shown)
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp).heightIn(min = 140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (item != null) Row(verticalAlignment = Alignment.CenterVertically) {
                            Glyph(snap.categoryOf(item).glyph, size = 16.dp, fallback = Glyphs.CATEGORY_DEFAULT, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            cue ?: stringResource(R.string.review_cue_untitled, item?.title ?: reviewDay(snap, java.time.Instant.ofEpochMilli(shown.createdAt).atZone(snap.zone).toLocalDate().toEpochDay())),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        AnimatedVisibility(revealed) {
                            Text(answer, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("review_answer"))
                        }
                        if (!revealed) Text(stringResource(R.string.review_try_recall), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!revealed) {
                FilledTonalButton(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.review_show)) }
            } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onAnswer(note, false); revealed = false; index++ }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(R.string.review_not_yet), maxLines = 1)
                }
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onAnswer(note, true); remembered++; revealed = false; index++
                }, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.review_remembered), maxLines = 1) }
            }
            Text(stringResource(R.string.review_why), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
