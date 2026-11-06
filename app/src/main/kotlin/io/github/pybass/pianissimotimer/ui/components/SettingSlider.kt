@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Muted
import io.github.pybass.pianissimotimer.ui.theme.Paper

// One setting of a sound. `steps` is the number of stops between the ends of `range`; 0 slides freely.
@Composable
fun SettingSlider(
    label: String,
    valueText: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onDone: () -> Unit,
    onChange: (Double) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Ink, fontSize = 16.sp)
            Text(valueText, color = Muted, fontSize = 16.sp, style = TextStyle(fontFeatureSettings = "tnum"))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = range,
            steps = steps,
            onValueChangeFinished = onDone,
            colors = SliderDefaults.colors(
                thumbColor = Action,
                activeTrackColor = Action,
                inactiveTrackColor = Action.copy(alpha = 0.14f),
                // The stops of a stepped slider; unset, they take Material's purple.
                activeTickColor = Paper,
                inactiveTickColor = Action,
            ),
        )
    }
}
