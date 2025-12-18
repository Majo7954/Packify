package com.ucb.deliveryapp.features.auth.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class UserDto(
    @DocumentId
    val id: String = "",

    @PropertyName("username")
    val username: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("password")
    val password: String = "",

    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.Companion.now(),

    @PropertyName("updated_at")
    val updatedAt: Timestamp = Timestamp.Companion.now()
) {
    constructor() : this("", "", "", "", Timestamp.Companion.now(), Timestamp.Companion.now())
}