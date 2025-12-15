package com.icc.silent_help.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icc.silent_help.ContactsAdapter
import com.icc.silent_help.EmergencyContact
import com.icc.silent_help.HudSensores
import com.icc.silent_help.R
import com.icc.silent_help.services.ShakeDetectionService
import com.icc.silent_help.ui.contacts.AddEmergencyContactActivity
import com.icc.silent_help.api.RetrofitClient
import com.icc.silent_help.utils.HttpHelper
import com.icc.silent_help.utils.UserPreferences
import org.json.JSONObject

class HomeFragment : Fragment() {

    private var isSystemArmed = false

    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var btnArmSystem: Button
    private lateinit var btnActivateAlert: Button
    private lateinit var rvEmergencyContacts: RecyclerView
    private lateinit var btnDeactivateSystem: Button
    private lateinit var btnAddContact: Button

    // Launcher para agregar contactos
    private val addContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recargar la lista de contactos
            loadEmergencyContactsFromDatabase()
        }
    }

    // Launcher para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        if (audioGranted && locationGranted && notificationsGranted) {
            Log.d("HomeFragment", "Permisos concedidos")
        } else {
            Toast.makeText(requireContext(), "Se requieren permisos para el funcionamiento completo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Solicitar permisos al iniciar
        checkAndRequestPermissions()

        ivStatusIcon = view.findViewById(R.id.iv_status_icon)
        tvStatusTitle = view.findViewById(R.id.tv_status_title)
        tvStatusSubtitle = view.findViewById(R.id.tv_status_subtitle)
        btnArmSystem = view.findViewById(R.id.btn_arm_system)
        btnActivateAlert = view.findViewById(R.id.btn_activate_alert)
        rvEmergencyContacts = view.findViewById(R.id.rv_emergency_contacts)
        btnDeactivateSystem = view.findViewById(R.id.btn_deactivate_system)
        btnAddContact = view.findViewById(R.id.btn_add_contact)

        // Cargar el estado del sistema desde las preferencias
        isSystemArmed = UserPreferences.isSystemArmed(requireContext())
        if (isSystemArmed) {
            startShakeDetection()
        }

        btnArmSystem.setOnClickListener {
            isSystemArmed = true
            UserPreferences.setSystemArmed(requireContext(), true)
            updateSystemStatusUI()
            startShakeDetection()
        }
        btnDeactivateSystem.setOnClickListener {
            isSystemArmed = false
            UserPreferences.setSystemArmed(requireContext(), false)
            updateSystemStatusUI()
            stopShakeDetection()
        }

        btnActivateAlert.setOnClickListener {
            val intent = Intent(activity, HudSensores::class.java)
            startActivity(intent)
        }

        btnAddContact.setOnClickListener {
            val intent = Intent(activity, AddEmergencyContactActivity::class.java)
            addContactLauncher.launch(intent)
        }

        updateSystemStatusUI()
        loadEmergencyContactsFromDatabase()
    }

    private fun startShakeDetection() {
        val intent = Intent(activity, ShakeDetectionService::class.java)
        activity?.startService(intent)
    }

    private fun stopShakeDetection() {
        val intent = Intent(activity, ShakeDetectionService::class.java)
        activity?.stopService(intent)
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

    /**
     * Carga los contactos de emergencia desde la base de datos
     */
    private fun loadEmergencyContactsFromDatabase() {
        val userPhone = UserPreferences.getUserPhone(requireContext())
        
        if (userPhone.isNullOrEmpty()) {
            Log.e("HomeFragment", "No se encontró el teléfono del usuario")
            setupContactsRecyclerView(emptyList())
            return
        }

        // Llamada HTTP al backend para obtener los contactos de emergencia
        HttpHelper.get(
            url = "${RetrofitClient.BASE_URL}api/user/emergency-contacts/$userPhone"
        ) { success, response ->
            if (!isAdded) return@get
            requireActivity().runOnUiThread {
                if (success) {
                    try {
                        val jsonResponse = JSONObject(response)
                        val contacts = mutableListOf<EmergencyContact>()
                        
                        if (jsonResponse.getBoolean("success")) {
                            val contactsArray = jsonResponse.getJSONArray("contacts")
                            
                            for (i in 0 until contactsArray.length()) {
                                val contactObj = contactsArray.getJSONObject(i)
                                val id = contactObj.getString("_id")
                                val name = contactObj.getString("name")
                                val phone = contactObj.getString("phone")
                                val relationship = contactObj.optString("relationship", "Contacto de Emergencia")
                                
                                // Generar iniciales
                                val initials = name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                
                                contacts.add(
                                    EmergencyContact(
                                        id = id,
                                        initials = initials,
                                        name = name,
                                        relationship = relationship,
                                        phone = phone
                                    )
                                )
                            }
                            
                            Log.d("HomeFragment", "✅ ${contacts.size} contactos cargados desde la base de datos")
                            setupContactsRecyclerView(contacts)
                        } else {
                            Log.w("HomeFragment", "No se encontraron contactos de emergencia")
                            setupContactsRecyclerView(emptyList())
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error al procesar contactos: ${e.message}")
                        setupContactsRecyclerView(emptyList())
                        Toast.makeText(
                            requireContext(),
                            "Error al cargar contactos de emergencia",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("HomeFragment", "Error al obtener contactos: $response")
                    setupContactsRecyclerView(emptyList())
                }
            }
        }
    }

    /**
     * Configura el RecyclerView con los contactos
     */
    private fun setupContactsRecyclerView(contacts: List<EmergencyContact>) {
        rvEmergencyContacts.layoutManager = LinearLayoutManager(requireContext())
        rvEmergencyContacts.adapter = ContactsAdapter(
            contacts = contacts,
            onEditClick = { contact -> editContact(contact) },
            onDeleteClick = { contact -> confirmDeleteContact(contact) }
        )
        
        if (contacts.isEmpty()) {
            // Opcional: Mostrar un mensaje cuando no hay contactos
            Log.w("HomeFragment", "No hay contactos de emergencia registrados")
        }
    }

    /**
     * Abre la actividad para editar un contacto
     */
    private fun editContact(contact: EmergencyContact) {
        val intent = Intent(activity, AddEmergencyContactActivity::class.java).apply {
            putExtra(AddEmergencyContactActivity.EXTRA_CONTACT_ID, contact.id)
            putExtra(AddEmergencyContactActivity.EXTRA_CONTACT_NAME, contact.name)
            putExtra(AddEmergencyContactActivity.EXTRA_CONTACT_PHONE, contact.phone)
            putExtra(AddEmergencyContactActivity.EXTRA_CONTACT_RELATIONSHIP, contact.relationship)
        }
        addContactLauncher.launch(intent)
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar un contacto
     */
    private fun confirmDeleteContact(contact: EmergencyContact) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar contacto")
            .setMessage("¿Estás seguro de que deseas eliminar a ${contact.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteContact(contact)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Elimina un contacto de emergencia
     */
    private fun deleteContact(contact: EmergencyContact) {
        val userPhone = UserPreferences.getUserPhone(requireContext())
        
        if (userPhone.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: No se encontró información del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        HttpHelper.delete(
            url = "${RetrofitClient.BASE_URL}api/user/emergency-contacts/$userPhone/${contact.id}"
        ) { success, response ->
            if (!isAdded) return@delete
            requireActivity().runOnUiThread {
                if (success) {
                    try {
                        val jsonResponse = JSONObject(response)
                        
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(
                                requireContext(),
                                "✅ Contacto eliminado",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Recargar la lista
                            loadEmergencyContactsFromDatabase()
                        } else {
                            val message = jsonResponse.optString("message", "Error al eliminar contacto")
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error al procesar respuesta: ${e.message}")
                        Toast.makeText(
                            requireContext(),
                            "Error al eliminar contacto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("HomeFragment", "Error al eliminar contacto: $response")
                    Toast.makeText(
                        requireContext(),
                        "Error al eliminar contacto",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
}