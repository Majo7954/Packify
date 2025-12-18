// RegisterScreenTest.kt
package com.ucb.deliveryapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ucb.deliveryapp.features.auth.presentation.RegisterScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registerScreen_displayAllElements() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegisterSuccess = {},
                onNavigateToLogin = {}
            )
        }

        // Verificar elementos principales
        composeTestRule.onNodeWithText("Registro del usuario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registrarse").assertIsDisplayed()
    }

    @Test
    fun registerScreen_fillAllFields() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegisterSuccess = {},
                onNavigateToLogin = {}
            )
        }

        // Llenar todos los campos
        composeTestRule.onNodeWithText("Ingresa el nombre del usuario")
            .performTextInput("Usuario Test")

        composeTestRule.onNodeWithText("Ingresa el correo electrónico")
            .performTextInput("test@ucb.edu.bo")

        composeTestRule.onNodeWithText("Ingresa tu contraseña")
            .performTextInput("password123")

        composeTestRule.onNodeWithText("Confirma tu contraseña")
            .performTextInput("password123")
    }

    @Test
    fun registerScreen_clickRegisterButton() {
        composeTestRule.setContent {
            RegisterScreen(
                onRegisterSuccess = {},
                onNavigateToLogin = {}
            )
        }

        // Click en botón de registro
        composeTestRule.onNodeWithText("Registrarse")
            .performClick()
    }
}