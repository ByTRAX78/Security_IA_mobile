package com.example.myapplication

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.LockScreenActivity

class MainActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val ADMIN_PERMISSION_REQUEST = 123
    private val FORCE_LOGIN = "FORCE_LOGIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate() llamado")
        // Inicializar Device Policy Manager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        // Hacer la barra de estado blanca
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContentView(R.layout.activity_main)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)

        // Inicializar MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.song)
        mediaPlayer?.isLooping = true

        // Verificar si se requiere login forzoso
        val forceLogin = intent.getBooleanExtra(FORCE_LOGIN, false)
        if (forceLogin) {
            // Ocultar botones adicionales y forzar login
            createAccountButton.visibility = View.GONE
            signInButton.text = "Iniciar Sesión para Desbloquear"
        }

        // Solicitar permisos de administrador si no están habilitados
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Se necesitan permisos para bloquear el dispositivo")
            }
            startActivityForResult(intent, ADMIN_PERMISSION_REQUEST)
        }

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Log.w("LoginApp", "Campos vacíos detectados")
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar credenciales (aquí deberías implementar tu lógica de autenticación real)
            if (isValidCredentials(email, password)) {
                // Si es un login forzoso tras desbloqueo
                if (intent.getBooleanExtra(FORCE_LOGIN, false)) {
                    // Lanzar la actividad de autenticación
                    val authIntent = Intent(this, LockScreenActivity::class.java)
                    startActivity(authIntent)
                } else {
                    // Lógica de login normal
                    controlSonido(signInButton)
                }
            } else {
                Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show()
            }
        }

        createAccountButton.setOnClickListener {
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                try {
                    // Solo intentamos bloquear el dispositivo sin configurar contraseña
                    devicePolicyManager.lockNow()
                    Toast.makeText(this, "Dispositivo bloqueado", Toast.LENGTH_SHORT).show()

                    // Establecer FORCE_LOGIN en true
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra(FORCE_LOGIN, true)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Error al bloquear dispositivo", e)
                }
            } else {
                // Solicitar permisos de administrador si no están activos
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Se necesitan permisos para bloquear el dispositivo")
                }
                startActivityForResult(intent, ADMIN_PERMISSION_REQUEST)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume() llamado")

        // Verificar si el dispositivo se acaba de desbloquear
        Log.d("MainActivity", "Verificando si se requiere login forzoso (FORCE_LOGIN: ${intent.getBooleanExtra(FORCE_LOGIN, false)})")
        if (intent.getBooleanExtra(FORCE_LOGIN, false)) {
            Log.d("MainActivity", "Dispositivo recién desbloqueado, lanzando AuthenticationActivity")
            // Lanzar la actividad de autenticación
            val authIntent = Intent(this, LockScreenActivity::class.java)
            startActivity(authIntent)
        }
    }

    private fun controlSonido(signInButton: Button) {
        // Controlar el sonido
        if (!isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
            signInButton.text = "Detener Sonido"
        } else {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
            isPlaying = false
            signInButton.text = "Sign in"
        }

        // Aquí agregarías tu lógica de navegación al menú principal
    }

    // Método de ejemplo para validar credenciales (REEMPLAZAR CON TU LÓGICA REAL)
    private fun isValidCredentials(email: String, password: String): Boolean {
        // Implementa tu lógica de autenticación aquí
        // Por ejemplo, comparar con una base de datos o servicio de autenticación
        return email == "1" && password == "1"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADMIN_PERMISSION_REQUEST) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}