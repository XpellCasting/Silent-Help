package com.icc.silent_help.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.icc.silent_help.HudSensores
import com.icc.silent_help.utils.UserPreferences

class EmergencyKeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private val longPressDuration = 3000L // 3 segundos

    private val longPressRunnable = Runnable { triggerEmergencyAlarm() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        cancelLongPress()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        // Si el sistema no está armado, no hacemos nada
        if (!UserPreferences.isSystemArmed(this)) {
            return super.onKeyEvent(event)
        }

        event ?: return super.onKeyEvent(event)

        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolumeUpPressed = (action == KeyEvent.ACTION_DOWN)
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeDownPressed = (action == KeyEvent.ACTION_DOWN)
        }

        if (isVolumeUpPressed && isVolumeDownPressed) {
            // Si ambos botones están presionados, iniciamos el temporizador
            handler.postDelayed(longPressRunnable, longPressDuration)
        } else {
            // Si alguno se suelta, cancelamos el temporizador
            cancelLongPress()
        }

        return super.onKeyEvent(event)
    }

    private fun cancelLongPress() {
        handler.removeCallbacks(longPressRunnable)
    }

    private fun triggerEmergencyAlarm() {
        val intent = Intent(this, HudSensores::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        
        // Una vez activada, reseteamos el estado para evitar reactivaciones
        isVolumeUpPressed = false
        isVolumeDownPressed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelLongPress()
    }
}