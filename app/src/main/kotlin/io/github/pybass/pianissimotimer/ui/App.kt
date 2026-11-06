@file:Suppress("FunctionName")

package io.github.pybass.pianissimotimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pybass.pianissimotimer.sound.Sound
import io.github.pybass.pianissimotimer.sound.SoundLibrary
import io.github.pybass.pianissimotimer.sound.SoundPlayer
import io.github.pybass.pianissimotimer.sound.SoundStore
import io.github.pybass.pianissimotimer.timer.Step
import io.github.pybass.pianissimotimer.timer.TimerLibrary
import io.github.pybass.pianissimotimer.timer.TimerState
import io.github.pybass.pianissimotimer.timer.TimerStore
import io.github.pybass.pianissimotimer.timer.newTimer
import io.github.pybass.pianissimotimer.ui.screens.CountdownScreen
import io.github.pybass.pianissimotimer.ui.screens.PausedScreen
import io.github.pybass.pianissimotimer.ui.screens.SoundScreen
import io.github.pybass.pianissimotimer.ui.screens.StepScreen
import io.github.pybass.pianissimotimer.ui.screens.TimerScreen
import io.github.pybass.pianissimotimer.ui.theme.Brass
import io.github.pybass.pianissimotimer.ui.theme.Newsreader
import io.github.pybass.pianissimotimer.ui.theme.Paper
import io.github.pybass.pianissimotimer.ui.theme.Soft
import kotlinx.coroutines.launch

// A sound in the editor. For an existing one, `sound` is as saved, so its name finds it in the store.
private data class Draft(val sound: Sound, val isNew: Boolean)

// The whole app. The timer state picks the screen; every change goes to a store, and the screen follows the store.
// `preview` plays Listen; the activity owns it.
@Composable
fun App(preview: SoundPlayer) {
    val context = LocalContext.current.applicationContext
    val timerStore = remember { TimerStore(context) }
    val soundStore = remember { SoundStore(context) }
    // All three are null until read from disk.
    val state by timerStore.state.collectAsState(initial = null)
    val timers by timerStore.library.collectAsState(initial = null)
    val sounds by soundStore.library.collectAsState(initial = null)
    Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Header()
        // The screen gets all the height under the header; a screen that does not fill it sits in the middle.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Screen(state, timers, sounds, timerStore, soundStore, preview)
        }
    }
}

@Composable
private fun Screen(
    state: TimerState?,
    timers: TimerLibrary?,
    sounds: SoundLibrary?,
    timerStore: TimerStore,
    soundStore: SoundStore,
    preview: SoundPlayer,
) {
    val scope = rememberCoroutineScope()
    when (val s = state) {
        null -> {}

        TimerState.Idle -> {
            val tl = timers
            val lib = sounds
            if (tl != null && lib != null) IdleScreens(tl, lib, timerStore, soundStore, preview)
        }

        is TimerState.Running -> CountdownScreen(
            running = s,
            onPause = { scope.launch { timerStore.pause(s) } },
            onStop = { scope.launch { timerStore.stop() } },
            onDone = { scope.launch { timerStore.done() } },
        )

        is TimerState.Paused -> PausedScreen(
            paused = s,
            onResume = { scope.launch { timerStore.resume(s) } },
            onStop = { scope.launch { timerStore.stop() } },
        )
    }
}

// The screens while no timer runs: the chosen timer; over it a step while one is open; over that a sound while a
// draft is open.
@Composable
private fun IdleScreens(
    timers: TimerLibrary,
    sounds: SoundLibrary,
    timerStore: TimerStore,
    soundStore: SoundStore,
    preview: SoundPlayer,
) {
    val scope = rememberCoroutineScope()
    // The index of the open step; null when none is open.
    var editing by remember { mutableStateOf<Int?>(null) }
    // The sound open in the sound editor; null when the editor is closed.
    var draft by remember { mutableStateOf<Draft?>(null) }
    val timer = timers.selectedTimer
    // Kept here, not in the timer screen: that screen leaves while a step is open, and the list must come back where
    // it was. Each timer gets a new one, so another timer opens at its first step.
    val stepsScroll = remember(timers.selected) { ScrollState(0) }
    val i = editing
    // `getOrNull`: right after Add step the index may point past the list this composition still sees.
    val step = i?.let { timer.steps.getOrNull(it) }
    val d = draft
    if (i == null || step == null) {
        TimerScreen(
            timers = timers,
            onSelectTimer = { scope.launch { timerStore.select(it) } },
            // A new timer takes the first sound in the library: it always exists, unlike a sound by name.
            onNewTimer = { scope.launch { timerStore.add(newTimer(timers.newName(), sounds.sounds.first().name)) } },
            onRenameTimer = { t, name -> scope.launch { timerStore.replace(t.name, t.copy(name = name)) } },
            onDeleteTimer = { scope.launch { timerStore.delete(it.name) } },
            sounds = sounds,
            stepsScroll = stepsScroll,
            onEditStep = { editing = it },
            // A new step is a copy of the last one, time included: a step is never stored with 0:00 (docs/spec.md 5.1).
            // It opens only once it is saved, so the step screen finds it in the timer.
            onAddStep = {
                scope.launch {
                    timerStore.replace(timer.name, timer.copy(steps = timer.steps + timer.steps.last()))
                    editing = timer.steps.size
                }
            },
            onListen = { timer.steps[it].let { step -> preview.play(sounds.soundFor(step.sound), step.volumeDb) } },
            onStart = { scope.launch { timerStore.start(timer, sounds) } },
        )
        return
    }

    suspend fun saveStep(new: Step) = timerStore.replace(timer.name, timer.withStep(i, new))
    if (d == null) {
        StepScreen(
            timerName = timer.name,
            steps = timer.steps,
            number = i + 1,
            step = step,
            onChange = { scope.launch { saveStep(it) } },
            sounds = sounds,
            onNewSound = { draft = Draft(sounds.soundFor(step.sound).copy(name = sounds.newName()), isNew = true) },
            onEditSound = { draft = Draft(it, isNew = false) },
            onListen = { preview.play(it, step.volumeDb) },
            // The screen closes first: with the step gone, its index would point past the end.
            onDelete = {
                editing = null
                scope.launch { timerStore.replace(timer.name, timer.withoutStep(i)) }
            },
            canDelete = timer.steps.size > 1,
            onDone = { editing = null },
        )
        return
    }
    SoundScreen(
        initial = d.sound,
        isNew = d.isNew,
        // The sound's own name is free for it, so a rename may change only the case.
        isTaken = { !it.equals(d.sound.name, ignoreCase = true) && sounds.isTaken(it) },
        onListen = { preview.play(it, step.volumeDb) },
        // Saves the new name with the saved settings, not the ones on the sliders. The draft takes the new name too,
        // so Save and Delete find the sound by it.
        onRename = { name ->
            val renamed = d.sound.copy(name = name)
            scope.launch {
                soundStore.replace(d.sound.name, renamed)
                // The step keeps its sound under the new name.
                if (step.sound == d.sound.name) saveStep(step.copy(sound = name))
            }
            draft = d.copy(sound = renamed)
        },
        // The editor closes once the change is saved, so the step screen already shows it. A new sound becomes the
        // step's sound.
        onSave = { sound ->
            scope.launch {
                if (d.isNew) {
                    soundStore.add(sound)
                    saveStep(step.copy(sound = sound.name))
                } else {
                    soundStore.replace(d.sound.name, sound)
                }
                draft = null
            }
        },
        onDelete = {
            scope.launch {
                soundStore.delete(d.sound.name)
                draft = null
            }
        },
        canDelete = sounds.sounds.size > 1,
        onCancel = { draft = null },
    )
}

// A running head: the fermata and the name. Large enough to read as a name, but in Soft, so the timer's name below is
// still the first thing read. One word: under the launcher icon "Timer" helps to find the app, here it says nothing.
@Composable
private fun Header() {
    Row(
        Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Drawn here, not an icon file: an icon takes one tint, and the dot has its own colour.
        Canvas(Modifier.size(24.dp)) {
            // A half circle of radius 8.5 dp around (12, 16), the dot just inside its top: the shape of the app icon.
            val arc = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Soft, 180f, 180f, false, Offset(3.5.dp.toPx(), 7.5.dp.toPx()), Size(17.dp.toPx(), 17.dp.toPx()), style = arc)
            drawCircle(Brass, 2.dp.toPx(), Offset(12.dp.toPx(), 15.dp.toPx()))
        }
        Text("Pianissimo", color = Soft, fontSize = 21.sp, fontFamily = Newsreader, fontStyle = FontStyle.Italic)
    }
}
