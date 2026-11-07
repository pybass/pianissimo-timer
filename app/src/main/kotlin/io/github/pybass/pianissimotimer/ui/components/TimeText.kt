@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle

// The big time of every screen, and each number of the time wheel.
@Composable
fun TimeText(text: String, muted: Boolean = false) {
    Text(
        text,
        color = if (muted) Muted else Ink,
        fontSize = 88.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.02).em,
        // Never wrap: a number of the wheel that does not fit its column must overflow, not break in two.
        softWrap = false,
        style = TimeStyle,
    )
}
