package com.ucb.deliveryapp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ucb.deliveryapp.features.auth.data.database.dao.UserDao
import com.ucb.deliveryapp.features.auth.data.database.entity.UserEntity
import com.ucb.deliveryapp.features.packages.data.database.dao.PackageDao
import com.ucb.deliveryapp.features.packages.data.database.entity.PackageEntity

@Database(
    entities = [UserEntity::class, PackageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun packageDao(): PackageDao
}