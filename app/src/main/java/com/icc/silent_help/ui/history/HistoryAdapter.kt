package com.icc.silent_help.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.icc.silent_help.R
import com.icc.silent_help.models.AlertHistoryItem
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HistoryAdapter(
    private val context: Context,
    private val alertList: List<AlertHistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.tv_alert_date)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_alert_time)
        val statusTextView: TextView = itemView.findViewById(R.id.tv_alert_status)
        val durationTextView: TextView = itemView.findViewById(R.id.tv_alert_duration)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_alert_address)
        val audioButton: MaterialButton = itemView.findViewById(R.id.btn_play_audio)
        val locationButton: MaterialButton = itemView.findViewById(R.id.btn_view_location)
        val evidenceButton: MaterialButton = itemView.findViewById(R.id.btn_download_evidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_alert_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return alertList.size
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val currentAlert = alertList[position]

        holder.dateTextView.text = currentAlert.date
        holder.timeTextView.text = currentAlert.time
        holder.statusTextView.text = currentAlert.status
        holder.durationTextView.text = currentAlert.duration
        holder.addressTextView.text = currentAlert.address

        if (currentAlert.status.equals("Resuelto", ignoreCase = true)) {
            holder.statusTextView.setBackgroundResource(R.drawable.status_badge_resolved)
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            holder.statusTextView.setBackgroundResource(R.drawable.status_badge_cancelled)
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }


        holder.audioButton.setOnClickListener {
            val audios = currentAlert.audios

            if (!audios.isNullOrEmpty()) {
                playMultipleAudios(audios)
            } else {
                Toast.makeText(context, "Audio no disponible", Toast.LENGTH_SHORT).show()
            }
        }
        holder.locationButton.setOnClickListener {
            Toast.makeText(context, "Mostrando ubicación de la alerta #${currentAlert.id}", Toast.LENGTH_SHORT).show()
        }
        holder.evidenceButton.setOnClickListener {
            Toast.makeText(context, "Descargando evidencia de la alerta #${currentAlert.id}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMultipleAudios(audios: List<String>) {
        try {
            val audioFiles = ArrayList<File>()
            
            // 1. Decodificar todos los audios a archivos temporales
            for ((index, base64String) in audios.withIndex()) {
                if (base64String.isNotEmpty()) {
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val tempFile = File.createTempFile("temp_audio_$index", ".3gp", context.cacheDir)
                    val fos = FileOutputStream(tempFile)
                    fos.write(decodedBytes)
                    fos.close()
                    audioFiles.add(tempFile)
                }
            }
            
            if (audioFiles.isEmpty()) {
                Toast.makeText(context, "No se encontraron audios válidos", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(context, "Reproduciendo ${audioFiles.size} clips...", Toast.LENGTH_SHORT).show()
            playAudioQueue(audioFiles, 0)

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error al preparar audios", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudioQueue(files: List<File>, currentIndex: Int) {
        if (currentIndex >= files.size) {
            Toast.makeText(context, "Reproducción finalizada", Toast.LENGTH_SHORT).show()
            // Limpieza final de archivos
            files.forEach { it.delete() }
            return
        }

        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(files[currentIndex].absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                it.release()
                // Reproducir el siguiente
                playAudioQueue(files, currentIndex + 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla uno, intentamos el siguiente
            playAudioQueue(files, currentIndex + 1)
        }
    }

    private fun playBase64Audio(base64String: String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val tempFile = File.createTempFile("temp_audio", ".3gp", context.cacheDir)
            val fos = FileOutputStream(tempFile)
            fos.write(decodedBytes)
            fos.close()

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            
            Toast.makeText(context, "Reproduciendo audio...", Toast.LENGTH_SHORT).show()

            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error al reproducir audio", Toast.LENGTH_SHORT).show()
        }
    }
}
