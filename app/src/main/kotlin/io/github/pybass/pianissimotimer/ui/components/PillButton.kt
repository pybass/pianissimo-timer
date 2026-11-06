@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Paper

// `modifier` sets the width. A height in it wins over the 56 dp here, because it comes first in the chain.
// `outlined` is for the second button next to a main one. Disabled, the button is an
// outline too: it says the action waits, without being the heaviest thing on the screen.
@Composable
fun PillButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (outlined) Color.Transparent else Action,
            contentColor = if (outlined) Action else Paper,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Action.copy(alpha = 0.55f),
        ),
        border = if (enabled && !outlined) null else BorderStroke(1.5.dp, Action.copy(alpha = 0.4f)),
    ) {
        Text(title, fontSize = 18.sp)
    }
}
