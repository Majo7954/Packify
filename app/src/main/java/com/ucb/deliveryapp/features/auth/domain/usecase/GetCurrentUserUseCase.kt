package com.ucb.deliveryapp.features.auth.domain.usecase

import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.auth.domain.repository.UserRepository

class GetCurrentUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): UserDto? {
        return repository.getCurrentUser()
    }
}
