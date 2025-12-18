package com.ucb.deliveryapp.features.packages.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey
    val id: String,

    val trackingNumber: String,
    val senderName: String,
    val recipientName: String,
    val recipientAddress: String,
    val recipientPhone: String,
    val weight: Double,
    val status: String,
    val priority: String,

    val estimatedDeliveryAtMillis: Long,
    val createdAtMillis: Long,
    val deliveredAtMillis: Long?,

    val notes: String?,
    val userId: String,

    val dirty: Boolean = false,
    val updatedAtMillis: Long = 0L
)
