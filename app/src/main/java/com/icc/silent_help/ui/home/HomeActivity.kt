package com.icc.silent_help.ui.home

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.icc.silent_help.R
import com.icc.silent_help.ui.biometrics.BiometricsFragment
import com.icc.silent_help.ui.history.HistoryFragment
import com.icc.silent_help.ui.sensors.SensorsFragment
import java.util.concurrent.Executor

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Flag to avoid re-authentication in the same session
    private var isUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.navigation_home -> selectedFragment = HomeFragment()
                R.id.navigation_history -> selectedFragment = HistoryFragment()
                R.id.navigation_biometrics -> selectedFragment = BiometricsFragment()
                R.id.navigation_sensors -> selectedFragment = SensorsFragment()
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Load the initial fragment
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_home
        }

        setupBiometrics()
    }

    override fun onStart() {
        super.onStart()
        // Check if we need to authenticate when the app becomes visible
        val prefs = getSharedPreferences("BiometricPrefs", Context.MODE_PRIVATE)
        val useBiometrics = prefs.getBoolean("use_biometrics", false)

        if (useBiometrics && !isUnlocked) {
            authenticateApp()
        }
    }

    private fun setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If the user cancels or there is an error, close the app
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        finish()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Authentication successful, allow access
                    isUnlocked = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // The fingerprint is not recognized, the prompt remains
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación requerida")
            .setSubtitle("Usa tus datos biométricos para acceder a la aplicación")
            .setNegativeButtonText("Salir")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    }

    private fun authenticateApp() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    override fun onStop() {
        super.onStop()
        // If the app is no longer visible, require re-authentication next time.
        isUnlocked = false
    }
}
