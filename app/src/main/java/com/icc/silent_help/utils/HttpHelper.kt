package com.icc.silent_help.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

/**
 * Clase auxiliar para manejar solicitudes HTTP
 */
object HttpHelper {

    private val client = OkHttpClient()

    /**
     * Envía un objeto JSON al backend mediante POST
     * @param url La URL del endpoint
     * @param data Los datos JSON a enviar
     * @param onResult Callback con la respuesta del servidor o error
     */
    fun post(
        url: String,
        data: JSONObject,
        onResult: (success: Boolean, response: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = RequestBody.create(mediaType, data.toString())

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    onResult(true, responseBody)
                } else {
                    onResult(false, "Error HTTP ${response.code}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Realiza una solicitud GET al backend
     * @param url La URL del endpoint
     * @param onResult Callback con la respuesta del servidor o error
     */
    fun get(
        url: String,
        onResult: (success: Boolean, response: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    onResult(true, responseBody)
                } else {
                    onResult(false, "Error HTTP ${response.code}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Envía un objeto JSON al backend mediante PUT
     * @param url La URL del endpoint
     * @param data Los datos JSON a enviar
     * @param onResult Callback con la respuesta del servidor o error
     */
    fun put(
        url: String,
        data: JSONObject,
        onResult: (success: Boolean, response: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = RequestBody.create(mediaType, data.toString())

                val request = Request.Builder()
                    .url(url)
                    .put(body)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    onResult(true, responseBody)
                } else {
                    onResult(false, "Error HTTP ${response.code}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /**
     * Realiza una solicitud DELETE al backend
     * @param url La URL del endpoint
     * @param onResult Callback con la respuesta del servidor o error
     */
    fun delete(
        url: String,
        onResult: (success: Boolean, response: String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .delete()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    onResult(true, responseBody)
                } else {
                    onResult(false, "Error HTTP ${response.code}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}
