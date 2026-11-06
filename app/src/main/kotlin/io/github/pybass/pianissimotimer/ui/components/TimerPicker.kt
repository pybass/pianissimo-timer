@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.timer.TimerLibrary
import io.github.pybass.pianissimotimer.ui.theme.ContentWidth
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted

// The name of the chosen timer; a tap opens the list of saved timers, where any timer can be renamed or deleted.
// `onSelect` gets a timer's name; `onRename` gets the timer and its new name, trimmed and not taken.
@Composable
fun TimerPicker(
    library: TimerLibrary,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onRename: (Timer, String) -> Unit,
    onDelete: (Timer) -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    // The timer in the rename dialog; null when the dialog is closed.
    var editing by remember { mutableStateOf<Timer?>(null) }
    TextButton(
        onClick = { choosing = true },
        modifier = Modifier.widthIn(max = ContentWidth),
        colors = ButtonDefaults.textButtonColors(contentColor = Ink),
    ) {
        Text(
            library.selected,
            Modifier.weight(1f, fill = false),
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painterResource(R.drawable.ic_expand),
            contentDescription = null,
            Modifier.padding(start = 6.dp).size(16.dp),
            tint = Muted,
        )
    }
    if (choosing) TimerSheet(library, onSelect, onNew, onEdit = { editing = it }, onClose = { choosing = false })
    val timer = editing
    if (timer != null) {
        RenameDialog(
            current = timer.name,
            // The timer's own name is free for it, so a rename may change only the case.
            isTaken = { !it.equals(timer.name, ignoreCase = true) && library.isTaken(it) },
            onDone = {
                onRename(timer, it)
                editing = null
            },
            onDismiss = { editing = null },
            onDelete = {
                onDelete(timer)
                editing = null
            },
            canDelete = library.timers.size > 1,
        )
    }
}
