package com.icc.silent_help.ui.biometrics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.icc.silent_help.R
import java.util.concurrent.Executor

class BiometricsFragment : Fragment() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var switchFingerprint: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_biometrics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get SharedPreferences
        val prefs = requireActivity().getSharedPreferences("BiometricPrefs", Context.MODE_PRIVATE)

        switchFingerprint = view.findViewById(R.id.switch_fingerprint)
        executor = ContextCompat.getMainExecutor(requireContext())

        // Load the saved preference and set the switch state
        switchFingerprint.isChecked = prefs.getBoolean("use_biometrics", false)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error de autenticación: $errString", Toast.LENGTH_SHORT).show()
                    // Revert the switch if authentication fails
                    switchFingerprint.isChecked = false
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "Autenticación exitosa! La protección está activada.", Toast.LENGTH_SHORT).show()
                    // Save the preference
                    prefs.edit().putBoolean("use_biometrics", true).apply()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Autenticación fallida", Toast.LENGTH_SHORT).show()
                    // Revert the switch
                    switchFingerprint.isChecked = false
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación por huella dactilar")
            .setSubtitle("Usa tu huella para activar la protección")
            .setNegativeButtonText("Cancelar")
            .build()

        switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If the user wants to enable, start the auth process
                checkBiometricSupportAndAuthenticate()
            } else {
                // If the user wants to disable, just save the preference
                prefs.edit().putBoolean("use_biometrics", false).apply()
                Toast.makeText(requireContext(), "Protección por huella desactivada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(requireContext(), "El dispositivo no tiene sensor de huella", Toast.LENGTH_SHORT).show()
                switchFingerprint.isChecked = false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(), "El sensor de huella no está disponible actualmente", Toast.LENGTH_SHORT).show()
                switchFingerprint.isChecked = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(requireContext(), "No tienes huellas registradas. Por favor, configura una.", Toast.LENGTH_LONG).show()
                // Redirect the user to security settings to enroll a fingerprint
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG)
                }
                startActivity(enrollIntent)
                switchFingerprint.isChecked = false
            }
            else -> {
                Toast.makeText(requireContext(), "Error desconocido", Toast.LENGTH_SHORT).show()
                switchFingerprint.isChecked = false
            }
        }
    }
}
