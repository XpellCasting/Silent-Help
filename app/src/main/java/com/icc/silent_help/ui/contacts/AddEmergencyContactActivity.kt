package com.icc.silent_help.ui.contacts

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.api.RetrofitClient
import com.icc.silent_help.databinding.ActivityAddEmergencyContactBinding
import com.icc.silent_help.utils.HttpHelper
import com.icc.silent_help.utils.UserPreferences
import org.json.JSONObject
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AddEmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEmergencyContactBinding
    private var isEditMode = false
    private var contactId: String? = null

    // Launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchContactPicker()
        } else {
            Toast.makeText(this, "Permiso denegado para leer contactos", Toast.LENGTH_SHORT).show()
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            val cursor = contentResolver.query(contactUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    // Obtener nombre
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                    
                    // Obtener ID para buscar teléfono
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    
                    // Obtener si tiene número
                    val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhoneNumber = if (hasPhoneIndex >= 0) it.getInt(hasPhoneIndex) else 0

                    if (hasPhoneNumber > 0) {
                        val phonesCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        phonesCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val numberIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                var number = if (numberIndex >= 0) pCursor.getString(numberIndex) else ""
                                
                                // Limpieza básica del número
                                number = number.replace(Regex("[^0-9+]"), "")
                                if (number.startsWith("+56")) {
                                    number = number.removePrefix("+56")
                                } else if (number.startsWith("56") && number.length > 9) {
                                    number = number.removePrefix("56")
                                }
                                
                                binding.etContactName.setText(name)
                                binding.etContactPhone.setText(number)
                            }
                        }
                    } else {
                        Toast.makeText(this, "El contacto seleccionado no tiene número de teléfono", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkPermissionAndPickContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            launchContactPicker()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }


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

        // Configurar botón importar
        binding.btnImportContact.setOnClickListener {
            checkPermissionAndPickContact()
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
            url = "${RetrofitClient.BASE_URL}api/user/emergency-contacts/$userPhone/$contactId",
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
            url = "${RetrofitClient.BASE_URL}api/user/emergency-contacts/$userPhone",
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
