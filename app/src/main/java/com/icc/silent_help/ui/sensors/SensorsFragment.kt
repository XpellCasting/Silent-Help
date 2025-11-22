package com.icc.silent_help.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.icc.silent_help.R

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

        // Configurar listener para la duración de grabación
        sbRecordingDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvRecordingDurationValue.text = "${progress}s"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar listener para el intervalo de ubicación
        sbLocationInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvLocationIntervalValue.text = "${progress}s"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Inicializar valores de texto
        tvRecordingDurationValue.text = "${sbRecordingDuration.progress}s"
        tvLocationIntervalValue.text = "${sbLocationInterval.progress}s"
    }
}
