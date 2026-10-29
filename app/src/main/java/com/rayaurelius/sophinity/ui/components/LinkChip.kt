package com.rayaurelius.sophinity.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.domain.Links

/** Opens an item's web link in the browser; shows the site name (e.g. "zoom.us"). Nothing if there's no usable link. */
@Composable
fun LinkChip(url: String, modifier: Modifier = Modifier) {
    if (!Links.isOpenable(url)) return
    val uri = LocalUriHandler.current
    val label = stringResource(R.string.lib_open_link, Links.host(url))
    AssistChip(
        onClick = { runCatching { uri.openUri(url) } },
        label = { Text(Links.host(url), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = modifier.semantics { contentDescription = label },
    )
}
