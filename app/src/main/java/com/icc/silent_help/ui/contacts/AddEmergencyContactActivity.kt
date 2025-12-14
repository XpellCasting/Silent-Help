package com.icc.silent_help.ui.contacts

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.databinding.ActivityAddEmergencyContactBinding
import com.icc.silent_help.utils.HttpHelper
import com.icc.silent_help.utils.UserPreferences
import org.json.JSONObject

class AddEmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEmergencyContactBinding
    private var isEditMode = false
    private var contactId: String? = null

    companion object {
        const val EXTRA_CONTACT_ID = "contact_id"
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_CONTACT_PHONE = "contact_phone"
        const val EXTRA_CONTACT_RELATIONSHIP = "contact_relationship"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEmergencyContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar si es modo edición
        contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
        isEditMode = contactId != null

        if (isEditMode) {
            loadContactData()
        }

        setupUI()
    }

    private fun loadContactData() {
        binding.etContactName.setText(intent.getStringExtra(EXTRA_CONTACT_NAME))
        binding.etContactPhone.setText(intent.getStringExtra(EXTRA_CONTACT_PHONE))
        binding.etContactRelationship.setText(intent.getStringExtra(EXTRA_CONTACT_RELATIONSHIP))
        binding.btnSaveContact.text = "Actualizar Contacto"
    }

    private fun setupUI() {
        // Configurar botón de volver
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Configurar botón de guardar
        binding.btnSaveContact.setOnClickListener {
            if (isEditMode) {
                updateEmergencyContact()
            } else {
                saveEmergencyContact()
            }
        }
    }

    private fun updateEmergencyContact() {
        val name = binding.etContactName.text.toString().trim()
        val phone = binding.etContactPhone.text.toString().trim()
        val relationship = binding.etContactRelationship.text.toString().trim()

        // Validaciones
        if (name.isEmpty()) {
            binding.etContactName.error = "El nombre es obligatorio"
            return
        }

        if (phone.isEmpty()) {
            binding.etContactPhone.error = "El teléfono es obligatorio"
            return
        }

        // Obtener el teléfono del usuario
        val userPhone = UserPreferences.getUserPhone(this)
        
        if (userPhone.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se encontró información del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón mientras se procesa
        binding.btnSaveContact.isEnabled = false
        binding.btnSaveContact.text = "Actualizando..."

        // Crear objeto JSON con los datos actualizados
        val contactData = JSONObject().apply {
            put("name", name)
            put("phone", phone)
            put("relationship", if (relationship.isEmpty()) "Contacto de Emergencia" else relationship)
        }

        // Enviar al backend
        HttpHelper.put(
            url = "http://192.168.1.12:3000/api/user/emergency-contacts/$userPhone/$contactId",
            data = contactData
        ) { success, response ->
            runOnUiThread {
                binding.btnSaveContact.isEnabled = true
                binding.btnSaveContact.text = "Actualizar Contacto"

                if (success) {
                    try {
                        val jsonResponse = JSONObject(response)
                        
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(
                                this,
                                "✅ Contacto actualizado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            Log.d("AddEmergencyContact", "Contacto actualizado: $name")
                            
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val message = jsonResponse.optString("message", "Error al actualizar contacto")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("AddEmergencyContact", "Error al procesar respuesta: ${e.message}")
                        Toast.makeText(
                            this,
                            "Error al procesar la respuesta del servidor",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("AddEmergencyContact", "Error al actualizar contacto: $response")
                    Toast.makeText(
                        this,
                        "Error al actualizar contacto. Inténtalo de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveEmergencyContact() {
        val name = binding.etContactName.text.toString().trim()
        val phone = binding.etContactPhone.text.toString().trim()
        val relationship = binding.etContactRelationship.text.toString().trim()

        // Validaciones
        if (name.isEmpty()) {
            binding.etContactName.error = "El nombre es obligatorio"
            return
        }

        if (phone.isEmpty()) {
            binding.etContactPhone.error = "El teléfono es obligatorio"
            return
        }

        // Obtener el teléfono del usuario
        val userPhone = UserPreferences.getUserPhone(this)
        
        if (userPhone.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se encontró información del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón mientras se procesa
        binding.btnSaveContact.isEnabled = false
        binding.btnSaveContact.text = "Guardando..."

        // Crear objeto JSON con los datos del contacto
        val contactData = JSONObject().apply {
            put("name", name)
            put("phone", phone)
            put("relationship", if (relationship.isEmpty()) "Contacto de Emergencia" else relationship)
        }

        // Enviar al backend
        HttpHelper.post(
            url = "http://192.168.1.12:3000/api/user/emergency-contacts/$userPhone",
            data = contactData
        ) { success, response ->
            runOnUiThread {
                binding.btnSaveContact.isEnabled = true
                binding.btnSaveContact.text = "Guardar Contacto"

                if (success) {
                    try {
                        val jsonResponse = JSONObject(response)
                        
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(
                                this,
                                "✅ Contacto agregado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            Log.d("AddEmergencyContact", "Contacto agregado: $name")
                            
                            // Volver a la pantalla anterior
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val message = jsonResponse.optString("message", "Error al agregar contacto")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("AddEmergencyContact", "Error al procesar respuesta: ${e.message}")
                        Toast.makeText(
                            this,
                            "Error al procesar la respuesta del servidor",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("AddEmergencyContact", "Error al agregar contacto: $response")
                    Toast.makeText(
                        this,
                        "Error al agregar contacto. Inténtalo de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
