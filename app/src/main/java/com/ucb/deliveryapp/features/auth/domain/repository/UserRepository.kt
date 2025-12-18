package com.ucb.deliveryapp.features.auth.domain.repository

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto

interface UserRepository {
    suspend fun registerUser(user: UserDto): Result<Boolean>
    suspend fun login(email: String, password: String): Result<UserDto>
    suspend fun getCurrentUser(): UserDto?
    suspend fun logout()
    suspend fun isEmailRegistered(email: String): Boolean
    suspend fun getUserById(userId: String): Result<UserDto>
    suspend fun updateUser(userId: String, updatedUser: UserDto): Result<Boolean>
}