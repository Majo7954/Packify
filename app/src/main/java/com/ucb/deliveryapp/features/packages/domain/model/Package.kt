package com.ucb.deliveryapp.features.packages.domain.model

data class Package(
    val id: String = "",
    val trackingNumber: String = "",
    val senderName: String = "",
    val recipientName: String = "",
    val recipientAddress: String = "",
    val recipientPhone: String = "",
    val weight: Double = 0.0,
    val status: String = PackageStatus.PENDING,
    val priority: String = PackagePriority.NORMAL,
    val estimatedDeliveryAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val deliveredAtMillis: Long? = null,
    val notes: String? = null,
    val userId: String = ""
)

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
