// kotlin+java/com/ucb/deliveryapp/data/entity/Package.kt
package com.ucb.deliveryapp.data.entity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Package(
    @DocumentId
    val id: String = "",
    val trackingNumber: String = "", // CAMBIAR: tracking_number → trackingNumber
    val senderName: String = "", // CAMBIAR: sender_name → senderName
    val recipientName: String = "", // CAMBIAR: recipient_name → recipientName
    val recipientAddress: String = "", // CAMBIAR: recipient_address → recipientAddress
    val recipientPhone: String = "", // CAMBIAR: recipient_phone → recipientPhone
    val weight: Double = 0.0,
    val status: String = PackageStatus.PENDING,
    val priority: String = PackagePriority.NORMAL,
    val estimatedDeliveryDate: Timestamp = Timestamp.now(), // CAMBIAR: estimated_delivery_date → estimatedDeliveryDate
    val createdAt: Timestamp = Timestamp.now(), // CAMBIAR: created_at → createdAt
    val deliveredAt: Timestamp? = null, // CAMBIAR: delivered_at → deliveredAt
    val notes: String? = null,
    val userId: String = "" // CAMBIAR: user_id → userId
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