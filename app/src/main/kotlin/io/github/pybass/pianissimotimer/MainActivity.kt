package io.github.pybass.pianissimotimer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.pybass.pianissimotimer.sound.SoundPlayer
import io.github.pybass.pianissimotimer.ui.App

class MainActivity : ComponentActivity() {
    // Plays Listen. The activity owns it, so it lives through every recomposition and is released once.
    private val preview = SoundPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The background is always light, so the icons of both system bars stay dark even in system dark mode. The
        // default navigation bar style would also put the system's white scrim under the three navigation buttons.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent { App(preview) }
    }

    override fun onDestroy() {
        preview.release()
        super.onDestroy()
    }
}
