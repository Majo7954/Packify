package com.ucb.deliveryapp.features.packages.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository

class UpdatePackageStatusUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(packageId: String, newStatus: String): Result<Boolean> {
        return packageRepository.updatePackageStatus(packageId, newStatus)
    }
}