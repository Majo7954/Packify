package com.ucb.deliveryapp.features.auth.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.auth.domain.repository.UserRepository

class UpdateUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, updatedUser: UserDto): Result<Boolean> {
        return repository.updateUser(userId, updatedUser)
    }
}
