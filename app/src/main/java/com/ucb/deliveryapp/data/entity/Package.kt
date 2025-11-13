package com.ucb.deliveryapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packages")
data class Package(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val trackingNumber: String,
    val senderName: String,
    val recipientName: String,
    val recipientAddress: String,
    val recipientPhone: String,
    val weight: Double,
    val status: String,
    val priority: String,
    val estimatedDeliveryDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val notes: String? = null,
    val userId: Int
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