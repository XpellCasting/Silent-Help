package com.icc.silent_help
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    // Variable para rastrear el estado del sistema.
    private var isSystemArmed = false

    // Declaración de vistas para poder acceder a ellas.
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var btnArmSystem: Button
    private lateinit var btnActivateAlert: Button
    private lateinit var rvEmergencyContacts: RecyclerView

    private lateinit var btnDeactivateSystem: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar las vistas.
        ivStatusIcon = findViewById(R.id.iv_status_icon)
        tvStatusTitle = findViewById(R.id.tv_status_title)
        tvStatusSubtitle = findViewById(R.id.tv_status_subtitle)
        btnArmSystem = findViewById(R.id.btn_arm_system)
        btnActivateAlert = findViewById(R.id.btn_activate_alert)
        rvEmergencyContacts = findViewById(R.id.rv_emergency_contacts)
        btnDeactivateSystem = findViewById(R.id.btn_deactivate_system)
        // Configurar el comportamiento de los botones.
        btnArmSystem.setOnClickListener {
            isSystemArmed = true
            updateSystemStatusUI()
        }
        btnDeactivateSystem.setOnClickListener {
            isSystemArmed = false
            updateSystemStatusUI()
        }

        // El botón para desactivar el sistema estaría dentro del de "Activar Alarma"
        // o en un menú diferente. Por simplicidad, lo manejamos aquí.
        // Podrías agregar un nuevo botón "Desactivar" que aparece cuando está armado.

        // Inicializar la UI con el estado por defecto (desarmado).
        updateSystemStatusUI()

        // Cargar y mostrar los contactos de emergencia.
        setupContactsRecyclerView()
    }

    private fun updateSystemStatusUI() {
        if (isSystemArmed) {
            ivStatusIcon.setImageResource(R.drawable.ic_shield_on)
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            tvStatusTitle.text = "Sistema armado"
            tvStatusSubtitle.text = "Listo para activar alarma de emergencia"

            btnArmSystem.visibility = View.GONE
            btnActivateAlert.visibility = View.VISIBLE
            btnDeactivateSystem.visibility = View.VISIBLE
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_shield_off)
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            tvStatusTitle.text = "Sistema desarmado"
            tvStatusSubtitle.text = "Activa el sistema para estar protegido"

            btnArmSystem.visibility = View.VISIBLE
            btnActivateAlert.visibility = View.GONE
            btnDeactivateSystem.visibility = View.GONE
        }
    }

    private fun setupContactsRecyclerView() {
        // Usamos datos de ejemplo ("mock data") como se describe en tus requisitos (RF-01).
        val contacts = listOf(
            EmergencyContact("MG", "María González", "Familia", "+123456789"),
            EmergencyContact("CR", "Dr. Carlos Ruiz", "Doctor", "+098765432"),
            EmergencyContact("AM", "Ana Martínez", "Amiga", "+987654321")
        )

        rvEmergencyContacts.layoutManager = LinearLayoutManager(this)
        rvEmergencyContacts.adapter = ContactsAdapter(contacts)
    }
}