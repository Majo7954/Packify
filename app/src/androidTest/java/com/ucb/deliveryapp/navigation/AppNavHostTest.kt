// AppNavHostTest.kt
package com.ucb.deliveryapp.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ucb.deliveryapp.ui.navigation.AppNavHost
import com.ucb.deliveryapp.ui.navigation.Routes
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        composeTestRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext())
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavHost(navController = navController)
        }
    }

    @Test
    fun appNavHost_verifyStartDestination() {
        composeTestRule.waitForIdle()
        val route = navController.currentBackStackEntry?.destination?.route
        assertEquals("Start destination should be ${Routes.LOGIN}", Routes.LOGIN, route)
    }

    @Test
    fun appNavHost_navigateToHome() {
        // Navegar a HOME
        navController.navigate(Routes.HOME)

        composeTestRule.waitForIdle()

        val currentRoute = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should navigate to ${Routes.HOME}", Routes.HOME, currentRoute)
    }

    @Test
    fun appNavHost_navigateToRegister_andBack() {
        // Navegar a REGISTER
        navController.navigate(Routes.REGISTER)
        composeTestRule.waitForIdle()

        // Verificar que estamos en REGISTER
        var currentRoute = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should be at ${Routes.REGISTER}", Routes.REGISTER, currentRoute)

        // Regresar a LOGIN
        val canGoBack = navController.popBackStack()
        composeTestRule.waitForIdle()

        assertTrue("Should be able to go back", canGoBack)
        currentRoute = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should return to ${Routes.LOGIN}", Routes.LOGIN, currentRoute)
    }

    @Test
    fun appNavHost_navigateThroughMainScreens() {
        // Flujo de navegación
        navController.navigate(Routes.HOME)
        composeTestRule.waitForIdle()

        navController.navigate(Routes.MENU)
        composeTestRule.waitForIdle()

        navController.navigate(Routes.PACKAGES)
        composeTestRule.waitForIdle()

        navController.navigate(Routes.CREATE_PACKAGE)
        composeTestRule.waitForIdle()

        navController.navigate(Routes.CONFIRMATION)
        composeTestRule.waitForIdle()

        val currentRoute = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should be at ${Routes.CONFIRMATION}", Routes.CONFIRMATION, currentRoute)
    }

    @Test
    fun appNavHost_navigateToPackageDetail_withParameter() {
        val testPackageId = "12345"

        // Ir primero a PACKAGES
        navController.navigate(Routes.PACKAGES)
        composeTestRule.waitForIdle()

        // Navegar a package_detail con parámetro
        navController.navigate("package_detail/$testPackageId")
        composeTestRule.waitForIdle()

        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val packageId = navController.currentBackStackEntry?.arguments?.getString("packageId")

        assertTrue("Should be at package_detail route",
            currentRoute?.contains("package_detail") == true)
        assertEquals("Package ID should be $testPackageId", testPackageId, packageId)
    }

    @Test
    fun appNavHost_registerSuccess_clearBackStack() {
        // Navegar a REGISTER
        navController.navigate(Routes.REGISTER)
        composeTestRule.waitForIdle()

        // Simular registro exitoso: navegar a HOME con clear back stack
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
        composeTestRule.waitForIdle()

        // Verificar que estamos en HOME
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should be at HOME after register", Routes.HOME, currentRoute)

        // Intentar volver atrás - no debería funcionar porque el back stack fue limpiado
        val canGoBack = navController.popBackStack()
        assertFalse("Should not be able to go back to LOGIN", canGoBack)

        // Verificar que seguimos en HOME
        val routeAfterPop = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should still be at HOME", Routes.HOME, routeAfterPop)
    }

    @Test
    fun appNavHost_loginSuccess_clearBackStack() {
        // Ya estamos en LOGIN (start destination)
        composeTestRule.waitForIdle()

        // Simular login exitoso: navegar a HOME con clear back stack
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
        composeTestRule.waitForIdle()

        // Verificar que estamos en HOME
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        assertEquals("Should be at HOME after login", Routes.HOME, currentRoute)

        // Intentar volver - no debería funcionar
        val canGoBack = navController.popBackStack()
        assertFalse("Should not be able to go back to LOGIN", canGoBack)
    }

    @Test
    fun appNavHost_testAllRoutes() {
        // Test simple para verificar que todas las rutas están definidas
        assertEquals("login", Routes.LOGIN)
        assertEquals("register", Routes.REGISTER)
        assertEquals("home", Routes.HOME)
        assertEquals("menu", Routes.MENU)
        assertEquals("support", Routes.SUPPORT)
        assertEquals("profile", Routes.PROFILE)
        assertEquals("packages", Routes.PACKAGES)
        assertEquals("create_package", Routes.CREATE_PACKAGE)
        assertEquals("confirmation", Routes.CONFIRMATION)
    }

    @Test
    fun appNavHost_testBackNavigation() {
        // Navegar a varias pantallas y verificar navegación hacia atrás
        navController.navigate(Routes.HOME)
        composeTestRule.waitForIdle()

        navController.navigate(Routes.MENU)
        composeTestRule.waitForIdle()

        // Verificar que estamos en MENU
        assertEquals(Routes.MENU, navController.currentBackStackEntry?.destination?.route)

        // Volver atrás
        navController.popBackStack()
        composeTestRule.waitForIdle()

        // Deberíamos estar en HOME
        assertEquals(Routes.HOME, navController.currentBackStackEntry?.destination?.route)
    }
}