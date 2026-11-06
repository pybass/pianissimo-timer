package io.github.pybass.pianissimotimer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.pybass.pianissimotimer.R

val Paper = Color(0xFFEEF0F3)
val Ink = Color(0xFF1E2733)
val Muted = Color(0xFF9AA3AF)

// Between Ink and Muted: text that must be read but stays behind the main one.
val Soft = Ink.copy(alpha = 0.62f)
val Action = Color(0xFF2E4A62)

// The metal of a bell: the dot of the fermata, wherever it marks "this one".
val Brass = Color(0xFFB08A4E)

// A bottom sheet: a shade lighter than Paper, so it lifts off the screen without the stock white.
val Sheet = Color(0xFFF7F8FA)
val Danger = Color(0xFFB3261E)

// Newsreader: every time on the screen and, in italic, the app name. SIL OFL, see third_party/. All its digits have
// one width, so a running time does not shift sideways; a face without such digits (`tnum`) cannot replace it.
// Light is the cut for 72 pt, for the big time and the keypad; Regular and the italic are the cut for 22 pt, for the
// small times and the name, where the hairlines of the big cut would fade.
val Newsreader = FontFamily(
    cut(R.font.newsreader, FontWeight.Light, FontStyle.Normal, opticalSize = 72f),
    cut(R.font.newsreader, FontWeight.Normal, FontStyle.Normal, opticalSize = 22f),
    cut(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic, opticalSize = 22f),
)

// One font file holds every weight and optical size; this picks one point in it. "opsz" is the optical size: the cut
// drawn for text of about that many points.
private fun cut(file: Int, weight: FontWeight, style: FontStyle, opticalSize: Float) = Font(
    file,
    weight,
    style,
    variationSettings = FontVariation.Settings(weight, style, FontVariation.Setting("opsz", opticalSize)),
)

// The style of every time and of the keypad digits. `tnum` asks for the equal-width digits. Newsreader's own line is
// only as tall as its letters, with the digits in its upper part: they would sit higher than the text next to them
// and touch what is above. A line of 1.2 em with all the added height on top puts the digits in its middle.
val TimeStyle = TextStyle(
    fontFamily = Newsreader,
    fontFeatureSettings = "tnum",
    lineHeight = 1.2.em,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.None),
)

// The narrow column: the keypad and the controls that line up with it.
val ContentWidth = 260.dp

// The step list, its bar and the buttons under them, on every screen: the sound row and Done on the step screen too,
// so the screens line up.
val StepsWidth = 300.dp
