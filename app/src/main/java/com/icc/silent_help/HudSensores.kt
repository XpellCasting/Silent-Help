package com.icc.silent_help

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat


class HudSensores : ComponentActivity(), AudioHandlerListener, LocationHandlerListener {

    // --- Handlers de Lógica ---
    private lateinit var audioHandler: AudioHandler
    private lateinit var locationHandler: LocationHandler

    // --- Vistas de la UI ---
    private lateinit var locationAddressTextView: TextView
    private lateinit var locationPrecisionTextView: TextView
    private lateinit var stopAlertButton: Button


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

        // Configurar Listeners de botones
        stopAlertButton.setOnClickListener {
            audioHandler.stopRecording()
            // Lógica para detener la alerta
            Toast.makeText(this, "Alerta Detenida", Toast.LENGTH_SHORT).show()

            finish()
        }

        // Iniciar el proceso de alerta
        startAlertProcess()
    }

    private fun startAlertProcess() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationHandler.hasLocationPermission() && hasAudioPermission()) {
            // Si ya tenemos permisos, iniciar todo
            locationHandler.requestLocation()
            audioHandler.startRecording()
        } else {
            // Si no, pedirlos
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE)
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
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

    }

    override fun onPlayingStarted() { /* No se usa en esta pantalla */ }
    override fun onPlayingStopped() { /* No se usa en esta pantalla */ }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            // Verificar si ambos permisos fueron concedidos
            val audioGranted = grantResults.getOrNull(0) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val locationGranted = grantResults.getOrNull(1) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (audioGranted && locationGranted) {
                startAlertProcess()
            } else {
                Toast.makeText(this, "Se requieren ambos permisos para activar la alerta.", Toast.LENGTH_LONG).show()
                finish() // Cierra la app si no se dan los permisos necesarios
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Liberar recursos para evitar fugas de memoria
        audioHandler.releaseResources()
    }
}

private fun ComponentActivity.onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
}
