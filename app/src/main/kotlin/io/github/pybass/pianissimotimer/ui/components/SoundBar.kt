@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.soundFor
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted

// One row for a step's sound: listen, choose it, set the volume. The step row of the timer screen, opened: as wide,
// no fill, the hairlines around it belong to the screen. `selected` is the step's sound id; `onSelect` gets a
// sound's id.
@Composable
fun SoundBar(
    sounds: List<Sound>,
    selected: String,
    onSelect: (String) -> Unit,
    onNewSound: () -> Unit,
    onEdit: (Sound) -> Unit,
    volumeDb: Int,
    onVolumeChange: (Int) -> Unit,
    onListen: (Sound) -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    val sound = sounds.soundFor(selected)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListenButton { onListen(sound) }
        TextButton(
            onClick = { choosing = true },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(contentColor = Ink),
        ) {
            Text(
                sound.name,
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
        VolumeWheel(volumeDb, onVolumeChange)
    }
    if (choosing) SoundSheet(sounds, sound.id, onSelect, onNewSound, onEdit, onListen, onClose = { choosing = false })
}
