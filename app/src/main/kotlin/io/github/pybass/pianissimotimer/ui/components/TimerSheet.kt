@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.data.AppData
import io.github.pybass.pianissimotimer.formatSeconds
import io.github.pybass.pianissimotimer.timer.Timer
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.Sheet
import io.github.pybass.pianissimotimer.ui.theme.Soft
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle
import kotlinx.coroutines.launch

// The saved timers with their total times, the chosen one marked. A timer of several steps lists their lengths too.
// `onSelect` gets a timer's id. The pencil of a row closes the sheet, then `onEdit` gets its timer.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSheet(data: AppData, onSelect: (String) -> Unit, onNew: () -> Unit, onEdit: (Timer) -> Unit, onClose: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Slide the sheet down first, then remove it; `then` runs after.
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onClose()
            then()
        }
    }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = Sheet) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text("Timer", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            for (timer in data.timers) {
                val chosen = timer.id == data.selectedTimerId
                // A one-step timer has nothing to list: its row stays one line, so the taller rows mark the chains.
                val steps = timer.steps.size > 1
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(if (steps) 64.dp else 56.dp)
                        .selectable(selected = chosen, role = Role.RadioButton) {
                            onSelect(timer.id)
                            close()
                        },
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChosenDot(chosen)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(timer.name, color = Ink, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (steps) {
                            Text(
                                timer.steps.joinToString(", ") { formatSeconds(it.seconds) },
                                color = Muted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(fontFeatureSettings = "tnum"),
                            )
                        }
                    }
                    Text(
                        formatSeconds(timer.seconds),
                        color = Soft,
                        fontSize = 19.sp,
                        style = TimeStyle,
                    )
                    IconButton(onClick = { close(then = { onEdit(timer) }) }) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit ${timer.name}", tint = Muted)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Ink.copy(alpha = 0.1f))
            Row(
                Modifier.fillMaxWidth().height(52.dp).clickable { close(then = onNew) },
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // As wide as the dot's place, so "New timer" lines up with the names.
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(16.dp), tint = Action)
                Text("New timer", color = Action, fontSize = 16.sp)
            }
        }
    }
}
