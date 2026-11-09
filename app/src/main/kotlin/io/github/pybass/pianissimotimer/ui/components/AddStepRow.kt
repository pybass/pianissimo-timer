@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Muted

// The row that adds a step, as wide as what it follows. In the step list of the timer screen (`inList`) it is one
// more row, at the left edge in Action like a link. Alone under the sound row of a one-step timer it follows the
// centered composition above it and steps back to Muted: a rare action there, not a second Start.
@Composable
fun AddStepRow(onClick: () -> Unit, inList: Boolean = true) {
    val color = if (inList) Action else Muted
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp, if (inList) Alignment.Start else Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = color)
        Text("Add step", color = color, fontSize = 16.sp)
    }
}
