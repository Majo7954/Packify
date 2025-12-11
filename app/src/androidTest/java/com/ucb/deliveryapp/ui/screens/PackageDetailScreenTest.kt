// PackageDetailScreenTest.kt
package com.ucb.deliveryapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ucb.deliveryapp.ui.screens.packages.PackageDetailScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun packageDetailScreen_loadsWithoutCrash() {
        composeTestRule.setContent {
            PackageDetailScreen(
                navController = rememberNavController(),
                packageId = "test-id-123"
            )
        }

        Thread.sleep(3000)
        // Si no hay crash, pasa
        assert(true)
    }

    @Test
    fun packageDetailScreen_displayScreen() {
        composeTestRule.setContent {
            PackageDetailScreen(
                navController = rememberNavController(),
                packageId = "test-id-123"
            )
        }

        Thread.sleep(3000)
        composeTestRule.onRoot().assertExists()
    }
}