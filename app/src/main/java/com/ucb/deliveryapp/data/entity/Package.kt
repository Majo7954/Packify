package com.ucb.deliveryapp.data.entity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Package(
    @DocumentId
    val id: String = "",

    @PropertyName("tracking_number")
    val trackingNumber: String = "",

    @PropertyName("sender_name")
    val senderName: String = "",

    @PropertyName("recipient_name")
    val recipientName: String = "",

    @PropertyName("recipient_address")
    val recipientAddress: String = "",

    @PropertyName("recipient_phone")
    val recipientPhone: String = "",

    @PropertyName("weight")
    val weight: Double = 0.0,

    @PropertyName("status")
    val status: String = PackageStatus.PENDING,

    @PropertyName("priority")
    val priority: String = PackagePriority.NORMAL,

    @PropertyName("estimated_delivery_date")
    val estimatedDeliveryDate: Timestamp = Timestamp.now(),

    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),

    @PropertyName("delivered_at")
    val deliveredAt: Timestamp? = null,

    @PropertyName("notes")
    val notes: String? = null,

    @PropertyName("user_id")
    val userId: String = ""
) {
    // Constructor sin parámetros para Firestore
    constructor() : this("", "", "", "", "", "", 0.0, PackageStatus.PENDING,
        PackagePriority.NORMAL, Timestamp.now(), Timestamp.now(), null, null, "")
}

object PackageStatus {
    const val PENDING = "pending"
    const val IN_TRANSIT = "in_transit"
    const val DELIVERED = "delivered"
    const val CANCELLED = "cancelled"
}

object PackagePriority {
    const val NORMAL = "normal"
    const val EXPRESS = "express"
    const val URGENT = "urgent"
}