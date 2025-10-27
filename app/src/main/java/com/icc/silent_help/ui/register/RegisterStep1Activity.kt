package com.icc.silent_help.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.databinding.ActivityRegisterStep1Binding
import com.icc.silent_help.utils.HttpHelper
import org.json.JSONObject
import android.util.Log

class RegisterStep1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterStep1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Inicializar binding
        binding = ActivityRegisterStep1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Acción del botón continuar
        binding.btnContinue.setOnClickListener {
            val nombreUsuario = binding.userFullName.text.toString().trim()
            val telefonoUsuario = binding.userPhoneNumber.text.toString().trim()
            val emailUsuario = binding.userEmail.text.toString().trim()
            val terminosCheck = binding.termCheckBox.isChecked

            // 🔹 Validación de campos
            if (nombreUsuario.isBlank() || telefonoUsuario.isBlank() || !terminosCheck) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 Crear el cuerpo JSON para la solicitud
            val data = JSONObject().apply {
                put("nombre", nombreUsuario)
                put("telefono", telefonoUsuario)
                put("email", emailUsuario)
            }

            // 🔹 Enviar la solicitud HTTP para generar el código SMS
            HttpHelper.post(
                url = "http://10.0.2.2:3000/api/user/send-code", // ⚠️ Cambia por tu endpoint real
                data = data
            ) { success, response ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(
                            this,
                            "Código enviado al número $telefonoUsuario",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ Pasar a la siguiente pantalla si todo salió bien
                        val intent = Intent(this, RegisterStep2Activity::class.java).apply {
                            putExtra("nombre", nombreUsuario)
                            putExtra("telefono", telefonoUsuario)
                            putExtra("email", emailUsuario)
                        }
                        startActivity(intent)
                    } else {
                        Log.e("RegisterStep1Activity", "Error al enviar el código: $response")

                    }
                }
            }
        }
    }
}
