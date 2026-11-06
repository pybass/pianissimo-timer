@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.formatDigits
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle

@Composable
fun TimeText(text: String, muted: Boolean = false) = TimeText(AnnotatedString(text), if (muted) Muted else Ink)

@Composable
fun TimeText(text: AnnotatedString, color: Color = Ink) {
    Text(
        text,
        color = color,
        fontSize = 88.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.02).em,
        style = TimeStyle,
    )
}

// The time while it is typed: a frame of mm:ss whose places not yet typed are muted, so the user sees where the next
// digit goes. Hours appear only once typed; from then on every place is typed, so nothing is muted.
@Composable
fun TypedTime(digits: String) {
    val text = if (digits.length <= 4) digits.padStart(4, '0').let { "${it.take(2)}:${it.drop(2)}" } else formatDigits(digits)
    val untyped = (text.count { it != ':' } - digits.length).coerceAtLeast(0)
    TimeText(
        buildAnnotatedString {
            var place = 0
            for (ch in text) {
                if (place < untyped) withStyle(SpanStyle(color = Muted)) { append(ch) } else append(ch)
                if (ch != ':') place++
            }
        },
    )
}
