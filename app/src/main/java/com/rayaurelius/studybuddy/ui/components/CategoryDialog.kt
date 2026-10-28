@file:OptIn(ExperimentalLayoutApi::class)

package com.rayaurelius.studybuddy.ui.components

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
import com.rayaurelius.studybuddy.data.db.CustomCategory
import com.rayaurelius.studybuddy.domain.Categories

private val categoryEmojis = listOf(
    "📌", "🛠️", "🎭", "🎞️", "🧑‍🏫", "🗺️", "🔬", "🧩", "📝", "💬",
    "🏛️", "🌐", "🧪", "🎨", "🎹", "🏃", "🧘", "🍳", "🌿", "🐾",
)
private val unitSuggestions = listOf("sessions", "lessons", "chapters", "episodes", "hours", "parts")

/** Create or edit a custom library category: a name, an emoji, and what progress is counted in. */
@Composable
fun CategoryDialog(
    initial: CustomCategory,
    onDismiss: () -> Unit,
    onSave: (CustomCategory) -> Unit,
    onDelete: ((CustomCategory) -> Unit)? = null,
    itemsUsing: Int = 0,
) {
    var c by remember { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isNew = initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    c.name, { c = c.copy(name = it.take(30)) },
                    label = { Text("Name") }, placeholder = { Text("e.g. Workshop, Documentary") },
                    supportingText = { if (c.name.isNotBlank()) Text("Shown as \"${Categories.pluralize(c.name)}\" in lists") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categoryEmojis.forEach { e ->
                        Text(
                            e, style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                                .then(if (c.emoji == e) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                                .clickable { c = c.copy(emoji = e) }.padding(6.dp),
                        )
                    }
                }
                Text("Track progress in", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    unitSuggestions.forEach { u -> FilterChip(c.unitLabel == u, { c = c.copy(unitLabel = u) }, { Text(u) }) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = c.name.isNotBlank(), onClick = { onSave(c.copy(name = c.name.trim())) }) { Text("Save") }
        },
        dismissButton = {
            if (!isNew && onDelete != null) {
                TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${initial.name}\"?") },
            text = {
                Text(
                    if (itemsUsing == 0) "This category isn't used by anything in your library."
                    else "$itemsUsing item${if (itemsUsing == 1) "" else "s"} will move to \"Other\". Nothing is lost."
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(initial) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
