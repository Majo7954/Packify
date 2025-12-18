package com.ucb.deliveryapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ucb.deliveryapp.features.home.presentation.ConfirmationScreen
import com.ucb.deliveryapp.features.home.presentation.HomeScreen
import com.ucb.deliveryapp.features.auth.presentation.LoginScreen
import com.ucb.deliveryapp.features.settings.presentation.MenuScreen
import com.ucb.deliveryapp.features.packages.presentation.PackageListScreen
import com.ucb.deliveryapp.features.auth.presentation.RegisterScreen
import com.ucb.deliveryapp.features.settings.presentation.SupportScreen
import com.ucb.deliveryapp.features.settings.presentation.ProfileScreen
import com.ucb.deliveryapp.features.home.presentation.CreatePackageComposeScreen
import com.ucb.deliveryapp.features.packages.presentation.PackageDetailScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MENU = "menu"
    const val SUPPORT = "support"
    const val PROFILE = "profile"
    const val PACKAGES = "packages"
    const val CREATE_PACKAGE = "create_package"
    const val CONFIRMATION = "confirmation"
    const val PACKAGE_DETAIL = "package_detail/{packageId}"

    fun packageDetail(packageId: String): String {
        return "package_detail/$packageId"
    }
}

@Composable
fun AppNavHost(
    isLoggedIn: Boolean,
    onLoginStateChange: (Boolean) -> Unit,
    pendingRoute: String?,
    onPendingRouteHandled: () -> Unit
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingRoute, isLoggedIn) {
        if (!isLoggedIn) return@LaunchedEffect
        val route = pendingRoute ?: return@LaunchedEffect

        navController.navigate(route) {
            launchSingleTop = true
        }
        onPendingRouteHandled()
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Routes.HOME else Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    onLoginStateChange(true)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    onLoginStateChange(true)
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
                navController = navController,
                onLogout = {
                    onLoginStateChange(false)
                }
            )
        }

        composable(Routes.MENU) {
            MenuScreen(navController = navController)
        }

        composable(Routes.SUPPORT) {
            SupportScreen(navController = navController)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    onLoginStateChange(false)
                }
            )
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

        composable(
            route = Routes.PACKAGE_DETAIL,
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageId = backStackEntry.arguments?.getString("packageId") ?: ""
            PackageDetailScreen(
                navController = navController,
                packageId = packageId
            )
        }
    }
}