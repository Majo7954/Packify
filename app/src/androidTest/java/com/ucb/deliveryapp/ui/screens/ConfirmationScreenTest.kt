// ConfirmationScreenTest.kt
package com.ucb.deliveryapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ucb.deliveryapp.ui.navigation.Routes
import com.ucb.deliveryapp.ui.screens.home.ConfirmationScreen
import com.ucb.deliveryapp.ui.screens.packages.PackageListScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfirmationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirmationScreen_displaySuccessMessage() {
        composeTestRule.setContent {
            ConfirmationScreen(
                navController = rememberNavController(),
                onNavigateToPackages = {}
            )
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Tu pedido ha sido realizado con éxito.")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Tu pedido ha sido realizado con éxito.")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun confirmationScreen_displayTrackOrderText() {
        composeTestRule.setContent {
            ConfirmationScreen(
                navController = rememberNavController(),
                onNavigateToPackages = {}
            )
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Realice el seguimiento:")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Realice el seguimiento:")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun confirmationScreen_displayViewPackagesButton() {
        composeTestRule.setContent {
            ConfirmationScreen(
                navController = rememberNavController(),
                onNavigateToPackages = {}
            )
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Ver mis paquetes")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Ver mis paquetes")
            .assertExists()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun confirmationScreen_clickViewPackagesButton() {
        composeTestRule.setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "confirmation"
            ) {
                composable("confirmation") {
                    ConfirmationScreen(
                        navController = navController,
                        onNavigateToPackages = {
                            navController.navigate("packages")
                        }
                    )
                }
                composable("packages") {
                    PackageListScreen(navController = navController)
                }
            }
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Ver mis paquetes")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Ver mis paquetes")
            .performClick()

        Thread.sleep(1000)
    }

    @Test
    fun confirmationScreen_displayCheckIcon() {
        composeTestRule.setContent {
            ConfirmationScreen(
                navController = rememberNavController(),
                onNavigateToPackages = {}
            )
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithContentDescription("Confirmado")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Confirmado")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun confirmationScreen_completeUIStructure() {
        composeTestRule.setContent {
            ConfirmationScreen(
                navController = rememberNavController(),
                onNavigateToPackages = {}
            )
        }

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Tu pedido ha sido realizado con éxito.")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Tu pedido ha sido realizado con éxito.")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Realice el seguimiento:")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Ver mis paquetes")
            .assertExists()
            .assertIsDisplayed()
    }
}