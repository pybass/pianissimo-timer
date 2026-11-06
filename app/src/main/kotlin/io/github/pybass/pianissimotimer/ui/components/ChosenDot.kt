@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pybass.pianissimotimer.ui.theme.Brass

// The mark of the chosen row in a list, the same dot that marks the current step on the countdown. It keeps its 16 dp
// place when `chosen` is false, so the names of all rows line up.
@Composable
fun ChosenDot(chosen: Boolean) {
    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
        if (chosen) Box(Modifier.size(8.dp).background(Brass, CircleShape))
    }
}
