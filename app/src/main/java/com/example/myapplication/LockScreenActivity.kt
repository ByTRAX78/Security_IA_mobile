package com.example.myapplication

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.concurrent.TimeUnit

class LockScreenActivity : Activity() {
    private lateinit var tvTimer: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var remainingTime = TimeUnit.MINUTES.toMillis(1) // 1 minuto
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configura la ventana antes de setContentView
        setupKioskMode()

        setContentView(R.layout.activity_lock_screen)
        tvTimer = findViewById(R.id.tvTimer)

        // Inicia el temporizador
        startCountdown()
    }

    private fun setupKioskMode() {
        // Previene que la actividad sea cerrada por el usuario
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configuración de banderas para modo inmersivo pegajoso
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        try {
            // Habilita el modo kiosk
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupKioskMode()
        }
    }

    // Previene el uso del botón de retroceso
    override fun onBackPressed() {
        // No hace nada
    }

    // Previene el uso de teclas de volumen y otras teclas del sistema
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun startCountdown() {
        runnable = object : Runnable {
            override fun run() {
                if (remainingTime > 0) {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60
                    tvTimer.text = String.format("%02d:%02d", minutes, seconds)
                    remainingTime -= 1000
                    handler.postDelayed(this, 1000)
                } else {
                    exitKioskMode()
                }
            }
        }
        handler.post(runnable)
    }

    private fun exitKioskMode() {
        try {
            stopLockTask()
            // Reinicia la aplicación en modo normal
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}