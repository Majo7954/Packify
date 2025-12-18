package com.ucb.deliveryapp.features.packages.domain.repository

import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package

interface PackageRepository {
    suspend fun createPackage(pkg: Package): Result<String>
    suspend fun updatePackage(pkg: Package): Result<Boolean>
    suspend fun deletePackage(packageId: String): Result<Boolean>
    suspend fun getAllPackages(): Result<List<Package>>
    suspend fun getUserPackages(userId: String): Result<List<Package>>
    suspend fun getPackageById(packageId: String): Result<Package>
    suspend fun trackPackage(trackingNumber: String): Result<Package>
    suspend fun updatePackageStatus(packageId: String, newStatus: String): Result<Boolean>
    suspend fun markAsDelivered(packageId: String): Result<Boolean>
}