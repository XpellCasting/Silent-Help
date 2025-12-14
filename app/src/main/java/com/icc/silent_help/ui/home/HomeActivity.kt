package com.icc.silent_help.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.icc.silent_help.ui.register.RegisterStep1Activity
import com.icc.silent_help.utils.UserPreferences
import android.util.Log
import java.util.concurrent.Executor

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Flag to avoid re-authentication in the same session
    private var isUnlocked = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (audioGranted && locationGranted) {
            Log.d("HomeActivity", "Permisos concedidos")
        } else {
            Toast.makeText(this, "Se requieren permisos para el funcionamiento completo", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solicitar permisos al iniciar la app
        checkAndRequestPermissions()

        // ✅ Verificar que el usuario esté registrado antes de continuar
        if (!UserPreferences.isUserRegistered(this)) {
            // Si no está registrado, redirigir al registro
            val intent = Intent(this, RegisterStep1Activity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // ✅ Log de datos del usuario (para debug)
        Log.d("HomeActivity", "Usuario: ${UserPreferences.getUserName(this)}")
        Log.d("HomeActivity", "Teléfono: ${UserPreferences.getUserPhone(this)}")
        Log.d("HomeActivity", "Email: ${UserPreferences.getUserEmail(this)}")
        
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
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    override fun onStop() {
        super.onStop()
        // If the app is no longer visible, require re-authentication next time.
        isUnlocked = false
    }
}
