package com.icc.silent_help

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.widget.Toast
import java.io.IOException

// Interfaz para comunicar eventos de audio a la Activity
interface AudioHandlerListener {
    fun onRecordingStarted()
    fun onRecordingStopped(filePath: String)
    fun onPlayingStarted()
    fun onPlayingStopped()
    fun onError(message: String)
}

class AudioHandler(private val context: Context, private val listener: AudioHandlerListener) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    private var isPlaying = false

    init {
        // Inicializar con un valor por defecto, aunque startRecording debería recibir el nombre
        audioFilePath = "${context.externalCacheDir?.absolutePath}/audiorecord_init.3gp"
    }

    fun startRecording(customFileName: String? = null) {
        if (isRecording) return
        if (isPlaying) stopPlaying()

        val fileName = customFileName ?: "audio_${System.currentTimeMillis()}.3gp"
        audioFilePath = "${context.externalCacheDir?.absolutePath}/$fileName"

        mediaRecorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
                isRecording = true
                listener.onRecordingStarted()
            } catch (e: IOException) {
                e.printStackTrace()
                listener.onError("Error al preparar la grabación")
                releaseMediaRecorder()
            }
        }
    }

    fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
            } catch (e: RuntimeException) {
                // Manejar crash si se para inmediatamente después de empezar
            }
            releaseMediaRecorder()
            isRecording = false
            audioFilePath?.let { listener.onRecordingStopped(it) }
        }
    }

    fun startPlaying() {
        if (isPlaying) return

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                start()
                this@AudioHandler.isPlaying = true
                listener.onPlayingStarted()
                setOnCompletionListener { stopPlaying() }
            } catch (e: IOException) {
                e.printStackTrace()
                listener.onError("No se pudo reproducir el archivo de audio")
                releaseMediaPlayer()
            }
        }
    }

    fun stopPlaying() {
        if (isPlaying) {
            releaseMediaPlayer()
            isPlaying = false
            listener.onPlayingStopped()
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Método para llamar desde el onStop de la Activity
    fun releaseResources() {
        if (isRecording) stopRecording()
        if (isPlaying) stopPlaying()
    }
}
