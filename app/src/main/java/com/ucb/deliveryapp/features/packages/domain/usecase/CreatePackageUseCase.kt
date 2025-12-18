package com.ucb.deliveryapp.features.packages.domain.usecase

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.repository.PackageRepository

class CreatePackageUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(pkg: Package): Result<String> {
        return packageRepository.createPackage(pkg)
    }
}