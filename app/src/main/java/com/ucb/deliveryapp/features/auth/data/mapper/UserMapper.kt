package com.ucb.deliveryapp.features.auth.data.mapper

import com.ucb.deliveryapp.features.auth.data.database.entity.UserEntity
import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.auth.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    email = email,
    createdAtMillis = createdAt.toDate().time,
    updatedAtMillis = updatedAt.toDate().time
)

fun User.toEntity(password: String? = null): UserEntity = UserEntity(
    id = id,
    username = username,
    email = email,
    password = password,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun UserEntity.toDomain(): User = User(
    id = id,
    username = username,
    email = email,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
