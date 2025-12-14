package com.icc.silent_help.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.icc.silent_help.R
import com.icc.silent_help.utils.UserPreferences

class SensorsFragment : Fragment() {

    private lateinit var sbRecordingDuration: SeekBar
    private lateinit var tvRecordingDurationValue: TextView
    private lateinit var sbLocationInterval: SeekBar
    private lateinit var tvLocationIntervalValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sensors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        sbRecordingDuration = view.findViewById(R.id.sb_recording_duration)
        tvRecordingDurationValue = view.findViewById(R.id.tv_recording_duration_value)
        sbLocationInterval = view.findViewById(R.id.sb_location_interval)
        tvLocationIntervalValue = view.findViewById(R.id.tv_location_interval_value)

        val switchGps = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_gps)
        val switchMic = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_mic)
        val switchProximity = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_proximity)

        // Cargar valores guardados
        val context = requireContext()
        sbRecordingDuration.progress = UserPreferences.getAudioDuration(context)
        switchGps.isChecked = UserPreferences.isGpsEnabled(context)
        switchMic.isChecked = UserPreferences.isMicEnabled(context)
        switchProximity.isChecked = UserPreferences.isProximityEnabled(context)

        tvRecordingDurationValue.text = "${sbRecordingDuration.progress}s"
        tvLocationIntervalValue.text = "${sbLocationInterval.progress}s"

        // Listeners para Switches
        switchGps.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setGpsEnabled(context, isChecked)
        }

        switchMic.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setMicEnabled(context, isChecked)
        }

        switchProximity.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setProximityEnabled(context, isChecked)
        }

        // Listener para SeekBar de Duración
        sbRecordingDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Asegurar mínimo de 10s
                val value = if (progress < 10) 10 else progress
                tvRecordingDurationValue.text = "${value}s"
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val value = if (it.progress < 10) 10 else it.progress
                    if (it.progress < 10) it.progress = 10
                    UserPreferences.setAudioDuration(context, value)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar listener para el intervalo de ubicación (Visual por ahora, o guardar si fuera necesario)
        sbLocationInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvLocationIntervalValue.text = "${progress}s"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
