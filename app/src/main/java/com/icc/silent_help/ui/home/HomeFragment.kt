package com.icc.silent_help.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icc.silent_help.ContactsAdapter
import com.icc.silent_help.EmergencyContact
import com.icc.silent_help.HudSensores
import com.icc.silent_help.R
import com.icc.silent_help.ui.register.RegisterStep1Activity

class HomeFragment : Fragment() {

    private var isSystemArmed = false

    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var btnArmSystem: Button
    private lateinit var btnActivateAlert: Button
    private lateinit var rvEmergencyContacts: RecyclerView
    private lateinit var btnDeactivateSystem: Button
    private lateinit var btnRegisterTemp: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivStatusIcon = view.findViewById(R.id.iv_status_icon)
        tvStatusTitle = view.findViewById(R.id.tv_status_title)
        tvStatusSubtitle = view.findViewById(R.id.tv_status_subtitle)
        btnArmSystem = view.findViewById(R.id.btn_arm_system)
        btnActivateAlert = view.findViewById(R.id.btn_activate_alert)
        rvEmergencyContacts = view.findViewById(R.id.rv_emergency_contacts)
        btnDeactivateSystem = view.findViewById(R.id.btn_deactivate_system)
        btnRegisterTemp = view.findViewById(R.id.btn_register_temp)

        btnArmSystem.setOnClickListener {
            isSystemArmed = true
            updateSystemStatusUI()
        }
        btnDeactivateSystem.setOnClickListener {
            isSystemArmed = false
            updateSystemStatusUI()
        }

        btnActivateAlert.setOnClickListener {
            val intent = Intent(activity, HudSensores::class.java)
            startActivity(intent)
        }

        btnRegisterTemp.setOnClickListener {
            val intent = Intent(activity, RegisterStep1Activity::class.java)
            startActivity(intent)
        }

        updateSystemStatusUI()
        setupContactsRecyclerView()
    }

    private fun updateSystemStatusUI() {
        if (isSystemArmed) {
            ivStatusIcon.setImageResource(R.drawable.ic_shield_on)
            ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            tvStatusTitle.text = "Sistema armado"
            tvStatusSubtitle.text = "Listo para activar alarma de emergencia"

            btnArmSystem.visibility = View.GONE
            btnActivateAlert.visibility = View.VISIBLE
            btnDeactivateSystem.visibility = View.VISIBLE
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_shield_off)
            ivStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            tvStatusTitle.text = "Sistema desarmado"
            tvStatusSubtitle.text = "Activa el sistema para estar protegido"

            btnArmSystem.visibility = View.VISIBLE
            btnActivateAlert.visibility = View.GONE
            btnDeactivateSystem.visibility = View.GONE
        }
    }

    private fun setupContactsRecyclerView() {
        val contacts = listOf(
            EmergencyContact("MG", "María González", "Familia", "+123456789"),
            EmergencyContact("CR", "Dr. Carlos Ruiz", "Doctor", "+098765432"),
            EmergencyContact("AM", "Ana Martínez", "Amiga", "+987654321")
        )

        rvEmergencyContacts.layoutManager = LinearLayoutManager(requireContext())
        rvEmergencyContacts.adapter = ContactsAdapter(contacts)
    }
}
