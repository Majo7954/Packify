package com.ucb.deliveryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.ui.navigation.Routes
import com.ucb.deliveryapp.ui.screens.home.ConfirmationScreen
import com.ucb.deliveryapp.ui.screens.home.HomeScreen
import com.ucb.deliveryapp.ui.screens.login.LoginScreen
import com.ucb.deliveryapp.ui.screens.menu.MenuScreen
import com.ucb.deliveryapp.ui.screens.packages.PackageListScreen
import com.ucb.deliveryapp.ui.screens.register.RegisterScreen
import com.ucb.deliveryapp.ui.screens.support.SupportScreen
import com.ucb.deliveryapp.ui.screens.profile.ProfileScreen
import com.ucb.deliveryapp.ui.screens.home.CreatePackageComposeScreen
import com.ucb.deliveryapp.ui.screens.packages.PackageDetailScreen
import com.ucb.deliveryapp.ui.theme.DeliveryappTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeliveryappTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DeliveryApp()
                }
            }
        }
    }
}

@Composable
fun DeliveryApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val loginDataStore = remember { LoginDataStore(context) }

    // Estado para verificar si está logueado
    var isLoggedIn by remember { mutableStateOf(false) }

    // Verificar sesión al inicio
    LaunchedEffect(Unit) {
        // Esto es una operación de suspensión, la ejecutamos en un coroutine
        isLoggedIn = loginDataStore.isLoggedIn()
    }

    // Navegar al login si no está logueado
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            // Navegar al login y limpiar el back stack
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }

    // Definir el NavHost
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Routes.HOME else Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    // Actualizar estado y navegar a HOME
                    isLoggedIn = true
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Después de registro exitoso, navegar a HOME
                    isLoggedIn = true
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToMenu = { navController.navigate(Routes.MENU) },
                navController = navController
            )
        }
        composable(Routes.MENU) {
            MenuScreen(navController = navController)
        }
        composable(Routes.SUPPORT) {
            SupportScreen(navController = navController)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.PACKAGES) {
            PackageListScreen(navController = navController)
        }
        composable(Routes.CREATE_PACKAGE) {
            CreatePackageComposeScreen(
                onNavigateToMenu = { navController.navigate(Routes.MENU) },
                navController = navController
            )
        }
        composable(Routes.CONFIRMATION) {
            ConfirmationScreen(
                navController = navController,
                onNavigateToPackages = {
                    navController.navigate(Routes.PACKAGES) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
        composable("package_detail/{packageId}") { backStackEntry ->
            val packageId = backStackEntry.arguments?.getString("packageId") ?: ""
            PackageDetailScreen(
                navController = navController,
                packageId = packageId
            )
        }
    }
}