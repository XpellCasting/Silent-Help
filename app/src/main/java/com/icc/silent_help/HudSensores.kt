package com.icc.silent_help

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.chip.Chip
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID

class HudSensores : FragmentActivity(), AudioHandlerListener, LocationHandlerListener {

    // --- Handlers de Lógica ---
    private lateinit var audioHandler: AudioHandler
    private lateinit var locationHandler: LocationHandler

    // --- Vistas de la UI ---
    private lateinit var locationAddressTextView: TextView
    private lateinit var locationPrecisionTextView: TextView
    private lateinit var stopAlertButton: Button
    private lateinit var timerChip: Chip

    // --- Biometría ---
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // --- Variables para el temporizador ---
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var startTime: Long = 0

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alerta_activada)

        // Inicializar Handlers
        audioHandler = AudioHandler(this, this)
        locationHandler = LocationHandler(this, this)

        // Vincular Vistas
        locationAddressTextView = findViewById(R.id.locationAddressTextView)
        locationPrecisionTextView = findViewById(R.id.locationPrecisionTextView)
        stopAlertButton = findViewById(R.id.stopAlertButton)
        timerChip = findViewById(R.id.timerChip)

        // Configurar Biometría
        setupBiometrics()

        // Configurar Listeners de botones
        stopAlertButton.setOnClickListener {
            val prefs = getSharedPreferences("BiometricPrefs", Context.MODE_PRIVATE)
            val useBiometrics = prefs.getBoolean("use_biometrics", false)

            if (useBiometrics) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                deactivateAlert()
            }
        }

        // Iniciar el proceso de alerta
        startAlertProcess()
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    deactivateAlert()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@HudSensores, "Autenticación cancelada", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@HudSensores, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificación de huella")
            .setSubtitle("Usa tu huella para desactivar la alerta")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun deactivateAlert() {
        stopTimer()
        audioHandler.stopRecording()
        Toast.makeText(this, "Alerta Detenida", Toast.LENGTH_SHORT).show()
    }

    private fun startAlertProcess() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationHandler.hasLocationPermission() && hasAudioPermission()) {
            locationHandler.requestLocation()
            audioHandler.startRecording()
            startTimer()
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE)
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerRunnable = Runnable {
            val millis = System.currentTimeMillis() - startTime
            val formattedTime = formatTime(millis)
            timerChip.text = formattedTime
            timerHandler.postDelayed(timerRunnable, 1000)
        }
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun uploadAudioToFirebase(filePath: String) {
        // 1. Mostrar un indicador de carga para que el usuario sepa que se está guardando
        Toast.makeText(this, "Subiendo evidencia...", Toast.LENGTH_SHORT).show()
        // Aquí podrías mostrar un ProgressBar en la UI si quisieras

        // 2. Referencia a Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference
        val file = Uri.fromFile(File(filePath))

        // Crea una ruta única: audios/usuario_id/nombre_archivo.3gp
        val audioRef = storageRef.child("alertas/${UUID.randomUUID()}.3gp")

        // 3. Subir el archivo
        val uploadTask = audioRef.putFile(file)

        uploadTask.addOnSuccessListener {
            // 4. Si se sube bien, obtenemos la URL de descarga
            audioRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                Log.d("Firebase", "Audio subido: $downloadUrl")

                // AQUI es donde envías los datos a tu MongoDB
                sendAlertToBackend(downloadUrl)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al subir audio: ${e.message}", Toast.LENGTH_LONG).show()
            // Aunque falle la subida, quizás quieras cerrar la app o guardar localmente
            finish()
        }
    }

    private fun sendAlertToBackend(audioUrl: String) {
        // Recuperar el texto de la ubicación actual de tus TextViews o variables
        val currentAddress = locationAddressTextView.text.toString()
        val timestamp = System.currentTimeMillis()

        // Crear el objeto (Pseudo-código, adáptalo a tu librería de HTTP como Retrofit o Ktor)
        val alertaData = hashMapOf(
            "audio_url" to audioUrl,
            "direccion" to currentAddress,
            "fecha" to timestamp,
            "usuario_id" to "ID_DEL_USUARIO_ACTUAL" // Si tienes login
        )

        // AQUI haces la petición POST a tu servidor (Node/Express/Mongo)
        // Ejemplo ficticio:
        // ApiService.crearAlerta(alertaData) {
        //      Toast.makeText(this, "Alerta registrada en historial", Toast.LENGTH_LONG).show()
        //      finish() // AHORA SÍ cerramos la actividad
        // }

        // Si no tienes el backend listo aún, solo cierra la activity por ahora:
        Toast.makeText(this, "URL lista para Mongo: $audioUrl", Toast.LENGTH_LONG).show()
        finish()
    }

    // --- Callbacks de LocationHandlerListener ---
    override fun onLocationFound(address: String, precision: Float) {
        locationAddressTextView.text = address
        locationPrecisionTextView.text = "Precisión: ±${precision.toInt()} metros"
    }

    override fun onLocationError(message: String) {
        locationAddressTextView.text = message
        locationPrecisionTextView.text = "Precisión: desconocida"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // --- Callbacks de AudioHandlerListener ---
    override fun onRecordingStarted() {
        Toast.makeText(this, "Grabación de evidencia iniciada", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordingStopped(filePath: String) {
        Toast.makeText(this, "Evidencia de audio guardada", Toast.LENGTH_SHORT).show()
        uploadAudioToFirebase(filePath)
    }
	
    override fun onPlayingStarted() { /* No se usa en esta pantalla */ }
    override fun onPlayingStopped() { /* No se usa en esta pantalla */ }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            val audioGranted = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED
            val locationGranted = grantResults.getOrNull(1) == PackageManager.PERMISSION_GRANTED

            if (audioGranted && locationGranted) {
                startAlertProcess()
            } else {
                Toast.makeText(this, "Se requieren ambos permisos para activar la alerta.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioHandler.releaseResources()
    }
}
