package com.ucb.deliveryapp.features.packages.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository

class GetPackageByIdUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(packageId: String): Result<Package> {
        return packageRepository.getPackageById(packageId)
    }
}
