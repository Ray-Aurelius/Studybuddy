@file:OptIn(ExperimentalLayoutApi::class)

package com.rayaurelius.sophinity.ui.components

import com.rayaurelius.sophinity.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rayaurelius.sophinity.data.db.CustomCategory
import com.rayaurelius.sophinity.ui.glyphs.GlyphPicker
import com.rayaurelius.sophinity.ui.glyphs.Glyphs

private val unitSuggestions = listOf(R.string.unit_sessions, R.string.unit_lessons, R.string.unit_chapters, R.string.unit_episodes, R.string.unit_hours, R.string.unit_parts)

/** Create or edit a custom library category: a name, an icon, and what progress is counted in. */
@Composable
fun CategoryDialog(
    initial: CustomCategory,
    onDismiss: () -> Unit,
    onSave: (CustomCategory) -> Unit,
    onDelete: ((CustomCategory) -> Unit)? = null,
    itemsUsing: Int = 0,
) {
    val defaultUnit = stringResource(R.string.unit_parts)
    var c by remember { mutableStateOf(if (initial.id == 0L && initial.unitLabel == "parts") initial.copy(unitLabel = defaultUnit) else initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isNew = initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.cat_new else R.string.cat_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    c.name, { c = c.copy(name = it.take(30)) },
                    label = { Text(stringResource(R.string.cat_name)) }, placeholder = { Text(stringResource(R.string.cat_name_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                GlyphPicker(Glyphs.categoryChoices, c.icon, { c = c.copy(icon = it) }, fallback = Glyphs.CATEGORY_DEFAULT)
                Text(stringResource(R.string.cat_track_in), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    unitSuggestions.forEach { id -> val u = stringResource(id); FilterChip(c.unitLabel == u, { c = c.copy(unitLabel = u) }, { Text(u) }) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = c.name.isNotBlank(), onClick = { onSave(c.copy(name = c.name.trim())) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            if (!isNew && onDelete != null) {
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        },
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.cat_delete_title, initial.name)) },
            text = {
                Text(
                    if (itemsUsing == 0) stringResource(R.string.cat_delete_unused)
                    else pluralStringResource(R.plurals.cat_delete_used, itemsUsing, itemsUsing)
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(initial) }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
