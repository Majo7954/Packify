package com.ucb.deliveryapp.features.packages.data.mapper

import com.google.firebase.Timestamp
import com.ucb.deliveryapp.features.packages.data.database.entity.PackageEntity
import com.ucb.deliveryapp.features.packages.data.remote.dto.PackageDto
import com.ucb.deliveryapp.features.packages.domain.model.Package
import java.util.Date

fun PackageDto.toDomain(): Package = Package(
    id = id,
    trackingNumber = trackingNumber,
    senderName = senderName,
    recipientName = recipientName,
    recipientAddress = recipientAddress,
    recipientPhone = recipientPhone,
    weight = weight,
    status = status,
    priority = priority,
    estimatedDeliveryAtMillis = estimatedDeliveryDate?.toDate()?.time ?: 0L,
    createdAtMillis = createdAt?.toDate()?.time ?: 0L,
    deliveredAtMillis = deliveredAt?.toDate()?.time,
    notes = notes,
    userId = userId
)

fun Package.toDto(): PackageDto = PackageDto(
    id = id,
    trackingNumber = trackingNumber,
    senderName = senderName,
    recipientName = recipientName,
    recipientAddress = recipientAddress,
    recipientPhone = recipientPhone,
    weight = weight,
    status = status,
    priority = priority,
    estimatedDeliveryDate = if (estimatedDeliveryAtMillis == 0L) null else Timestamp(Date(estimatedDeliveryAtMillis)),
    createdAt = if (createdAtMillis == 0L) null else Timestamp(Date(createdAtMillis)),
    deliveredAt = deliveredAtMillis?.let { Timestamp(Date(it)) },
    notes = notes,
    userId = userId
)

fun PackageEntity.toDomain(): Package = Package(
    id = id,
    trackingNumber = trackingNumber,
    senderName = senderName,
    recipientName = recipientName,
    recipientAddress = recipientAddress,
    recipientPhone = recipientPhone,
    weight = weight,
    status = status,
    priority = priority,
    estimatedDeliveryAtMillis = estimatedDeliveryAtMillis,
    createdAtMillis = createdAtMillis,
    deliveredAtMillis = deliveredAtMillis,
    notes = notes,
    userId = userId
)

fun Package.toEntity(
    dirty: Boolean = false,
    updatedAtMillis: Long = System.currentTimeMillis()
): PackageEntity = PackageEntity(
    id = id,
    trackingNumber = trackingNumber,
    senderName = senderName,
    recipientName = recipientName,
    recipientAddress = recipientAddress,
    recipientPhone = recipientPhone,
    weight = weight,
    status = status,
    priority = priority,
    estimatedDeliveryAtMillis = estimatedDeliveryAtMillis,
    createdAtMillis = createdAtMillis,
    deliveredAtMillis = deliveredAtMillis,
    notes = notes,
    userId = userId,
    dirty = dirty,
    updatedAtMillis = updatedAtMillis
)
