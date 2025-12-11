// PackageListScreenTest.kt
package com.ucb.deliveryapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ucb.deliveryapp.ui.screens.packages.PackageListScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun packageListScreen_loadsWithoutCrash() {
        composeTestRule.setContent {
            PackageListScreen(navController = rememberNavController())
        }

        Thread.sleep(3000)
        assert(true)
    }

    @Test
    fun packageListScreen_displayScreen() {
        composeTestRule.setContent {
            PackageListScreen(navController = rememberNavController())
        }

        Thread.sleep(3000)
        composeTestRule.onRoot().assertExists()
    }
}