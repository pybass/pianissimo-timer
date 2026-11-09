package io.github.pybass.pianissimotimer.bell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.pybass.pianissimotimer.data.AppDataStore
import io.github.pybass.pianissimotimer.timer.TimerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// The intent actions of the notification buttons.
const val ACTION_PAUSE = "pause"
const val ACTION_RESUME = "resume"
const val ACTION_STOP = "stop"

// Sent when the user swipes the waiting or paused notification away: it must come back (docs/spec.md 2.2).
const val ACTION_SHOW = "show"

// Runs a notification button. Android starts it when the button is tapped, even when the app is not running, and
// gives it a few seconds. The store does the work, so the buttons and the screen keep the state the same way.
class BellActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = AppDataStore(context.applicationContext)
        // onReceive cannot suspend and must return at once; the store's read and write take milliseconds.
        runBlocking {
            // A button that does not fit the state is ignored: the notification may lag a moment behind a change made
            // in the app, so Pause may arrive for a timer that is already paused.
            when (val state = store.data.first().state) {
                is TimerState.Running -> when (intent.action) {
                    ACTION_PAUSE -> store.pause(state)

                    ACTION_STOP -> store.stop()

                    ACTION_SHOW -> {
                        val now = System.currentTimeMillis()
                        val step = state.step(now)
                        // Once every bell is due there is nothing left to wait for.
                        val due = state.bells[step].dueAt
                        if (due > now) showWaitingNotification(context, due, stepsLeft = state.bells.size - step)
                    }
                }

                is TimerState.Paused -> when (intent.action) {
                    ACTION_RESUME -> store.resume(state)
                    ACTION_STOP -> store.stop()
                    ACTION_SHOW -> showPausedNotification(context, state.left, state.bells.size - state.step)
                }

                // The last bell may ring while the state is already Idle: the app saves Idle as soon as it is due, and
                // Android may have dropped its alarm. Stop must still silence it.
                TimerState.Idle -> if (intent.action == ACTION_STOP) cancelBells(context)
            }
        }
    }
}
