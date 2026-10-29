package com.rayaurelius.sophinity.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.SophinityApp
import com.rayaurelius.sophinity.data.db.LearningItem
import com.rayaurelius.sophinity.domain.Links
import com.rayaurelius.sophinity.timer.LinkTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Opens a library item's web link. Unless turned off in Settings, it first starts the stopwatch on
 * that item (or resumes it, or saves whatever else was being timed and switches), so time spent in
 * an online seminar or audiobook player is counted without having to remember the timer.
 */
@Composable
fun rememberItemLinkOpener(): (LearningItem) -> Unit {
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    val container = (context.applicationContext as? SophinityApp)?.container
    return { item ->
        val url = Links.normalize(item.url)
        scope.launch {
            // Items not saved yet have no id to time against; they just open the link.
            if (container != null && item.id != 0L && container.settingsStore.current().linkStartsTimer) {
                val message = when (container.timer.startForLink(item.id, item.subjectId)) {
                    LinkTimer.Action.STARTED -> R.string.link_timer_started
                    LinkTimer.Action.RESUMED -> R.string.link_timer_resumed
                    LinkTimer.Action.SWITCHED -> R.string.link_timer_switched
                    LinkTimer.Action.ALREADY_TIMING -> null
                }
                message?.let { res ->
                    withContext(Dispatchers.Main.immediate) {
                        Toast.makeText(context, resources.getString(res, item.title), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            withContext(Dispatchers.Main.immediate) { runCatching { uri.openUri(url) } }
        }
    }
}

/** A library item's link, labelled with the site name (e.g. "zoom.us"). Nothing if there's no usable link. */
@Composable
fun LinkChip(item: LearningItem, modifier: Modifier = Modifier) {
    if (!Links.isOpenable(item.url)) return
    val open = rememberItemLinkOpener()
    val label = stringResource(R.string.lib_open_link, Links.host(item.url))
    AssistChip(
        onClick = { open(item) },
        label = { Text(Links.host(item.url), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = modifier.semantics { contentDescription = label },
    )
}
