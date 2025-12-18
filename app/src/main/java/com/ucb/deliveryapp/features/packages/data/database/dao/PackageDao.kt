package com.ucb.deliveryapp.features.packages.data.database.dao

import androidx.room.*
import com.ucb.deliveryapp.features.packages.data.database.entity.PackageEntity

@Dao
interface PackageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pkg: PackageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(pkgs: List<PackageEntity>)

    @Query("SELECT * FROM packages WHERE userId = :userId ORDER BY createdAtMillis DESC")
    suspend fun getByUser(userId: String): List<PackageEntity>

    @Query("SELECT * FROM packages ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<PackageEntity>

    @Query("SELECT * FROM packages WHERE id = :packageId LIMIT 1")
    suspend fun getById(packageId: String): PackageEntity?

    @Query("SELECT * FROM packages WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getByTrackingNumber(trackingNumber: String): PackageEntity?

    @Query("UPDATE packages SET status = :newStatus, updatedAtMillis = :updatedAt WHERE id = :packageId")
    suspend fun updateStatus(packageId: String, newStatus: String, updatedAt: Long)

    @Query("UPDATE packages SET status = 'delivered', deliveredAtMillis = :deliveryTime, updatedAtMillis = :updatedAt WHERE id = :packageId")
    suspend fun markAsDelivered(packageId: String, deliveryTime: Long, updatedAt: Long)

    @Query("SELECT * FROM packages WHERE dirty = 1")
    suspend fun getDirty(): List<PackageEntity>

    @Query("UPDATE packages SET dirty = :dirty WHERE id = :packageId")
    suspend fun setDirty(packageId: String, dirty: Boolean)

    @Query("DELETE FROM packages WHERE id = :packageId")
    suspend fun deleteById(packageId: String)
}
