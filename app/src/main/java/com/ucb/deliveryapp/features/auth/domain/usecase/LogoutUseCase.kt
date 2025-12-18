package com.ucb.deliveryapp.features.auth.domain.usecase

import com.ucb.deliveryapp.features.auth.domain.repository.UserRepository

class LogoutUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}
