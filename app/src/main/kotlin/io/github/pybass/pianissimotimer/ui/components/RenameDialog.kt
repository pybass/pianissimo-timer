@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.ui.theme.Action
import io.github.pybass.pianissimotimer.ui.theme.Danger
import io.github.pybass.pianissimotimer.ui.theme.Ink
import io.github.pybass.pianissimotimer.ui.theme.Sheet

// `isTaken` gets a trimmed name; `onDone` gets the name trimmed. `onDelete` adds a Delete button; `canDelete` is false
// for the last item left.
@Composable
fun RenameDialog(
    current: String,
    isTaken: (String) -> Boolean,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    canDelete: Boolean = true,
) {
    // TextFieldValue holds the selection too: the whole name starts selected, so typing replaces it.
    var field by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val name = field.text.trim()
    val taken = isTaken(name)
    val valid = name.isNotEmpty() && !taken
    val focus = remember { FocusRequester() }
    // Puts the cursor in the field, so the keyboard opens with the dialog.
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        // All buttons in one row: the dialog's own slots sit together at the end, and Delete must stand apart on the
        // left, as far from OK as it can be. OK is the one filled button, as Save is on the sound screen.
        confirmButton = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        // Moved left by the button's own padding, so the word starts under the field.
                        modifier = Modifier.offset(x = (-12).dp),
                        enabled = canDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                    ) { Text("Delete", fontSize = 16.sp) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Action)) {
                    Text("Cancel", fontSize = 16.sp)
                }
                Spacer(Modifier.width(4.dp))
                PillButton("OK", Modifier.height(44.dp), enabled = valid, onClick = { onDone(name) })
            }
        },
        // Set like the title of a bottom sheet.
        title = { Text("Rename", fontSize = 20.sp, fontWeight = FontWeight.Medium) },
        text = {
            // The selection and its handles take the app's colour in place of the system's.
            val selection = TextSelectionColors(handleColor = Action, backgroundColor = Action.copy(alpha = 0.2f))
            CompositionLocalProvider(LocalTextSelectionColors provides selection) {
                BasicTextField(
                    value = field,
                    onValueChange = { field = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    textStyle = TextStyle(color = Ink, fontSize = 20.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    // Done on the keyboard works as OK.
                    keyboardActions = KeyboardActions(onDone = { if (valid) onDone(name) }),
                    singleLine = true,
                    cursorBrush = SolidColor(Action),
                    // A line under the name, not a box around it.
                    decorationBox = { innerTextField ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            innerTextField()
                            HorizontalDivider(thickness = 1.5.dp, color = if (taken) Danger else Action)
                            // Always there, even empty: the dialog keeps its height when the message appears.
                            Text(if (taken) "This name is taken" else "", color = Danger, fontSize = 13.sp)
                        }
                    },
                )
            }
        },
        containerColor = Sheet,
        titleContentColor = Ink,
    )
}
