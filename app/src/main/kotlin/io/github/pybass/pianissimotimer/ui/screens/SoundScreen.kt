@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundType
import io.github.pybass.pianissimotimer.ui.components.ListenButton
import io.github.pybass.pianissimotimer.ui.components.PillButton
import io.github.pybass.pianissimotimer.ui.components.RenameDialog
import io.github.pybass.pianissimotimer.ui.components.SettingSlider
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.ContentWidth
import io.github.pybass.pianissimotimer.ui.theme.Danger
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.Paper
import kotlin.math.pow
import kotlin.math.roundToInt

// A plain "#", not "♯": the phone draws the sharp sign from another font with a taller line, so the slider's row
// would jump in height between notes.
private val NoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

// The name, the type and one slider per setting of the sound. A change plays the sound, so the user tunes it by
// ear. `isTaken` gets a trimmed name. An existing sound is renamed at once through `onRename`, and Save keeps only
// the settings; a new sound gets its name with Save. A new sound has no Delete; `canDelete` is false for the last
// sound left.
@Composable
fun SoundScreen(
    initial: Sound,
    isNew: Boolean,
    isTaken: (String) -> Boolean,
    onListen: (Sound) -> Unit,
    onRename: (String) -> Unit,
    onSave: (Sound) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    onCancel: () -> Unit,
) {
    var sound by remember { mutableStateOf(initial) }
    var renaming by remember { mutableStateOf(false) }
    val listen = { onListen(sound) }
    // The system back gesture cancels, as the Cancel button does.
    BackHandler(onBack = onCancel)
    Column(Modifier.width(ContentWidth), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The name and the pencil are one button. Renaming is rare, so it has no field of its own.
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { renaming = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    sound.name,
                    Modifier.weight(1f, fill = false),
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(painterResource(R.drawable.ic_edit), contentDescription = "Rename", Modifier.size(18.dp), tint = Muted)
            }
            ListenButton(listen)
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val types = SoundType.entries
            types.forEachIndexed { i, type ->
                SegmentedButton(
                    selected = type == sound.type,
                    onClick = {
                        sound = sound.copy(type = type)
                        onListen(sound)
                    },
                    shape = SegmentedButtonDefaults.itemShape(i, types.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Action,
                        activeContentColor = Paper,
                        activeBorderColor = Action,
                        inactiveContentColor = Ink,
                        inactiveBorderColor = Action.copy(alpha = 0.4f),
                    ),
                    // The check mark would crowd four short labels.
                    icon = {},
                    // Four labels share ContentWidth: the default 12 dp padding and a large system font wrap "String".
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                ) {
                    Text(type.label, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
        // C2 to C6; the slider stops on whole notes only.
        SettingSlider("Pitch", noteName(sound.note), sound.note.toDouble(), 36f..84f, steps = 47, onDone = listen) {
            sound = sound.copy(note = it.roundToInt())
        }
        // A stop every half second: the ear tells no finer difference in a slow rise.
        SettingSlider("Rise", "${sound.fadeInSeconds.toText(1)} s", sound.fadeInSeconds, 0f..10f, steps = 19, onDone = listen) {
            sound = sound.copy(fadeInSeconds = (it * 2).roundToInt() / 2.0)
        }
        SettingSlider("Length", "${sound.lengthSeconds.toText(1)} s", sound.lengthSeconds, 1f..12f, onDone = listen) {
            sound = sound.copy(lengthSeconds = it)
        }
        // An existing sound saves only changed settings; its name is already saved by the rename. A new sound saves
        // even unchanged.
        PillButton(
            "Save",
            Modifier.width(ContentWidth),
            enabled = isNew || sound.copy(name = initial.name) != initial,
            onClick = { onSave(sound) },
        )
        Row(Modifier.fillMaxWidth()) {
            if (!isNew) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = canDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                ) {
                    Text("Delete", fontSize = 16.sp)
                }
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(contentColor = Action),
            ) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    }
    if (renaming) {
        RenameDialog(
            current = sound.name,
            isTaken = isTaken,
            onDone = {
                sound = sound.copy(name = it)
                if (!isNew) onRename(it)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}

// MIDI numbering: 60 is C4, middle C.
private fun noteName(note: Int) = NoteNames[note % 12] + (note / 12 - 1)

// Rounds for display. Not String.format: it would use the phone's locale, and "4,5" reads oddly next to "s".
private fun Double.toText(decimals: Int): String {
    val scale = 10.0.pow(decimals)
    return ((this * scale).roundToInt() / scale).toString()
}
