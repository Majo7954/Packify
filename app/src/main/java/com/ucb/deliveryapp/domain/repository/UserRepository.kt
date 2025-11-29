package com.ucb.deliveryapp.domain.repository

import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.util.Result

interface UserRepository {
    suspend fun registerUser(user: User): Result<Boolean>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun logout()
    suspend fun isEmailRegistered(email: String): Boolean
    suspend fun getUserById(userId: String): Result<User>
    // En tu UserRepository interface, agrega:
    suspend fun updateUser(userId: String, updatedUser: User): Result<Boolean>
}