package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result

class GetPackageByIdUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(packageId: String): Result<Package> {
        return packageRepository.getPackageById(packageId)
    }
}