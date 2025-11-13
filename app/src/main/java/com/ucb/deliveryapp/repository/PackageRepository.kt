package com.ucb.deliveryapp.repository

import com.ucb.deliveryapp.data.db.PackageDao
import com.ucb.deliveryapp.data.entity.Package

class PackageRepository(private val packageDao: PackageDao) {

    suspend fun createPackage(pkg: Package): Long {
        return packageDao.insertPackage(pkg)
    }

    suspend fun updatePackage(pkg: Package) {
        packageDao.updatePackage(pkg)
    }

    suspend fun deletePackage(pkg: Package) {
        packageDao.deletePackage(pkg)
    }

    suspend fun getAllPackages(): List<Package> {
        return packageDao.getAllPackages()
    }

    suspend fun getUserPackages(userId: Int): List<Package> {
        return packageDao.getPackagesByUser(userId)
    }

    suspend fun getPackageById(packageId: Int): Package? {
        return packageDao.getPackageById(packageId)
    }

    suspend fun trackPackage(trackingNumber: String): Package? {
        return packageDao.getPackageByTrackingNumber(trackingNumber)
    }

    suspend fun updatePackageStatus(packageId: Int, newStatus: String) {
        packageDao.updatePackageStatus(packageId, newStatus)
    }

    suspend fun markAsDelivered(packageId: Int) {
        val currentTime = System.currentTimeMillis()
        packageDao.markAsDelivered(packageId, currentTime)
    }
}