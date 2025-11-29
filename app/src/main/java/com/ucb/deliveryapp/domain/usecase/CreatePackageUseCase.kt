package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result

class CreatePackageUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(pkg: Package): Result<String> {
        return packageRepository.createPackage(pkg)
    }
}