package io.github.pybass.pianissimotimer.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Samples per second; the AudioTrack that plays the sound uses the same rate.
const val SAMPLE_RATE = 44100

// Everything above this is cut from every sound: high overtones sting in silence, and a phone speaker makes
// them sharper still.
private const val TOP_HZ = 2000.0

// Mono samples in -1..1 at SAMPLE_RATE, peak at -1 dBFS, just under full scale: the app can only turn a sound down,
// never up. The sound holds its tone for `rise` seconds while the loudness grows, then fades by 60 dB in `length`
// seconds; it ends 0.5 s after, with a fade-out instead of a cut.
internal fun synthesize(type: SoundType, note: Int, rise: Double, length: Double): FloatArray {
    val f0 = 440 * 2.0.pow((note - 69) / 12.0)
    val out = DoubleArray(((rise + length + 0.5) * SAMPLE_RATE).toInt())
    when (type) {
        // A 1.4 pair with a low index, decaying slowly, and a second carrier 1.5 Hz up for a slow shimmer.
        SoundType.BELL -> {
            fmPair(out, f0, 1.4, 1.0, length * 0.4, length, rise)
            fmPair(out, f0 + 1.5, 1.4, 1.0, length * 0.4, length, rise, amp = 0.7)
            softStart(out, 0.4)
        }

        // A 1:1 pair whose index dies fast, the tine's tick at 14x, and a body an octave below: the Rhodes recipe.
        SoundType.PIANO -> {
            fmPair(out, f0, 1.0, 1.6, 0.4, length, rise)
            fmPair(out, f0, 14.0, 0.15, 0.15, length * 0.3, rise, amp = 0.25)
            fmPair(out, f0 * 0.5, 1.0, 0.5, 0.6, length, rise, amp = 0.4)
            softStart(out, 0.3)
        }

        // A small index that never fades: hollow and steady, with the slowest start.
        SoundType.FLUTE -> {
            fmPair(out, f0, 2.0, 0.65, Double.POSITIVE_INFINITY, length, rise)
            fmPair(out, f0, 1.0, 0.2, Double.POSITIVE_INFINITY, length * 0.7, rise, amp = 0.3)
            softStart(out, 0.6)
        }

        SoundType.STRING -> {
            string(out, f0, length, rise)
            softStart(out, 0.15)
        }
    }
    // The rise: even in dB, from -40 dB to full, which the ear hears as a steady growth. A rise even in amplitude is
    // heard at once and then seems to stand still. The soft start of the type brings the sound from zero to -40 dB.
    val riseFrames = (rise * SAMPLE_RATE).toInt()
    for (i in 0 until riseFrames) out[i] *= 10.0.pow(-2.0 * (riseFrames - i) / riseFrames)
    lowPass(out, TOP_HZ)
    val fade = SAMPLE_RATE / 2
    for (i in 0 until fade) out[out.size - 1 - i] *= i.toDouble() / fade
    val scale = 10.0.pow(-1.0 / 20) / out.maxOf { abs(it) }
    return FloatArray(out.size) { (out[it] * scale).toFloat() }
}

// Adds one FM operator pair to `out`: a sine carrier at f0 whose phase is pushed by a sine modulator at f0*ratio.
// The index (how far the phase is pushed) sets the number of overtones; it fades in `indexDecay` seconds, so the
// sound starts rich and ends pure, like a real struck object. `decay` is the seconds to fade by 60 dB. Both fades
// start after `hold` seconds, so a slow rise ends on the full tone, not on what is left of it.
private fun fmPair(
    out: DoubleArray,
    f0: Double,
    ratio: Double,
    index: Double,
    indexDecay: Double,
    decay: Double,
    hold: Double,
    amp: Double = 1.0,
) {
    val k = ln(1000.0) / decay
    for (i in out.indices) {
        val t = i.toDouble() / SAMPLE_RATE
        val fading = max(0.0, t - hold)
        val idx = index * exp(-fading / indexDecay)
        out[i] += amp * exp(-k * fading) * sin(2 * PI * f0 * t + idx * sin(2 * PI * f0 * ratio * t))
    }
}

// Karplus-Strong: a burst of noise circles a delay line one period long; each pass it is averaged with its
// neighbour (which dulls the highs) and scaled so it fades by 60 dB in `length` seconds. For the first `hold` seconds
// the scale instead gives back what the averaging takes from the base tone: the string keeps its loudness and only
// loses overtones.
private fun string(out: DoubleArray, f0: Double, length: Double, hold: Double) {
    // The averaging adds half a sample of delay, so the line is half a sample short.
    val l = (SAMPLE_RATE / f0 - 0.5).roundToInt()
    val g = 0.001.pow(1 / (f0 * length))
    // A soft pluck: the noise is low-passed at 2.5 kHz before it enters the string. The seed keeps the sound the
    // same on every play.
    val random = Random(2)
    val c = 1 - exp(-2 * PI * 2500 / SAMPLE_RATE)
    var y = 0.0
    for (i in 0..l) {
        y += c * (random.nextDouble(-1.0, 1.0) - y)
        out[i] = y
    }
    // The averaging passes the base tone, whose period is l + 0.5 samples, at cos(PI / (l + 0.5)).
    val held = 1 / cos(PI / (l + 0.5))
    val holdFrames = (hold * SAMPLE_RATE).toInt()
    for (i in l + 1 until out.size) out[i] = (if (i < holdFrames) held else g) * 0.5 * (out[i - l] + out[i - l - 1])
}

// Raised cosine over the first `seconds`: starts with zero slope, so no click.
private fun softStart(out: DoubleArray, seconds: Double) {
    val n = (seconds * SAMPLE_RATE).toInt()
    for (i in 0 until n) out[i] *= 0.5 - 0.5 * cos(PI * i / n)
}

// Second-order Butterworth low-pass, in place. Coefficients from the RBJ Audio EQ Cookbook with Q = 1/sqrt(2),
// which makes the passband flat.
private fun lowPass(x: DoubleArray, cutoff: Double) {
    val w0 = 2 * PI * cutoff / SAMPLE_RATE
    val alpha = sin(w0) / (2 * sqrt(0.5))
    val a0 = 1 + alpha
    val b0 = (1 - cos(w0)) / 2 / a0
    val b1 = (1 - cos(w0)) / a0
    val a1 = -2 * cos(w0) / a0
    val a2 = (1 - alpha) / a0
    var x1 = 0.0
    var x2 = 0.0
    var y1 = 0.0
    var y2 = 0.0
    for (i in x.indices) {
        val xi = x[i]
        val y = b0 * xi + b1 * x1 + b0 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = xi
        y2 = y1
        y1 = y
        x[i] = y
    }
}
