@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Brass
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.TimeStyle

// One setting of a sound: the label, its value in the serif of every number in the app, and a slider drawn in the
// language of the countdown's arc: a hairline track, the part up to the value in Action, a brass dot at the value.
// Material's Slider does the dragging, the stops and the accessibility; only its track and thumb are drawn here.
// `unit` follows the value small and muted. `steps` is the number of stops between the ends of `range`; 0 slides
// freely. The Slider with its own track and thumb is still marked experimental, like the sheets.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingSlider(
    label: String,
    valueText: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    unit: String? = null,
    onDone: () -> Unit,
    onChange: (Double) -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Ink, fontSize = 17.sp)
            Text(
                buildAnnotatedString {
                    append(valueText)
                    if (unit != null) {
                        append(" ")
                        withStyle(SpanStyle(color = Muted, fontSize = 14.sp, fontFamily = FontFamily.Default)) { append(unit) }
                    }
                },
                color = Ink,
                fontSize = 24.sp,
                style = TimeStyle,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = range,
            steps = steps,
            onValueChangeFinished = onDone,
            thumb = { Box(Modifier.size(14.dp).background(Brass, CircleShape)) },
            track = { state ->
                val fraction = (state.value - range.start) / (range.endInclusive - range.start)
                Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                    val y = size.height / 2
                    drawLine(Ink.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
                    if (fraction > 0f) {
                        drawLine(Action, Offset(0f, y), Offset(size.width * fraction, y), 3.dp.toPx(), StrokeCap.Round)
                    }
                }
            },
        )
    }
}
