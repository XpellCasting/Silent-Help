package com.icc.silent_help.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icc.silent_help.R
import com.icc.silent_help.models.AlertHistoryItem
import com.icc.silent_help.api.AlertRequest
import com.icc.silent_help.api.RetrofitClient
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.icc.silent_help.utils.UserPreferences

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout para este fragmento
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        recyclerView = view.findViewById(R.id.rv_alert_history)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Cargar historial desde la API
        loadAlertHistory()
    }

    private fun loadAlertHistory() {
        val userId = UserPreferences.getUserId(requireContext()) ?: "USUARIO_DESCONOCIDO"

        RetrofitClient.instance.getAlerts(userId).enqueue(object : Callback<List<AlertRequest>> {
            override fun onResponse(call: Call<List<AlertRequest>>, response: Response<List<AlertRequest>>) {
                if (response.isSuccessful) {
                    val alerts = response.body()
                    if (alerts != null) {
                        val historyItems = alerts.map { mapToHistoryItem(it) }
                        historyAdapter = HistoryAdapter(requireContext(), historyItems)
                        recyclerView.adapter = historyAdapter
                    }
                } else {
                    Toast.makeText(context, "Error al cargar historial", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AlertRequest>>, t: Throwable) {
                Toast.makeText(context, "Fallo de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mapToHistoryItem(request: AlertRequest): AlertHistoryItem {
        return AlertHistoryItem(
            id = request.userId, // O usar un ID único generado si viene del backend
            date = request.date,
            time = request.startTime, // Asumiendo que startTime es "HH:mm:ss"
            status = "Resuelto", // O mapear status real si viene del backend
            duration = request.duration,
            address = request.direccion,
            audioBase64 = request.audio_base64
        )
    }
}
