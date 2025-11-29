// kotlin+java/com/ucb/deliveryapp/ui/screens/menu/MenuScreen.kt
package com.ucb.deliveryapp.ui.screens.menu

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ucb.deliveryapp.ui.screens.packages.PackageListActivity
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(context.applicationContext as Application)
    )
    val coroutineScope = rememberCoroutineScope()

    // Estado para el diálogo de confirmación de logout
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Menú") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Opción 1: Inicio
                MenuItem(
                    title = "Inicio",
                    onClick = {
                        navController.popBackStack(com.ucb.deliveryapp.ui.navigation.Routes.HOME, false)
                    }
                )

                // Opción 2: Envíos
                MenuItem(
                    title = "Mis Envíos",
                    onClick = {
                        val intent = android.content.Intent(context, PackageListActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                // Opción 3: Perfil (placeholder)
                MenuItem(
                    title = "Perfil",
                    onClick = {
                        navController.navigate(com.ucb.deliveryapp.ui.navigation.Routes.PROFILE)
                    }
                )

                // Opción 4: Soporte (placeholder)
                MenuItem(
                    title = "Soporte",
                    onClick = {
                        navController.navigate(com.ucb.deliveryapp.ui.navigation.Routes.SUPPORT)
                    }
                )

                // Opción 5: Cerrar Sesión - AHORA CON DIÁLOGO DE CONFIRMACIÓN
                MenuItem(
                    title = "Cerrar Sesión",
                    onClick = {
                        showLogoutDialog = true
                    }
                )
            }
        }
    )

    // Diálogo de confirmación para cerrar sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión? Tendrás que iniciar sesión nuevamente para acceder a tu cuenta.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            // Ejecutar logout
                            viewModel.logout()
                            // Navegar al login limpiando el back stack
                            navController.navigate(com.ucb.deliveryapp.ui.navigation.Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MenuItem(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}