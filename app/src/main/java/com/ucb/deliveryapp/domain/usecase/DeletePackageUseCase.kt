package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result

class DeletePackageUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(packageId: String): Result<Boolean> {
        return packageRepository.deletePackage(packageId)
    }
}