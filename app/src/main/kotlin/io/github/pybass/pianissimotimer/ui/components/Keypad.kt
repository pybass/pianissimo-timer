@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.ui.theme.ContentWidth
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle

// Three keys and two gaps fill the row exactly.
private val KeySize = (ContentWidth - 16.dp * 2) / 3

private const val BACKSPACE = "⌫"

// Digit keys as on a phone; `onDigits` gets "0".."9" or "00".
@Composable
fun Keypad(onDigits: (String) -> Unit, onDelete: () -> Unit) {
    // Rows sit closer than keys in a row: the screen is short on height, not on width.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", BACKSPACE).chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (key in row) {
                    RoundKey(key) { if (key == BACKSPACE) onDelete() else onDigits(key) }
                }
            }
        }
    }
}

@Composable
private fun RoundKey(label: String, onClick: () -> Unit) {
    Box(Modifier.size(KeySize).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (label == BACKSPACE) {
            // Muted: an action among digits, not a digit.
            Icon(painterResource(R.drawable.ic_backspace), contentDescription = "Backspace", Modifier.size(30.dp), tint = Muted)
        } else {
            // The face of the time above, so a key looks like the digit it types.
            Text(label, color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Light, style = TimeStyle)
        }
    }
}
