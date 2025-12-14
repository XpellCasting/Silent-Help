package com.icc.silent_help
data class EmergencyContact(
    val id: String = "",
    val initials: String,
    val name: String,
    val relationship: String,
    val phone: String
)