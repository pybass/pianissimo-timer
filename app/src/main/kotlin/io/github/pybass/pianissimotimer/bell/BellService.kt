package io.github.pybass.pianissimotimer.bell

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.sound.DefaultSound
import io.github.pybass.pianissimotimer.sound.SoundPlayer

// Intent extras: the bell as JSON, the time of the next step's bell in ms since the epoch, absent for the last step,
// and the number of steps after this one.
const val BELL = "bell"
const val NEXT_DUE = "nextDue"
const val STEPS_LEFT = "stepsLeft"

// Plays the bell; AlarmManager starts it at the bell's time, even when the app is not running.
// A foreground service, because Android cuts an app's background audio within seconds.
class BellService : Service() {
    private val player = SoundPlayer()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("Bell", "Bell is due")
        // The waiting notification belonged to this bell. With a next step it now counts down to that one.
        val nextDue = intent?.getLongExtra(NEXT_DUE, 0) ?: 0
        val stepsLeft = intent?.getIntExtra(STEPS_LEFT, 0) ?: 0
        if (nextDue > 0) showWaitingNotification(this, nextDue, stepsLeft) else hideWaitingNotification(this)
        val notification = bellNotification(this, R.drawable.ic_bell_ringing)
            .setContentText(if (nextDue > 0) "Next step" else "Time is up")
            // Android delays a foreground service's notification by up to 10 s; a short bell would end before it showed.
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(button(this, "Stop", ACTION_STOP))
            .build()
        startForeground(RINGING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        // Stopping the service also removes its notification. A missing or unreadable bell plays the default sound
        // at full volume.
        val bell = intent?.getStringExtra(BELL)?.let(Bell::fromJson)
        player.play(bell?.sound ?: DefaultSound, bell?.volumeDb ?: 0) { stopSelf() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
