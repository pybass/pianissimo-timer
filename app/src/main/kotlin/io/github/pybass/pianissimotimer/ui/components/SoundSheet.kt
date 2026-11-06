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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.Sheet
import kotlinx.coroutines.launch

// The saved sounds, `selected` marked. Each has its own speaker, so the user can compare them by ear without leaving
// the list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSheet(
    library: SoundLibrary,
    selected: String,
    onSelect: (String) -> Unit,
    onNewSound: () -> Unit,
    onEdit: (Sound) -> Unit,
    onListen: (Sound) -> Unit,
    onClose: () -> Unit,
) {
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
            Text("Sound", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            for (sound in library.sounds) {
                val chosen = sound.name == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .selectable(selected = chosen, role = Role.RadioButton) {
                            onSelect(sound.name)
                            close()
                        },
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChosenDot(chosen)
                    Text(sound.name, Modifier.weight(1f), color = Ink, fontSize = 17.sp)
                    // No fill and a grey tint: Listen stays the main button of the row.
                    IconButton(onClick = { close(then = { onEdit(sound) }) }) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit ${sound.name}", tint = Muted)
                    }
                    ListenButton { onListen(sound) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Ink.copy(alpha = 0.1f))
            Row(
                Modifier.fillMaxWidth().height(52.dp).clickable { close(then = onNewSound) },
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // As wide as the dot's place, so "New sound" lines up with the names.
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, Modifier.size(16.dp), tint = Action)
                Text("New sound", color = Action, fontSize = 16.sp)
            }
        }
    }
}
