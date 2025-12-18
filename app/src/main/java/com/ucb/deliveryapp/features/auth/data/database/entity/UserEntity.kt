package com.ucb.deliveryapp.features.auth.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,

    val username: String,
    val email: String,

    val password: String? = null,

    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
