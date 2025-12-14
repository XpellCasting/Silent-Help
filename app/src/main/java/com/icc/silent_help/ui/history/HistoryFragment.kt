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

        RetrofitClient.instance.getAlertsByUser(userId).enqueue(object : Callback<List<AlertHistoryItem>> {
            override fun onResponse(call: Call<List<AlertHistoryItem>>, response: Response<List<AlertHistoryItem>>) {
                if (response.isSuccessful) {
                    val alerts = response.body()
                    if (alerts != null) {
                        historyAdapter = HistoryAdapter(requireContext(), alerts)
                        recyclerView.adapter = historyAdapter
                    }
                } else {
                    Toast.makeText(context, "Error al cargar historial", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AlertHistoryItem>>, t: Throwable) {
                Toast.makeText(context, "Fallo de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
