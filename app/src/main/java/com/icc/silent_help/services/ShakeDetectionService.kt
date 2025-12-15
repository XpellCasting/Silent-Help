package com.icc.silent_help.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.icc.silent_help.Actions
import com.icc.silent_help.utils.UserPreferences
import kotlin.math.abs

class ShakeDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastX: Float = 0.0f
    private var lastY: Float = 0.0f
    private var lastZ: Float = 0.0f
    private var lastUpdate: Long = 0

    // Este valor se puede ajustar para cambiar la sensibilidad de la detección
    private val shakeThreshold = 1200

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 100) {
                val diffTime = (currentTime - lastUpdate)
                lastUpdate = currentTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                if (speed > shakeThreshold) {
                    onShake()
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    private fun onShake() {
        if (UserPreferences.isSystemArmed(this) && UserPreferences.isShakeToAlarmEnabled(this)) {
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                action = Actions.TRIGGER_ALARM
            }
            sendBroadcast(intent)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario para esta implementación
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}