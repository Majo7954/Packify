package com.ucb.deliveryapp.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ucb.deliveryapp.core.remoteconfig.RemoteConfigManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteConfigIntegrationTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        RemoteConfigManager.initialize(context)

        Thread.sleep(2000)
    }

    @Test
    fun testRemoteConfigValues() {
        val notificationsEnabled = RemoteConfigManager.isNotificationsEnabled()
        val maintenanceMode = RemoteConfigManager.isMaintenanceMode()
        val requiredVersion = RemoteConfigManager.getRequiredVersion()

        println("Remote Config values:")
        println("- Notifications enabled: $notificationsEnabled")
        println("- Maintenance mode: $maintenanceMode")
        println("- Required version: $requiredVersion")

        val sound = RemoteConfigManager.getNotificationSound()
        assert(sound.isNotEmpty())
    }
}