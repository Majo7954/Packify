package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.domain.repository.UserRepository
import com.ucb.deliveryapp.util.Result

class LoginUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return userRepository.login(email, password)
    }
}