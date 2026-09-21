package jp.trackrail.shopclipper.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import jp.trackrail.shopclipper.core.ProjectOption
import jp.trackrail.shopclipper.todoist.TodoistSection

// Pieces both the settings screen and the share sheet use.

/** The Inbox is "no project_id in the request", so its id is empty (計画書 D10). */
val InboxOption = ProjectOption(id = "", name = "インボックス", depth = 0, label = "（インボックス・既定）")
val NoSectionOption = TodoistSection(id = "", name = "（セクションなし）")

/** Todoist's scale: 4 = P1 … 1 = P4 (計画書 R3). */
val Priorities = listOf(4 to "P1", 3 to "P2", 2 to "P3", 1 to "P4")

@Composable
fun <T> Chooser(
    label: String,
    options: List<T>,
    selected: T?,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.let(text) ?: "—", modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
