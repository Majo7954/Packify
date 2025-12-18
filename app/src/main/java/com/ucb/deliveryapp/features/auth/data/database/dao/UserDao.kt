package com.ucb.deliveryapp.features.auth.data.database.dao

import androidx.room.*
import com.ucb.deliveryapp.features.auth.data.database.entity.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearAll()
}
