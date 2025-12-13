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
    private lateinit var switchBiometrics: SwitchMaterial

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

        val prefs = requireActivity().getSharedPreferences("BiometricPrefs", Context.MODE_PRIVATE)

        switchBiometrics = view.findViewById(R.id.switch_biometrics)
        executor = ContextCompat.getMainExecutor(requireContext())

        switchBiometrics.isChecked = prefs.getBoolean("use_biometrics", false)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error de autenticación: $errString", Toast.LENGTH_SHORT).show()
                    switchBiometrics.isChecked = false
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "¡Autenticación exitosa! La protección biométrica está activada.", Toast.LENGTH_SHORT).show()
                    prefs.edit().putBoolean("use_biometrics", true).apply()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Autenticación fallida", Toast.LENGTH_SHORT).show()
                    switchBiometrics.isChecked = false
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación biométrica")
            .setSubtitle("Usa tus datos biométricos para activar la protección")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        switchBiometrics.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBiometricSupportAndAuthenticate()
            } else {
                prefs.edit().putBoolean("use_biometrics", false).apply()
                Toast.makeText(requireContext(), "Protección biométrica desactivada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(requireContext(), "Tu dispositivo no es compatible con la autenticación biométrica", Toast.LENGTH_SHORT).show()
                switchBiometrics.isChecked = false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(), "Los sensores biométricos no están disponibles actualmente", Toast.LENGTH_SHORT).show()
                switchBiometrics.isChecked = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(requireContext(), "No tienes datos biométricos registrados. Por favor, configúralos en los ajustes de tu dispositivo.", Toast.LENGTH_LONG).show()
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_WEAK)
                }
                startActivity(enrollIntent)
                switchBiometrics.isChecked = false
            }
            else -> {
                Toast.makeText(requireContext(), "Error desconocido", Toast.LENGTH_SHORT).show()
                switchBiometrics.isChecked = false
            }
        }
    }
}
