package com.icc.silent_help.models

import com.google.gson.annotations.SerializedName

data class AlertHistoryItem(
    @SerializedName("_id") val id: String,
    val date: String,
    @SerializedName("startTime") val time: String,
    val status: String,
    val duration: String,
    @SerializedName("direccion") val address: String,
    val audios: List<String>? = null
)
