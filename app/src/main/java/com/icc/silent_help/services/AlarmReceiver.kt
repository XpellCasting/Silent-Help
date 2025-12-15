package com.icc.silent_help.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.icc.silent_help.Actions
import com.icc.silent_help.HudSensores
import com.icc.silent_help.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Actions.TRIGGER_ALARM) {
            context?.let {
                val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "emergency_alarm_channel"
                val channelName = "Emergency Alarm"
                
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
                
                val hudIntent = Intent(it, HudSensores::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    it,
                    0,
                    hudIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(it, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este recurso
                    .setContentTitle("¡Alarma de Emergencia!")
                    .setContentText("Presiona para abrir la pantalla de emergencia.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntent, true)
                    .build()
                
                notificationManager.notify(1, notification)
            }
        }
    }
}