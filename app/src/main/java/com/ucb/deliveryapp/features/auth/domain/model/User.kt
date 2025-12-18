package com.ucb.deliveryapp.features.auth.domain.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)
