@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.StepsWidth

// One bar with the length of each step next to the others. The step at `current` is in full colour; the others are
// faint.
@Composable
fun StepBar(steps: List<Step>, current: Int? = null) {
    Row(Modifier.width(StepsWidth), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        steps.forEachIndexed { i, step ->
            val colour = if (i == current) Action else Action.copy(alpha = 0.28f)
            Box(Modifier.weight(step.seconds.toFloat()).height(4.dp).background(colour, CircleShape))
        }
    }
}
