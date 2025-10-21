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

        // Datos de ejemplo
        val alertHistoryList = getDummyData()

        // Crear y asignar el adapter
        historyAdapter = HistoryAdapter(requireContext(), alertHistoryList)
        recyclerView.adapter = historyAdapter
    }

    private fun getDummyData(): List<AlertHistoryItem> {
        return listOf(
            AlertHistoryItem(
                id = "001",
                date = "15 Nov 2025",
                time = "14:30",
                status = "Resuelto",
                duration = "5 min",
                address = "Calle Principal 123, Ciudad"
            ),
            AlertHistoryItem(
                id = "002",
                date = "14 Nov 2025",
                time = "09:15",
                status = "Cancelado",
                duration = "1 min",
                address = "Avenida Secundaria 456, Pueblo"
            ),
            AlertHistoryItem(
                id = "003",
                date = "12 Nov 2025",
                time = "21:45",
                status = "Resuelto",
                duration = "12 min",
                address = "Bulevar del Centro 789, Metrópolis"
            )
        )
    }
}
