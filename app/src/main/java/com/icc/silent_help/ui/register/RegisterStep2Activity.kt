package com.icc.silent_help.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.icc.silent_help.databinding.ActivityRegisterStep2Binding
import com.icc.silent_help.utils.HttpHelper
import org.json.JSONObject
import android.util.Log
import kotlin.math.log

import com.icc.silent_help.api.RetrofitClient

class RegisterStep2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterStep2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Inicializar binding y establecer la vista raíz
        binding = ActivityRegisterStep2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Recuperar datos desde la Activity anterior
        val nombre = intent.getStringExtra("nombre") ?: ""
        val telefono = intent.getStringExtra("telefono") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        // ✅ Mostrar número en la interfaz
        binding.phoneNumberText.text =
            if (telefono.isNotBlank()) telefono else "Número no disponible"


        // ✅ Acción del botón “Verificar código”
        binding.btnVerifyCode.setOnClickListener {
            val codigo = binding.vericationCode.text.toString().trim()

            if (codigo.isEmpty()) {
                Toast.makeText(
                    this,
                    "Por favor ingresa el código de verificación",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // ✅ Enviar el código al backend para verificación
            verificarCodigoConBackend(telefono, codigo) { verificado ->

                if (verificado) {
                    // ✅ Si es correcto, pasar a la siguiente Activity
                    val intent = Intent(this, RegisterStep3Activity::class.java).apply {
                        putExtra("nombre", nombre)
                        putExtra("telefono", telefono)
                        putExtra("email", email)
                        putExtra("codigo", codigo)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Código incorrecto o expirado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }



    /**
     * 🔹 Envía el código ingresado al backend para validarlo
     */
    private fun verificarCodigoConBackend(
        telefono: String,
        codigo: String,
        callback: (Boolean) -> Unit
    ) {
        val data = JSONObject().apply {
            put("telefono", telefono)
            put("codigo", codigo)
        }

        Log.d("DEBUG", data.toString())

        // 🔹 Llamada HTTP al backend para verificar código
        HttpHelper.post(
            url = "${RetrofitClient.BASE_URL}api/user/verify-code",
            data = data
        ) { success, response ->
            runOnUiThread {
                if (success) {
                    // Aquí podrías analizar la respuesta JSON si tu backend devuelve algo como { valid: true }
                    Toast.makeText(this, "Código verificado correctamente", Toast.LENGTH_SHORT).show()
                    callback(true)
                } else {
                    Toast.makeText(this, "Error al verificar código: $response", Toast.LENGTH_LONG).show()
                    callback(false)
                }
            }
        }
    }
}
