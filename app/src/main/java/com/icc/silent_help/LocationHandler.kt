package com.icc.silent_help

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale

// Interfaz para comunicar eventos de ubicación a la Activity
interface LocationHandlerListener {
    fun onLocationFound(address: String, precision: Float)
    fun onLocationError(message: String)
}

class LocationHandler(private val context: Context, private val listener: LocationHandlerListener) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        if (!hasLocationPermission()) {
            listener.onLocationError("Permiso de ubicación no concedido.")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
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
}
