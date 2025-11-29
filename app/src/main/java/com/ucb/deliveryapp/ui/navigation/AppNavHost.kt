// kotlin+java/com/ucb/deliveryapp/ui/navigation/AppNavHost.kt
package com.ucb.deliveryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ucb.deliveryapp.ui.screens.home.HomeScreen
import com.ucb.deliveryapp.ui.screens.login.LoginScreen
import com.ucb.deliveryapp.ui.screens.menu.MenuScreen
import com.ucb.deliveryapp.ui.screens.register.RegisterScreen
import com.ucb.deliveryapp.ui.screens.support.SupportScreen
import com.ucb.deliveryapp.ui.screens.profile.ProfileScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MENU = "menu"
    const val SUPPORT = "support"
    const val PROFILE = "profile"
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
                onNavigateToMenu = { navController.navigate(Routes.MENU) }
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
    }
}