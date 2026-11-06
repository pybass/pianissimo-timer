package io.github.pybass.pianissimotimer.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

// 30 ms: long enough to hide a cut in the middle of the wave, which is heard as a click.
private const val FADE_FRAMES = SAMPLE_RATE * 30 / 1000

// 10 ms: the frames the writer mixes at a time.
private const val CHUNK_FRAMES = SAMPLE_RATE / 100

// 100 ms: a new sound starts up to this much later, but a writer late by less never runs the stream dry, which would
// click too.
private const val BUFFER_FRAMES = SAMPLE_RATE / 10

// A sound in the stream. `gain` is linear; `pos` is the next sample to write.
private class Voice(val samples: FloatArray, val gain: Float, val onDone: () -> Unit) {
    var pos = 0
}

// Plays one sound at a time, for the bell and for Listen; a new play fades out the previous sound, then starts. All
// sounds in a row go through one stream, and the fades are mixed here: opening and closing a system stream for each
// sound clicked on the phone. The owner calls release when done.
class SoundPlayer {
    private val main = Handler(Looper.getMainLooper())

    // Guards the fields below: play and release change them while the writer thread reads them.
    private val lock = Any()
    private var voice: Voice? = null

    // Starts once `voice` has faded out.
    private var next: Voice? = null

    // Frames left in the fade-out of `voice`; 0 when it is not fading.
    private var fadeLeft = 0

    // False once the writer has nothing left to mix; the stream may still play out its buffer.
    private var writing = false

    // `volumeDb` is 0 or less: how far below its full loudness the sound plays, on top of the alarm volume.
    // `onDone` runs on the main thread when the sound has played to the end, not when it is cut short.
    fun play(sound: Sound, volumeDb: Int, onDone: () -> Unit = {}) {
        // A linear gain applied on top of the alarm volume; the system volumes stay untouched (docs/spec.md 1.7).
        val new = Voice(sound.samples(), 10.0.pow(volumeDb / 20.0).toFloat(), onDone)
        synchronized(lock) {
            if (voice == null) {
                voice = new
            } else {
                next = new
                if (fadeLeft == 0) fadeLeft = FADE_FRAMES
            }
            if (!writing) {
                writing = true
                thread(name = "SoundPlayer") { write() }
            }
        }
    }

    // Fades out the sound; the stream closes once it is silent.
    fun release() {
        synchronized(lock) {
            next = null
            if (voice != null && fadeLeft == 0) fadeLeft = FADE_FRAMES
        }
    }

    // Runs on the writer thread for as long as sounds follow each other.
    private fun write() {
        // Audio priority: a writer that runs late leaves a gap in the stream.
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val track = AudioTrack.Builder()
            // Alarm usage: plays on the alarm volume and passes Do Not Disturb when it allows alarms.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            ).setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(BUFFER_FRAMES * Float.SIZE_BYTES)
            .build()
        track.play()
        val chunk = FloatArray(CHUNK_FRAMES)
        var written = 0L // frames
        // A sound that has played to its end in the stream; its onDone waits until the speaker reaches endFrame.
        var ended: Voice? = null
        var endFrame = 0L
        while (true) {
            val silent = synchronized(lock) {
                if (voice == null) {
                    // Under the lock, so a play from now on opens a new stream.
                    writing = false
                    return@synchronized true
                }
                for (i in chunk.indices) {
                    val v = voice
                    if (v == null) {
                        chunk[i] = 0f
                        continue
                    }
                    val fading = fadeLeft > 0
                    // Raised cosine from 1 to 0: starts and ends with zero slope, so the fade itself does not click.
                    val fade = if (fading) 0.5f - 0.5f * cos(PI.toFloat() * fadeLeft-- / FADE_FRAMES) else 1f
                    chunk[i] = v.samples[v.pos++] * v.gain * fade
                    val finished = v.pos == v.samples.size
                    if ((fading && fadeLeft == 0) || finished) {
                        if (finished && !fading) {
                            ended = v
                            endFrame = written + i + 1
                        }
                        voice = next
                        next = null
                        fadeLeft = 0
                    }
                }
                false
            }
            if (silent) break
            // A failed write returns an error code instead of throwing; unchecked, the bell would be silent.
            val result = track.write(chunk, 0, CHUNK_FRAMES, AudioTrack.WRITE_BLOCKING)
            check(result == CHUNK_FRAMES) { "AudioTrack.write returned $result" }
            written += CHUNK_FRAMES
            ended?.let {
                if (track.playbackHeadPosition >= endFrame) {
                    main.post(it.onDone)
                    ended = null
                }
            }
        }
        // Lets the written frames play out. The bound guards against a stream that stops moving.
        val deadline = SystemClock.uptimeMillis() + 1000
        while (track.playbackHeadPosition < written && SystemClock.uptimeMillis() < deadline) Thread.sleep(10)
        track.release()
        ended?.let { main.post(it.onDone) }
    }
}
