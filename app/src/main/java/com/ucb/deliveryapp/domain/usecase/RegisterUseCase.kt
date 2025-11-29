package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.domain.repository.UserRepository
import com.ucb.deliveryapp.util.Result

class RegisterUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Boolean> {
        return userRepository.registerUser(user)
    }
}