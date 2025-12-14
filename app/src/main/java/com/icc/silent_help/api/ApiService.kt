package com.icc.silent_help.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

data class AlertRequest(
    val userId: String,
    val direccion: String,
    val audio_base64: String,
    val startTime: String, // "HH:mm:ss"
    val endTime: String,
    val date: String, // "dd/MM/yyyy"
    val duration: String
)

data class AlertResponse(
    val message: String
)


interface ApiService {
    @POST("alerts")
    fun createAlert(@Body request: AlertRequest): Call<AlertResponse>

    @GET("alerts/{userId}")
    fun getAlerts(@Path("userId") userId: String): Call<List<AlertRequest>>
}
