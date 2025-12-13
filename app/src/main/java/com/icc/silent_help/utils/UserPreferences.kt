package com.icc.silent_help.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase de utilidad para manejar las preferencias del usuario de forma centralizada
 * Garantiza la persistencia de datos durante toda la vida de la aplicación
 */
object UserPreferences {

    private const val PREFS_NAME = "UserPrefs"
    
    // Claves para las preferencias
    private const val KEY_IS_REGISTERED = "isRegistered"
    private const val KEY_USER_ID = "userId"
    private const val KEY_USER_NAME = "userName"
    private const val KEY_USER_PHONE = "userPhone"
    private const val KEY_USER_EMAIL = "userEmail"
    private const val KEY_EMERGENCY_CONTACT_NAME = "emergencyContactName"
    private const val KEY_EMERGENCY_CONTACT_PHONE = "emergencyContactPhone"
    private const val KEY_REGISTRATION_DATE = "registrationDate"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Verifica si el usuario está registrado
     */
    fun isUserRegistered(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_REGISTERED, false)
    }

    /**
     * Guarda todos los datos del usuario después del registro completo
     */
    fun saveUserData(
        context: Context,
        userId: String?,
        name: String,
        phone: String,
        email: String,
        emergencyContactName: String,
        emergencyContactPhone: String
    ) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_IS_REGISTERED, true)
            userId?.let { putString(KEY_USER_ID, it) }
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_EMERGENCY_CONTACT_NAME, emergencyContactName)
            putString(KEY_EMERGENCY_CONTACT_PHONE, emergencyContactPhone)
            putLong(KEY_REGISTRATION_DATE, System.currentTimeMillis())
            apply() // Usar apply() para guardar de forma asíncrona
        }
    }

    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_ID, null)
    }

    /**
     * Obtiene el nombre del usuario
     */
    fun getUserName(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_NAME, null)
    }

    /**
     * Obtiene el teléfono del usuario
     */
    fun getUserPhone(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_PHONE, null)
    }

    /**
     * Obtiene el email del usuario
     */
    fun getUserEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_EMAIL, null)
    }

    /**
     * Obtiene el nombre del contacto de emergencia
     */
    fun getEmergencyContactName(context: Context): String? {
        return getPreferences(context).getString(KEY_EMERGENCY_CONTACT_NAME, null)
    }

    /**
     * Obtiene el teléfono del contacto de emergencia
     */
    fun getEmergencyContactPhone(context: Context): String? {
        return getPreferences(context).getString(KEY_EMERGENCY_CONTACT_PHONE, null)
    }

    /**
     * Obtiene la fecha de registro (en milisegundos)
     */
    fun getRegistrationDate(context: Context): Long {
        return getPreferences(context).getLong(KEY_REGISTRATION_DATE, 0)
    }

    /**
     * Limpia todos los datos del usuario (útil para cerrar sesión o resetear)
     */
    fun clearUserData(context: Context) {
        getPreferences(context).edit().clear().apply()
    }

    /**
     * Actualiza el nombre del usuario
     */
    fun updateUserName(context: Context, name: String) {
        getPreferences(context).edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * Actualiza el email del usuario
     */
    fun updateUserEmail(context: Context, email: String) {
        getPreferences(context).edit().putString(KEY_USER_EMAIL, email).apply()
    }

    /**
     * Actualiza el contacto de emergencia
     */
    fun updateEmergencyContact(context: Context, name: String, phone: String) {
        getPreferences(context).edit().apply {
            putString(KEY_EMERGENCY_CONTACT_NAME, name)
            putString(KEY_EMERGENCY_CONTACT_PHONE, phone)
            apply()
        }
    }
}
