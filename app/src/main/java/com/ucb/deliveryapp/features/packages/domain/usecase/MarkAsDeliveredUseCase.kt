package com.ucb.deliveryapp.features.packages.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository

class MarkAsDeliveredUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(packageId: String): Result<Boolean> {
        return packageRepository.markAsDelivered(packageId)
    }
}