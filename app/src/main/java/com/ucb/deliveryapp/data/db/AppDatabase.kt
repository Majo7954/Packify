package com.ucb.deliveryapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.data.entity.Package

@Database(
    entities = [User::class, Package::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun packageDao(): PackageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS packages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trackingNumber TEXT NOT NULL,
                        senderName TEXT NOT NULL,
                        recipientName TEXT NOT NULL,
                        recipientAddress TEXT NOT NULL,
                        recipientPhone TEXT NOT NULL,
                        weight REAL NOT NULL,
                        status TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        estimatedDeliveryDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        deliveredAt INTEGER,
                        notes TEXT,
                        userId INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "packify_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}