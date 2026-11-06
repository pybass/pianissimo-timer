package io.github.pybass.pianissimotimer

// Hours appear only when non-zero: 25:00, 1:30:00.
fun formatTime(h: Int, m: Int, s: Int): String {
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
}

fun formatSeconds(seconds: Int) = formatTime(seconds / 3600, seconds % 3600 / 60, seconds % 60)

// `left` is in ms. Rounds up: a timer shows 0:01 until the very end, and 0:00 never shows.
fun formatLeft(left: Long) = formatSeconds(((left + 999) / 1000).toInt().coerceAtLeast(0))

// Typed digits fill hh mm ss from the right, as in the Android Clock timer: "2500" is 25:00. Minutes and seconds may
// exceed 59: "9000" is 90 minutes.
const val MAX_DIGITS = 6

private fun digitsToParts(digits: String) = digits.padStart(MAX_DIGITS, '0').chunked(2).map { it.toInt() }

fun formatDigits(digits: String): String {
    val (h, m, s) = digitsToParts(digits)
    return formatTime(h, m, s)
}

fun digitsToSeconds(digits: String): Int {
    val (h, m, s) = digitsToParts(digits)
    return h * 3600 + m * 60 + s
}

// The digits that show `seconds`, normalized: 5400 s gives "13000" (1:30:00) even if the user had typed "9000".
fun secondsToDigits(seconds: Int) = formatSeconds(seconds).filter { it != ':' }.trimStart('0')
