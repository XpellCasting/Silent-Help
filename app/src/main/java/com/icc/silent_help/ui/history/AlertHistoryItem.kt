package com.icc.silent_help.models


data class AlertHistoryItem(
    val id: String,
    val date: String,
    val time: String,
    val status: String,
    val duration: String,
    val address: String
)
