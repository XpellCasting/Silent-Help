package com.icc.silent_help

data class Alerta(
    val id: String,
    val userId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val audioUrl: String,
    val status: String = "En curso"
)
