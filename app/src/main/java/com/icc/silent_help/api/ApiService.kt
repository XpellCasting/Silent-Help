package com.icc.silent_help.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.icc.silent_help.models.AlertHistoryItem

// --- Data Classes (Modelos de Petición/Respuesta) ---

data class AlertRequest(
    val userId: String,
    val direccion: String,
    val startTime: String, // "HH:mm:ss"
    val endTime: String,
    val date: String, // "dd/MM/yyyy"
    val duration: String,
    val audios: List<String>? = null
)

data class Alert(
    val _id: String
)

data class CreateAlertResponse(
    val message: String,
    val alert: Alert?
)

data class AlertResponse(
    val message: String,
    val alert: Alert?,
    val notifiedContacts: List<String>? = null
)

data class AudioRequest(
    val audio_base64: String
)

data class EndAlertRequest(
    val endTime: String,
    val duration: String
)

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val direccion: String? = null
)

// --- Definición de la Interfaz API ---

interface ApiService {

    @POST("api/alerts/create")
    fun createAlert(@Body request: AlertRequest): Call<AlertResponse>

    @GET("api/alerts/{userId}")
    fun getAlertsByUser(@Path("userId") userId: String): Call<List<AlertHistoryItem>>

    @PUT("api/alerts/{alertId}/audio")
    fun addAudioToAlert(@Path("alertId") alertId: String, @Body request: AudioRequest): Call<AlertResponse>

    @PUT("api/alerts/{alertId}/end")
    fun endAlert(@Path("alertId") alertId: String, @Body request: EndAlertRequest): Call<Void>

    @PUT("api/alerts/{alertId}/location")
    fun updateLocation(@Path("alertId") alertId: String, @Body request: LocationUpdateRequest): Call<Void>
}