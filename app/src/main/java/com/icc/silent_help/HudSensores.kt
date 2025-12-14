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
import android.os.PowerManager
import com.google.android.material.chip.Chip
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import android.net.Uri
import android.util.Log
import android.util.Base64
import com.icc.silent_help.api.AlertRequest
import com.icc.silent_help.api.AlertResponse
import com.icc.silent_help.api.AudioRequest
import com.icc.silent_help.api.EndAlertRequest
import com.icc.silent_help.api.LocationUpdateRequest
import com.icc.silent_help.api.RetrofitClient
import com.icc.silent_help.utils.UserPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HudSensores : FragmentActivity(), AudioHandlerListener, LocationHandlerListener {

    // --- Handlers de Lógica ---
    private lateinit var audioHandler: AudioHandler
    private lateinit var locationHandler: LocationHandler

    // --- Vistas de la UI ---
    private lateinit var locationAddressTextView: TextView
    private lateinit var locationPrecisionTextView: TextView
    private lateinit var contactsStatusTextView: TextView
    private lateinit var stopAlertButton: Button
    private lateinit var timerChip: Chip
    private lateinit var audioCard: androidx.cardview.widget.CardView

    // --- Biometría ---
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // --- Variables para el temporizador ---
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var startTime: Long = 0
    private var endTime: Long = 0
    
    // --- Variables para grabación por fragmentos ---
    private var currentAlertId: String? = null
    private var isAlertActive: Boolean = false
    private val recordingHandler = Handler(Looper.getMainLooper())
    private var recordingDurationSeconds: Int = 120
    private var isChunkRecording = false
    
    // --- Proximidad ---
    private var wakeLock: PowerManager.WakeLock? = null
    
    // --- Flags de Estado ---
    private var pendingFinalization = false
    
    // --- Cola de audios pendientes ---
    private val pendingAudioFiles = mutableListOf<String>()

    // --- Control de creación de alerta ---
    private val alertCreationHandler = Handler(Looper.getMainLooper())
    private var isInitialAlertCreated = false

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
        contactsStatusTextView = findViewById(R.id.contactsStatusTextView)
        stopAlertButton = findViewById(R.id.stopAlertButton)
        timerChip = findViewById(R.id.timerChip)
        audioCard = findViewById(R.id.audioCard)

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

    
    private fun setupProximitySensor() {
        if (UserPreferences.isProximityEnabled(this)) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SilentHelp:ProximityWakeLock")
                wakeLock?.setReferenceCounted(false)
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 horas max
                }
            } else {
                Toast.makeText(this, "Sensor de proximidad no soportado para apagar pantalla", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun releaseProximitySensor() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun deactivateAlert() {
        isAlertActive = false
        stopTimer()
        locationHandler.stopLocationUpdates() // Detener GPS
        releaseProximitySensor()
        endTime = System.currentTimeMillis()
        
        // Detener grabación si está activa.
        // IMPORTANTE: Esto llamará a onRecordingStopped, pero como isAlertActive es false,
        // esa función sabrá que es el último chunk.
        if (isChunkRecording) {
            audioHandler.stopRecording()
        } else {
             // Si no estaba grabando (ej: solo gps), finalizamos la alerta directamente aquí
             finalizeAlertOnBackend()
        }
        
        // Detener callbacks de grabación (el loop)
        recordingHandler.removeCallbacksAndMessages(null)
        
        Toast.makeText(this, "Alerta Detenida", Toast.LENGTH_SHORT).show()
    }

    private fun finalizeAlertOnBackend() {
        if (currentAlertId == null) {
            // Si el ID es nulo, significa que createInitialAlert aún no ha regresado.
            // Marcamos flag para que, al regresar, se finalice automáticamente.
            Log.d("HUD", "Finalización pendiente: Esperando ID de alerta...")
            pendingFinalization = true
            return
        }

        val durationMillis = endTime - startTime
        val durationStr = formatTime(durationMillis)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val endTimeStr = sdf.format(Date(endTime))

        val request = EndAlertRequest(endTimeStr, durationStr)

        RetrofitClient.instance.endAlert(currentAlertId!!, request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("API", "Alerta finalizada correctamente")
                } else {
                    Log.e("API", "Error al finalizar alerta: ${response.code()}")
                }
                finish() // Cierra la actividad solo después de intentar finalizar
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("API", "Fallo al finalizar alerta: ${t.message}")
                finish()
            }
        })
    }

    private fun startAlertProcess() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationHandler.hasLocationPermission() && hasAudioPermission()) {
            isAlertActive = true
            
            // 1. Iniciar Ubicación
            if (UserPreferences.isGpsEnabled(this)) {
                val frequency = UserPreferences.getGpsFrequency(this)
                locationHandler.startLocationUpdates(frequency)
            } else {
                locationAddressTextView.text = "Ubicación desactivada por configuración"
                locationPrecisionTextView.text = ""
            }

            // 2. Iniciar Timer Visual
            startTimer()
            
            // 3. Crear Alerta (Esperar un poco para intentar obtener ubicación, si no, crear igual)
            alertCreationHandler.postDelayed({
                if (!isInitialAlertCreated) {
                    createInitialAlert("")
                    isInitialAlertCreated = true
                }
            }, 4000) // Esperar 4 segundos máximo

            // 4. Iniciar Grabación (si está habilitado)
            if (UserPreferences.isMicEnabled(this)) {
                recordingDurationSeconds = UserPreferences.getAudioDuration(this)
                startAudioChunkLoop()
            } else {
                Toast.makeText(this, "Grabación desactivada por configuración", Toast.LENGTH_SHORT).show()
            }

            // 5. Activar Proximidad
            setupProximitySensor()
            
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE)
        }
    }


    
    private fun startAudioChunkLoop() {
        if (!isAlertActive) return
        
        isChunkRecording = true
        audioHandler.startRecording()
        
        // Programar detención del chunk
        recordingHandler.postDelayed({
            if (isAlertActive && isChunkRecording) {
                audioHandler.stopRecording()
                // onRecordingStopped se encargará de subir y llamar a startAudioChunkLoop de nuevo
            }
        }, recordingDurationSeconds * 1000L)
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

    private fun createInitialAlert(filePath: String) {
        // Recuperar datos
        var currentAddress = locationAddressTextView.text.toString()
        if (currentAddress.isBlank() || currentAddress == "Buscando ubicación...") {
            currentAddress = "Ubicación en proceso..."
        }
        
        val durationMillis = System.currentTimeMillis() - startTime // Duración hasta ahora
        val durationStr = formatTime(durationMillis)
        
        // Formatear fechas
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val startTimeStr = sdf.format(Date(startTime))
        val endTimeStr = sdf.format(Date()) // Hora actual como fin preliminar
        
        val dateSdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
        val dateStr = dateSdf.format(Date(startTime))

        // Codificar audio a Base64 (puede ser vacío si filePath es "")
        val audioBase64 = if (filePath.isNotEmpty()) encodeAudioToBase64(filePath) else ""

        // Obtener ID real del usuario
        val realUserId = UserPreferences.getUserId(this) ?: "USUARIO_DESCONOCIDO"

        val request = AlertRequest(
            userId = realUserId,
            direccion = currentAddress,
            audios = if (audioBase64.isNotEmpty()) listOf(audioBase64) else emptyList(),
            startTime = startTimeStr,
            endTime = endTimeStr,
            date = dateStr,
            duration = durationStr
        )

        Toast.makeText(this, "Iniciando alerta...", Toast.LENGTH_SHORT).show()

        RetrofitClient.instance.createAlert(request).enqueue(object : Callback<AlertResponse> {
            override fun onResponse(call: Call<AlertResponse>, response: Response<AlertResponse>) {
                if (response.isSuccessful) {
                    val alertResponse = response.body()
                    currentAlertId = alertResponse?.alert?._id
                    Toast.makeText(this@HudSensores, "Alerta iniciada. ID: $currentAlertId", Toast.LENGTH_SHORT).show()
                    Log.d("API", "Alerta creada: ${alertResponse?.message}")

                    // Actualizar UI de contactos notificados
                    val notified = alertResponse?.notifiedContacts
                    if (notified != null && notified.isNotEmpty()) {
                         val names = notified.joinToString(", ")
                         contactsStatusTextView.text = "Notificados: $names"
                    } else {
                         contactsStatusTextView.text = "Enviando alertas..."
                    }
                    
                    // Procesar audios pendientes en cola
                    if (pendingAudioFiles.isNotEmpty()) {
                        Log.d("HUD", "Procesando ${pendingAudioFiles.size} audios en cola...")
                        val iterator = pendingAudioFiles.iterator()
                        while (iterator.hasNext()) {
                            val path = iterator.next()
                            sendAudioChunk(path)
                            iterator.remove()
                        }
                    }
                    
                    // Si había una finalización pendiente
                    if (pendingFinalization) {
                        finalizeAlertOnBackend()
                    }
                    
                    // Si seguimos activos y era grabación por chunks, el loop continuará en onRecordingStopped
                } else {
                    Toast.makeText(this@HudSensores, "Error al crear alerta: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AlertResponse>, t: Throwable) {
                Toast.makeText(this@HudSensores, "Fallo de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun sendAudioChunk(filePath: String) {
        if (currentAlertId == null) {
            Log.e("API", "No se puede enviar chunk: AlertID es nulo")
            return
        }

        val audioBase64 = encodeAudioToBase64(filePath)
        val request = AudioRequest(audio_base64 = audioBase64)
        
        Log.d("API", "Enviando chunk de audio...")
        
        RetrofitClient.instance.addAudioToAlert(currentAlertId!!, request).enqueue(object : Callback<AlertResponse> {
            override fun onResponse(call: Call<AlertResponse>, response: Response<AlertResponse>) {
                if (response.isSuccessful) {
                    Log.d("API", "Chunk de audio enviado correctamente")
                } else {
                    Log.e("API", "Error al enviar chunk: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AlertResponse>, t: Throwable) {
                Log.e("API", "Fallo al enviar chunk: ${t.message}")
            }
        })
    }

    private fun encodeAudioToBase64(path: String): String {
        return try {
            val file = File(path)
            val bytes = FileInputStream(file).use { it.readBytes() }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("Base64", "Error encoding audio: ${e.message}")
            ""
        }
    }

    // --- Callbacks de LocationHandlerListener ---
    override fun onLocationFound(address: String, precision: Float) {
        locationAddressTextView.text = address
        locationPrecisionTextView.text = "Precisión: ±${precision.toInt()} metros"
        
        // Si la alerta inicial aún no se ha creado, crearla ahora con la dirección encontrada
        if (!isInitialAlertCreated) {
            alertCreationHandler.removeCallbacksAndMessages(null) // Cancelar el timeout
            createInitialAlert("") // createInitialAlert tomará el texto actual del TextView
            isInitialAlertCreated = true
        }

        // Si ya tenemos ID de alerta, actualizamos la dirección en el backend inmediatamente
        if (currentAlertId != null) {
            // Nota: No tenemos lat/long aquí fácilmente sin guardarlos, pero podemos esperar al siguiente update
            // O idealmente, LocationHandler debería pasar lat/long a onLocationFound o viceversa.
            // Por simplicidad, solo actualizamos la UI y dejamos que el próximo onLocationUpdate lo envíe,
            // pero como onLocationUpdate es por intervalo, podría tardar.
            // Mejor opción: Si LocationHandler guarda la última ubicación, podríamos usarla.
        }
    }

    override fun onLocationUpdate(latitude: Double, longitude: Double) {
        if (currentAlertId != null) {
            val currentAddress = locationAddressTextView.text.toString()
            // Solo enviar dirección si no es el placeholder de "Buscando..."
            val addressToSend = if (currentAddress.contains("...")) null else currentAddress
            
            val request = LocationUpdateRequest(latitude, longitude, addressToSend)
            RetrofitClient.instance.updateLocation(currentAlertId!!, request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("API", "Ubicación actualizada: $latitude, $longitude")
                    } else {
                        Log.e("API", "Error al actualizar ubicación: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("API", "Fallo al enviar ubicación: ${t.message}")
                }
            })
        }
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
        isChunkRecording = false
        
        // Actualizar UI: Ocultar tarjeta de grabación
        runOnUiThread {
            audioCard.visibility = android.view.View.GONE
            Toast.makeText(this, "Grabación finalizada", Toast.LENGTH_SHORT).show()
        }
        
        if (currentAlertId == null) {
            // Si el ID aún no está listo, guardamos el audio en cola
            Log.d("HUD", "Alert ID pendiente. Audio en cola: $filePath")
            pendingAudioFiles.add(filePath)
        } else {
            // Ya existe alerta, enviamos audio directamente
            sendAudioChunk(filePath)
        }
        
        // NOTA: Eliminamos el bucle startAudioChunkLoop() para que solo grabe una vez
        
        if (!isAlertActive) {
            finalizeAlertOnBackend()
        }
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
        if (isFinishing) {
            releaseProximitySensor()
        }
    }
}
