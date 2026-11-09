@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.sound.FadeInRange
import io.github.pybass.pianissimotimer.sound.LengthRange
import io.github.pybass.pianissimotimer.sound.NoteRange
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundType
import io.github.pybass.pianissimotimer.ui.components.ListenButton
import io.github.pybass.pianissimotimer.ui.components.PillButton
import io.github.pybass.pianissimotimer.ui.components.RenameDialog
import io.github.pybass.pianissimotimer.ui.components.SettingSlider
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Brass
import io.github.pybass.pianissimotimer.ui.theme.Danger
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.StepsWidth
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
    // Hung from the top like the step screen, Save at the bottom like Start and Done; the free height sits between.
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Column(Modifier.width(StepsWidth), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            TypeRow(sound.type) {
                sound = sound.copy(type = it)
                onListen(sound)
            }
            // The slider stops on whole notes only: a stop for every note but the two ends.
            val notes = NoteRange.count() - 2
            SettingSlider(
                "Pitch",
                noteName(sound.note),
                sound.note.toDouble(),
                NoteRange.asFloat,
                steps = notes,
                onDone = listen,
            ) {
                sound = sound.copy(note = it.roundToInt())
            }
            // A stop every half second: the ear tells no finer difference in a slow rise.
            val halves = ((FadeInRange.endInclusive - FadeInRange.start) * 2).toInt() - 1
            val rise = sound.fadeInSeconds.toText(1)
            SettingSlider("Rise", rise, sound.fadeInSeconds, FadeInRange.asFloat, steps = halves, unit = "s", onDone = listen) {
                sound = sound.copy(fadeInSeconds = (it * 2).roundToInt() / 2.0)
            }
            val length = sound.lengthSeconds.toText(1)
            SettingSlider("Length", length, sound.lengthSeconds, LengthRange.asFloat, unit = "s", onDone = listen) {
                sound = sound.copy(lengthSeconds = it)
            }
        }
        // An existing sound saves only changed settings; its name is already saved by the rename. A new sound saves
        // even unchanged.
        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            PillButton(
                "Save",
                Modifier.width(StepsWidth),
                enabled = isNew || sound.copy(name = initial.name) != initial,
                onClick = { onSave(sound) },
            )
        }
        Row(Modifier.width(StepsWidth)) {
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
        Spacer(Modifier.height(20.dp))
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

// The types of sound in one row, the chosen one in Ink with the brass dot under it, as the sheets mark the chosen
// row. Each name is padded to a finger's height; the dot keeps its place under every name so the row does not jump
// when the choice moves.
@Composable
private fun TypeRow(chosen: SoundType, onChoose: (SoundType) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SoundType.entries.forEach { type ->
            Column(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { onChoose(type) }.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(type.label, color = if (type == chosen) Ink else Muted, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(6.dp).background(if (type == chosen) Brass else Color.Transparent, CircleShape))
            }
        }
    }
}

// MIDI numbering: 60 is C4, middle C.
private fun noteName(note: Int) = NoteNames[note % 12] + (note / 12 - 1)

// The sliders take Float ranges; the settings keep theirs in their own types.
private val IntRange.asFloat get() = first.toFloat()..last.toFloat()
private val ClosedFloatingPointRange<Double>.asFloat get() = start.toFloat()..endInclusive.toFloat()

// Rounds for display. Not String.format: it would use the phone's locale, and "4,5" reads oddly next to "s".
private fun Double.toText(decimals: Int): String {
    val scale = 10.0.pow(decimals)
    return ((this * scale).roundToInt() / scale).toString()
}
