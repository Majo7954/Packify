package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result

class UpdatePackageStatusUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(packageId: String, newStatus: String): Result<Boolean> {
        return packageRepository.updatePackageStatus(packageId, newStatus)
    }
}