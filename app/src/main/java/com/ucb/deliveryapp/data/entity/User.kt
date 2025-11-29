package com.ucb.deliveryapp.data.entity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val id: String = "",

    @PropertyName("username")
    val username: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("password")
    val password: String = "",

    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),

    @PropertyName("updated_at")
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Constructor sin parámetros para Firestore
    constructor() : this("", "", "", "", Timestamp.now(), Timestamp.now())
}