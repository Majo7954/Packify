package com.ucb.deliveryapp.data.db

import androidx.room.*
import com.ucb.deliveryapp.data.entity.Package

@Dao
interface PackageDao {

    @Insert
    suspend fun insertPackage(pkg: Package): Long

    @Update
    suspend fun updatePackage(pkg: Package)

    @Delete
    suspend fun deletePackage(pkg: Package)

    @Query("SELECT * FROM packages ORDER BY createdAt DESC")
    suspend fun getAllPackages(): List<Package>

    @Query("SELECT * FROM packages WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPackagesByUser(userId: Int): List<Package>

    @Query("SELECT * FROM packages WHERE id = :packageId")
    suspend fun getPackageById(packageId: Int): Package?

    @Query("SELECT * FROM packages WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getPackagesByStatus(status: String): List<Package>

    @Query("SELECT * FROM packages WHERE trackingNumber = :trackingNumber")
    suspend fun getPackageByTrackingNumber(trackingNumber: String): Package?

    @Query("UPDATE packages SET status = :newStatus WHERE id = :packageId")
    suspend fun updatePackageStatus(packageId: Int, newStatus: String)

    @Query("UPDATE packages SET status = 'delivered', deliveredAt = :deliveryTime WHERE id = :packageId")
    suspend fun markAsDelivered(packageId: Int, deliveryTime: Long)

    @Query("SELECT COUNT(*) FROM packages WHERE status = :status")
    suspend fun countPackagesByStatus(status: String): Int
}