package com.icc.silent_help.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

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

data class AlertResponse(
    val message: String,
    val alert: Alert?
)

data class AudioRequest(
    val audio_base64: String
)

data class EndAlertRequest(
    val endTime: String,
    val duration: String
)

interface ApiService {
    @POST("alerts/create")
    fun createAlert(@Body request: AlertRequest): Call<AlertResponse>

    @GET("alerts/{userId}")
    fun getAlerts(@Path("userId") userId: String): Call<List<AlertRequest>>

    @retrofit2.http.PUT("alerts/{alertId}/audio")
    fun addAudio(@Path("alertId") alertId: String, @Body request: AudioRequest): Call<AlertResponse>

    @retrofit2.http.PUT("alerts/{alertId}/end")
    fun endAlert(@Path("alertId") alertId: String, @Body request: EndAlertRequest): Call<AlertResponse>
}
