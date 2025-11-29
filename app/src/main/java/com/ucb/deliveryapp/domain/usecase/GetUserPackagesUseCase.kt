package com.ucb.deliveryapp.domain.usecase

import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.domain.repository.PackageRepository
import com.ucb.deliveryapp.util.Result

class GetUserPackagesUseCase(
    private val packageRepository: PackageRepository
) {
    suspend operator fun invoke(userId: String): Result<List<Package>> {
        return packageRepository.getUserPackages(userId)
    }
}