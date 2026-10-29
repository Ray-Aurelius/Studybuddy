package com.rayaurelius.sophinity.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rayaurelius.sophinity.R
import com.rayaurelius.sophinity.data.Snapshot
import com.rayaurelius.sophinity.data.db.ItemStatus
import com.rayaurelius.sophinity.ui.glyphs.Glyph
import com.rayaurelius.sophinity.ui.glyphs.Glyphs

/** Every library item (in progress first, then want-to, then finished), or none. */
@Composable
fun LibraryItemPicker(snap: Snapshot, selectedId: Long?, onSelect: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val order = listOf(ItemStatus.ACTIVE, ItemStatus.WANT, ItemStatus.DONE)
    val items = snap.items.sortedWith(compareBy({ order.indexOf(it.status) }, { it.title.lowercase() }))
    val sel = selectedId?.let { snap.itemsById[it] }
    Box {
        OutlinedCard(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(sel?.let { snap.categoryOf(it).glyph } ?: "scroll", size = 22.dp, fallback = Glyphs.CATEGORY_DEFAULT,
                    tint = if (sel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(sel?.title ?: stringResource(R.string.sessions_not_linked), Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (sel != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(open, { open = false }, Modifier.heightIn(max = 360.dp)) {
            DropdownMenuItem(text = { Text(stringResource(R.string.sessions_not_linked)) }, onClick = { onSelect(null); open = false })
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(item.status.labelRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Glyph(snap.categoryOf(item).glyph, size = 20.dp, fallback = Glyphs.CATEGORY_DEFAULT) },
                    onClick = { onSelect(item.id); open = false },
                )
            }
        }
    }
}
