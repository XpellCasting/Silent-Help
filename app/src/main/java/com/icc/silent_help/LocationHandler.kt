package com.icc.silent_help

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.io.IOException
import java.util.Locale

// Interfaz para comunicar eventos de ubicación a la Activity
interface LocationHandlerListener {
    fun onLocationFound(address: String, precision: Float)
    fun onLocationError(message: String)
    fun onLocationUpdate(latitude: Double, longitude: Double)
}

class LocationHandler(private val context: Context, private val listener: LocationHandlerListener) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        if (!hasLocationPermission()) {
            listener.onLocationError("Permiso de ubicación no concedido.")
            return
        }

        // Cambiamos lastLocation por getCurrentLocation para forzar una actualización fresca
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null) {
                // Enviar coordenadas iniciales
                listener.onLocationUpdate(location.latitude, location.longitude)
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addressText = if (addresses != null && addresses.isNotEmpty()) {
                        addresses[0].getAddressLine(0) ?: "Dirección no encontrada"
                    } else {
                        "No se encontró la dirección."
                    }
                    listener.onLocationFound(addressText, location.accuracy)
                } catch (e: IOException) {
                    Log.e("GeocoderError", "Servicio no disponible", e)
                    listener.onLocationError("Error al obtener la dirección")
                }
            } else {
                listener.onLocationError("No se pudo obtener la ubicación")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(intervalSeconds: Int) {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalSeconds * 1000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(intervalSeconds * 1000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    listener.onLocationUpdate(location.latitude, location.longitude)
                    
                    // Geocoding en segundo plano
                    Thread {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            // En Android 33+ getFromLocation tiene una versión asíncrona, pero para compatibilidad usamos la síncrona en un hilo
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            
                            val addressText = if (addresses != null && addresses.isNotEmpty()) {
                                addresses[0].getAddressLine(0) ?: "Dirección no encontrada"
                            } else {
                                "No se encontró la dirección."
                            }
                            
                            // Volver al hilo principal para actualizar UI
                            android.os.Handler(Looper.getMainLooper()).post {
                                listener.onLocationFound(addressText, location.accuracy)
                            }
                        } catch (e: Exception) {
                            Log.e("LocationHandler", "Error en Geocoding: ${e.message}")
                            android.os.Handler(Looper.getMainLooper()).post {
                                listener.onLocationError("Error de red al obtener dirección")
                            }
                        }
                    }.start()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}
