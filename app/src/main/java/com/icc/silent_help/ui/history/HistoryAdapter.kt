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
            Toast.makeText(context, "Reproduciendo audio de la alerta #${currentAlert.id}", Toast.LENGTH_SHORT).show()
        }
        holder.locationButton.setOnClickListener {
            Toast.makeText(context, "Mostrando ubicación de la alerta #${currentAlert.id}", Toast.LENGTH_SHORT).show()
        }
        holder.evidenceButton.setOnClickListener {
            Toast.makeText(context, "Descargando evidencia de la alerta #${currentAlert.id}", Toast.LENGTH_SHORT).show()
        }
    }
}
