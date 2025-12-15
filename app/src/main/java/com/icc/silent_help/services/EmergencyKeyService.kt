package com.icc.silent_help.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.icc.silent_help.HudSensores

class EmergencyKeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private val pressTimeout = 500L // 500 ms para presionar ambos botones

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesitamos procesar otros eventos de accesibilidad
    }

    override fun onInterrupt() {
        // El servicio fue interrumpido
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)

        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                isVolumeUpPressed = true
                checkCombination()
                // Reset after a timeout to avoid sticky presses
                handler.postDelayed({ isVolumeUpPressed = false }, pressTimeout)
            } else {
                isVolumeUpPressed = false
            }
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (action == KeyEvent.ACTION_DOWN) {
                isVolumeDownPressed = true
                checkCombination()
                // Reset after a timeout
                handler.postDelayed({ isVolumeDownPressed = false }, pressTimeout)
            } else {
                isVolumeDownPressed = false
            }
        }

        // No consumimos el evento, para que el volumen siga funcionando normalmente
        return super.onKeyEvent(event)
    }

    private fun checkCombination() {
        if (isVolumeUpPressed && isVolumeDownPressed) {
            // ¡Combinación detectada!
            triggerEmergencyAlarm()

            // Reseteamos los estados para evitar múltiples activaciones
            isVolumeUpPressed = false
            isVolumeDownPressed = false
        }
    }

    private fun triggerEmergencyAlarm() {
        val intent = Intent(this, HudSensores::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}