package com.icc.silent_help.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.databinding.ActivityRegisterStep3Binding
import com.icc.silent_help.ui.home.HomeActivity
import com.icc.silent_help.utils.HttpHelper
import com.icc.silent_help.utils.UserPreferences
import org.json.JSONObject
import android.util.Log

import com.icc.silent_help.api.RetrofitClient

class RegisterStep3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterStep3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterStep3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Datos previos del registro
        val nombre = intent.getStringExtra("nombre") ?: ""
        val telefono = intent.getStringExtra("telefono") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        // 🔹 Acción del botón principal
        binding.btnCompleteSetup.setOnClickListener {
            val nombreEmergencia = binding.emergencyContactName.text.toString().trim()
            val telefonoEmergencia = binding.emergencyContactPhone.text.toString().trim()

            if (nombreEmergencia.isEmpty() || telefonoEmergencia.isEmpty()) {
                Toast.makeText(this, "Completa los datos del contacto de emergencia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 Crear el cuerpo JSON
            val data = JSONObject().apply {
                put("nombre", nombre)
                put("telefono", telefono)
                put("email", email)
                put("contactoEmergencia", JSONObject().apply {
                    put("name", nombreEmergencia)
                    put("phone", telefonoEmergencia)
                })
            }

            Log.d("RegisterStep3", "➡ Enviando datos: $data")

            // 🔹 Llamada HTTP al backend
            HttpHelper.post(
                url = "${RetrofitClient.BASE_URL}api/user/complete-register",
                data = data
            ) { success, response ->
                runOnUiThread {
                    if (success) {
                        // ✅ Extraer el userId del response si existe
                        val userId = try {
                            val jsonResponse = JSONObject(response)
                            if (jsonResponse.has("data")) {
                                val dataObj = jsonResponse.getJSONObject("data")
                                dataObj.optString("userId", null)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("RegisterStep3", "Error extrayendo userId: ${e.message}")
                            null
                        }

                        // ✅ Guardar todos los datos del usuario usando UserPreferences
                        UserPreferences.saveUserData(
                            context = this,
                            userId = userId,
                            name = nombre,
                            phone = telefono,
                            email = email,
                            emergencyContactName = nombreEmergencia,
                            emergencyContactPhone = telefonoEmergencia
                        )

                        Log.d("RegisterStep3", "✅ Datos guardados persistentemente")
                        Toast.makeText(this, "✅ Registro completado con éxito", Toast.LENGTH_LONG).show()
                        
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("RegisterStep3", "❌ Error: $response")
                        Toast.makeText(this, "Error al completar el registro", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
