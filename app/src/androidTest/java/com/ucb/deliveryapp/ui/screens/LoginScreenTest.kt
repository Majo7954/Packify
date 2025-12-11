// LoginScreenTest.kt - VERSIÓN DEFINITIVA
package com.ucb.deliveryapp.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ucb.deliveryapp.ui.screens.login.LoginScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displayAllElements() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {}
            )
        }

        // Verificar elementos principales
        composeTestRule.onNodeWithText("Inicio de sesión").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsDisplayed()
    }

    @Test
    fun loginScreen_fillEmailField() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {}
            )
        }

        // Escribir en campo de email
        composeTestRule.onNodeWithText("Escribe aquí el correo electrónico")
            .performTextInput("test@ucb.edu.bo")
    }

    @Test
    fun loginScreen_fillPasswordField() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {}
            )
        }

        // Escribir en campo de contraseña
        composeTestRule.onNodeWithText("Ingresa aquí tu contraseña")
            .performTextInput("mypassword")
    }

    @Test
    fun loginScreen_navigateToRegister() {
        var navigationCalled = false

        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = { navigationCalled = true },
                onLoginSuccess = {}
            )
        }

        // CORRECCIÓN: Buscar por el texto COMPLETO "¿No tienes cuenta? Regístrate aquí"
        // porque es un solo texto con buildAnnotatedString
        composeTestRule.onNodeWithText("¿No tienes cuenta? Regístrate aquí")
            .assertExists("Debe existir el texto de registro")
            .performClick()

        assert(navigationCalled)
    }

    @Test
    fun loginScreen_loginButtonEnabled() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {}
            )
        }

        // Verificar que el botón existe y está habilitado
        composeTestRule.onNodeWithText("Iniciar Sesión")
            .assertExists()
            .assertIsEnabled()
    }

    @Test
    fun loginScreen_privacyPolicyLinkExists() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {}
            )
        }

        // Verificar que el enlace a política de privacidad existe
        composeTestRule.onNodeWithText("Términos y Política de Privacidad")
            .assertExists()
    }

    @Test
    fun loginScreen_clickLoginButton() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToRegister = {},
                onLoginSuccess = {}
            )
        }

        // Hacer click en el botón de login
        composeTestRule.onNodeWithText("Iniciar Sesión")
            .performClick()
    }
}