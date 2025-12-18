package com.ucb.deliveryapp.features.packages.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class PackageDto(
    @DocumentId
    val id: String = "",

    @PropertyName("trackingNumber")
    val trackingNumber: String = "",

    @PropertyName("senderName")
    val senderName: String = "",

    @PropertyName("recipientName")
    val recipientName: String = "",

    @PropertyName("recipientAddress")
    val recipientAddress: String = "",

    @PropertyName("recipientPhone")
    val recipientPhone: String = "",

    @PropertyName("weight")
    val weight: Double = 0.0,

    @PropertyName("status")
    val status: String = "",

    @PropertyName("priority")
    val priority: String = "",

    @PropertyName("estimatedDeliveryDate")
    val estimatedDeliveryDate: Timestamp? = null,

    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @PropertyName("deliveredAt")
    val deliveredAt: Timestamp? = null,

    @PropertyName("notes")
    val notes: String? = null,

    @PropertyName("userId")
    val userId: String = ""
)