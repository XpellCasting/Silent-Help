package com.icc.silent_help

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var playButton: Button
    private lateinit var stopPlayButton: Button
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var isPlaying = false

    private lateinit var alertButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val REQUEST_LOCATION_PERMISSION = 101
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupAudioButtons()

        alertButton = findViewById(R.id.alertButton)
        alertButton.setOnClickListener {
            handleLocationAlert()
        }
    }

    private fun setupAudioButtons() {

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        playButton = findViewById(R.id.playButton)
        stopPlayButton = findViewById(R.id.stopPlayButton)

        stopButton.isEnabled = false
        playButton.isEnabled = false
        stopPlayButton.isEnabled = false

        startButton.setOnClickListener {
            if (checkPermission(Manifest.permission.RECORD_AUDIO)) {
                startRecording()
            } else {
                requestPermission(Manifest.permission.RECORD_AUDIO, REQUEST_RECORD_AUDIO_PERMISSION)
            }
        }
        stopButton.setOnClickListener { stopRecording() }
        playButton.setOnClickListener { startPlaying() }
        stopPlayButton.setOnClickListener { stopPlaying() }
    }



    private fun handleLocationAlert() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            getLocationAndAddress()
        } else {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_LOCATION_PERMISSION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndAddress() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addressText = if (addresses != null && addresses.isNotEmpty()) {
                        addresses[0].getAddressLine(0)
                    } else {
                        "No se encontró la dirección."
                    }
                    showLocationAlert( addressText)
                } catch (e: IOException) {
                    Log.e("GeocoderError", "Servicio de Geocoder no disponible", e)
                    Toast.makeText(this, "Error al obtener la dirección", Toast.LENGTH_SHORT).show()

                    showLocationAlert( "Error al buscar dirección.")
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLocationAlert( address: String) {
        val alertMessage = "Ubicación: $address "
        Log.d("SilentHelp", alertMessage)
        Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show()
    }


    private fun startRecording() {
        if (isPlaying) stopPlaying()
        audioFilePath = "${externalCacheDir?.absolutePath}/audiorecord.3gp"
        mediaRecorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
                isRecording = true
                updateUIAfterStartRecording()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error al preparar la grabación", Toast.LENGTH_SHORT).show()
                releaseMediaRecorder()
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            mediaRecorder?.stop()
            releaseMediaRecorder()
            isRecording = false
            updateUIAfterStopRecording()
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun startPlaying() {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                start()
                this@MainActivity.isPlaying = true
                updateUIAfterStartPlaying()
                setOnCompletionListener { stopPlaying() }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "No se pudo reproducir el archivo", Toast.LENGTH_SHORT).show()
                releaseMediaPlayer()
            }
        }
    }

    private fun stopPlaying() {
        if (isPlaying) {
            releaseMediaPlayer()
            isPlaying = false
            updateUIAfterStopPlaying()
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }



    private fun updateUIAfterStartRecording() {
        startButton.isEnabled = false
        stopButton.isEnabled = true
        playButton.isEnabled = false
        stopPlayButton.isEnabled = false
        Toast.makeText(this, "Grabación iniciada", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIAfterStopRecording() {
        startButton.isEnabled = true
        stopButton.isEnabled = false
        playButton.isEnabled = true
        Toast.makeText(this, "Grabación detenida.", Toast.LENGTH_LONG).show()
    }

    private fun updateUIAfterStartPlaying() {
        playButton.isEnabled = false
        stopPlayButton.isEnabled = true
        startButton.isEnabled = false // No grabar mientras se reproduce
        Toast.makeText(this, "Reproduciendo audio", Toast.LENGTH_SHORT).show()
    }

    private fun updateUIAfterStopPlaying() {
        playButton.isEnabled = true
        stopPlayButton.isEnabled = false
        startButton.isEnabled = true
    }


    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (permissionGranted) startRecording()
                else Toast.makeText(this, "Permiso de grabación denegado", Toast.LENGTH_SHORT).show()
            }
            REQUEST_LOCATION_PERMISSION -> {
                if (permissionGranted) getLocationAndAddress()
                else Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onStop() {
        super.onStop()
        if (isRecording) stopRecording()
        if (isPlaying) stopPlaying()
    }
}

