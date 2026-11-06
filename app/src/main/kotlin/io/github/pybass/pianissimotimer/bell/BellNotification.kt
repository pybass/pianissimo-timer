package io.github.pybass.pianissimotimer.bell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.pybass.pianissimotimer.R
import io.github.pybass.pianissimotimer.formatLeft

// Separate IDs: a bell started while another one rings keeps its waiting notification when the ringing one goes away.
// The waiting and paused notifications share one ID, so each replaces the other.
private const val WAITING_ID = 1
const val RINGING_ID = 2

private const val CHANNEL_ID = "bell"

// The fermata icon in the status bar, with the time left as a chip next to the clock, until the bell rings.
// `stepsLeft` counts the steps not finished yet; the launcher may show it as a badge on the app icon.
fun showWaitingNotification(context: Context, due: Long, stepsLeft: Int) {
    val notification = bellNotification(context, R.drawable.ic_bell_waiting)
        .setContentText("Bell is waiting")
        .setNumber(stepsLeft)
        // The system counts down to `due` by itself, so the app does not update the notification.
        .setWhen(due)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setOngoing(true)
        // A Live Update: the status bar shows the chip with the time left.
        .setRequestPromotedOngoing(true)
        .addExtras(samsungLiveUpdate())
        .setDeleteIntent(actionIntent(context, ACTION_SHOW))
        .addAction(button(context, "Pause", ACTION_PAUSE))
        .addAction(button(context, "Stop", ACTION_STOP))
        .build()
    context.getSystemService(NotificationManager::class.java).notify(WAITING_ID, notification)
}

// `left` is in ms. A Live Update with the time left frozen in the chip: without the short text Samsung One UI scrolls
// the title there. A plain low-importance notification would keep its icon out of the status bar, and the state must
// stay visible (docs/spec.md 2.5).
fun showPausedNotification(context: Context, left: Long, stepsLeft: Int) {
    val notification = bellNotification(context, R.drawable.ic_bell_paused)
        .setContentText("Paused, ${formatLeft(left)} left")
        .setShortCriticalText(formatLeft(left))
        .setNumber(stepsLeft)
        .setShowWhen(false)
        .setOngoing(true)
        .setRequestPromotedOngoing(true)
        .addExtras(samsungLiveUpdate())
        .setDeleteIntent(actionIntent(context, ACTION_SHOW))
        .addAction(button(context, "Resume", ACTION_RESUME))
        .addAction(button(context, "Stop", ACTION_STOP))
        .build()
    context.getSystemService(NotificationManager::class.java).notify(WAITING_ID, notification)
}

// Samsung One UI ignores `setRequestPromotedOngoing` without this undocumented extra. Never add
// `android.ongoingActivityNoti.style`: it cancels the effect.
private fun samsungLiveUpdate() = Bundle().apply { putBoolean("android.ongoingActivityNoti.automation", true) }

fun hideWaitingNotification(context: Context) {
    context.getSystemService(NotificationManager::class.java).cancel(WAITING_ID)
}

// A button that sends `action` to BellActionReceiver. No icon: the shade shows only the title.
fun button(context: Context, title: String, action: String): Notification.Action =
    Notification.Action.Builder(null, title, actionIntent(context, action)).build()

// Sends `action` to BellActionReceiver. Explicit intents with different actions are different, so one request code
// serves them all. As a delete intent it runs when the user swipes the notification away, not when the app removes it.
private fun actionIntent(context: Context, action: String): PendingIntent {
    val intent = Intent(context, BellActionReceiver::class.java).setAction(action)
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}

fun bellNotification(context: Context, icon: Int): Notification.Builder {
    // Low importance: no sound and no pop-up of its own. Creating an existing channel again keeps its settings.
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(NotificationChannel(CHANNEL_ID, "Bell", NotificationManager.IMPORTANCE_LOW))
    return Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(icon)
        .setContentTitle("Pianissimo Timer")
}
