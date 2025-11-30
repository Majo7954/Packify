// AppNavHost.kt
package com.ucb.deliveryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ucb.deliveryapp.ui.screens.home.HomeScreen
import com.ucb.deliveryapp.ui.screens.login.LoginScreen
import com.ucb.deliveryapp.ui.screens.menu.MenuScreen
import com.ucb.deliveryapp.ui.screens.packages.PackageListScreen
import com.ucb.deliveryapp.ui.screens.register.RegisterScreen
import com.ucb.deliveryapp.ui.screens.support.SupportScreen
import com.ucb.deliveryapp.ui.screens.profile.ProfileScreen
import com.ucb.deliveryapp.ui.screens.home.CreatePackageComposeScreen
import com.ucb.deliveryapp.ui.screens.packages.PackageDetailScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MENU = "menu"
    const val SUPPORT = "support"
    const val PROFILE = "profile"
    const val PACKAGES = "packages"
    const val CREATE_PACKAGE = "create_package" // NUEVA RUTA AGREGADA
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
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
                navController = navController // ✅ Agregar este parámetro
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
                navController = navController // ✅ AGREGAR este parámetro
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