package io.github.pybass.pianissimotimer

// Hours appear only when non-zero: 25:00, 1:30:00.
fun formatTime(h: Int, m: Int, s: Int): String {
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$ss" else "$m:$ss"
}

fun formatSeconds(seconds: Int) = formatTime(seconds / 3600, seconds % 3600 / 60, seconds % 60)

// `left` is in ms. Rounds up: a timer shows 0:01 until the very end, and 0:00 never shows.
fun formatLeft(left: Long) = formatSeconds(((left + 999) / 1000).toInt().coerceAtLeast(0))
