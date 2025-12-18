package com.ucb.deliveryapp.core.remoteconfig

import android.content.Context
import com.google.firebase.ktx.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {
    private lateinit var remoteConfig: FirebaseRemoteConfig

    private val defaultConfigs = mapOf(
        "notifications_enabled" to true,
        "notification_sound" to "default",
        "notification_vibration" to true,
        "in_app_notifications" to true,
        "fetch_interval_minutes" to 60L,
        "maintenance_mode" to false,
        "app_version_required" to "1.0.0",
        "force_update" to false,
        "feature_packages_enabled" to true,
        "feature_profile_enabled" to true,
        "debug_logs" to false,
        "map_provider" to "mapbox"
    )

    fun initialize(context: Context) {
        remoteConfig = Firebase.remoteConfig

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (BuildConfig.DEBUG) 0 else 3600
            )
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(defaultConfigs)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Remote Config cargado")
                } else {
                    println("Remote Config error: ${task.exception}")
                }
            }
    }

    suspend fun forceFetch(): Boolean {
        return try {
            remoteConfig.fetch(0).await()
            remoteConfig.activate().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isNotificationsEnabled(): Boolean =
        remoteConfig.getBoolean("notifications_enabled")

    fun getNotificationSound(): String =
        remoteConfig.getString("notification_sound")

    fun isVibrationEnabled(): Boolean =
        remoteConfig.getBoolean("notification_vibration")

    fun isInAppNotificationsEnabled(): Boolean =
        remoteConfig.getBoolean("in_app_notifications")

    fun isMaintenanceMode(): Boolean =
        remoteConfig.getBoolean("maintenance_mode")

    fun getRequiredVersion(): String =
        remoteConfig.getString("app_version_required")

    fun isForceUpdate(): Boolean =
        remoteConfig.getBoolean("force_update")
}