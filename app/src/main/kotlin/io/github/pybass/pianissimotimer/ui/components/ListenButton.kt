@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.ui.theme.Muted

// A speaker, not a ▶: next to Start, a ▶ reads as "start the timer". No fill and a muted tint, so the only filled
// button on a screen is its main one.
@Composable
fun ListenButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, colors = IconButtonDefaults.iconButtonColors(contentColor = Muted)) {
        Icon(painterResource(R.drawable.ic_speaker), contentDescription = "Listen")
    }
}
