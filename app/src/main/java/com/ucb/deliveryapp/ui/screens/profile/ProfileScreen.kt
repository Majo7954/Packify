// kotlin+java/com/ucb/deliveryapp/ui/screens/profile/ProfileScreen.kt
package com.ucb.deliveryapp.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

// Colores personalizados
val verdeDelivery = Color(0xFF00A76D)
val amarilloDelivery = Color(0xFFFAC10C)
val rojoDelivery = Color(0xFFF44336) // Rojo para cerrar sesión
val verdeClarito = Color(0xFF80D4B6) // Verde clarito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(context.applicationContext as android.app.Application)
    )
    val coroutineScope = rememberCoroutineScope()

    // Estados locales
    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Observar el usuario actual
    val currentUser by viewModel.currentUser.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val profileUpdateState by viewModel.profileUpdateState.collectAsState()

    // Cargar usuario cuando la pantalla se abre
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    // Actualizar campos cuando cambia el usuario
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            username = user.username
            email = user.email
        }
    }

    // Manejar resultado de actualización
    LaunchedEffect(profileUpdateState) {
        when (profileUpdateState) {
            is com.ucb.deliveryapp.util.Result.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "✅ Perfil actualizado exitosamente",
                        duration = SnackbarDuration.Short
                    )
                }
                isEditing = false
                viewModel.resetProfileUpdateState()
            }
            is com.ucb.deliveryapp.util.Result.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "❌ Error al actualizar perfil",
                        duration = SnackbarDuration.Short
                    )
                }
                viewModel.resetProfileUpdateState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Imagen en lugar de texto
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nombre),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .height(32.dp)
                                .widthIn(max = 200.dp)
                                .padding(start = 92.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver al menú",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = verdeDelivery
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        // Fondo blanco para TODA la pantalla debajo de la barra superior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título - Alineado a la izquierda, color negro
                Text(
                    if (isEditing) "Editar Perfil" else "Perfil",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                // Información del usuario - SIN CONTENEDOR
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Campo de nombre de usuario
                    ProfileField(
                        label = "Nombre de usuario:",
                        value = username,
                        isEditing = isEditing,
                        onValueChange = { username = it },
                        placeholder = "Ingresa tu nombre de usuario"
                    )

                    // Campo de email (solo lectura)
                    ProfileField(
                        label = "Correo electrónico:",
                        value = email,
                        isEditing = false, // Email no editable por ahora
                        onValueChange = { /* No editable */ },
                        placeholder = "Correo electrónico"
                    )

                    // Información adicional
                    Text(
                        "ID: ${currentUser?.id?.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }

                // Información sobre edición - Con fondo verde clarito (solo cuando no está editando)
                if (!isEditing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = verdeClarito)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "💡 Información",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Presiona el botón 'Editar Perfil' para modificar tu información. " +
                                        "El correo electrónico no se puede modificar por seguridad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Botones de acción
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditing) {
                        // Botones cuando está editando - SOLO CANCELAR Y GUARDAR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Botón cancelar - COLOR ROJO
                            Button(
                                onClick = {
                                    isEditing = false
                                    // Restaurar valores originales
                                    currentUser?.let { user ->
                                        username = user.username
                                        email = user.email
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !loadingState,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = rojoDelivery,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    "Cancelar",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botón guardar - COLOR AMARILLO
                            Button(
                                onClick = {
                                    if (username.isBlank()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "El nombre de usuario no puede estar vacío",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } else {
                                        currentUser?.let { currentUser ->
                                            val updatedUser = currentUser.copy(
                                                username = username.trim()
                                            )
                                            viewModel.updateUserProfile(updatedUser)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !loadingState && username.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = amarilloDelivery,
                                    contentColor = Color.White
                                )
                            ) {
                                if (loadingState) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Guardar",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Guardar",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Botones cuando NO está editando - SOLO EDITAR PERFIL Y CERRAR SESIÓN
                        // Botón Editar Perfil (amarillo)
                        Button(
                            onClick = { isEditing = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !loadingState,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = amarilloDelivery,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                "Editar Perfil",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Botón Cerrar Sesión (rojo)
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = rojoDelivery,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                "Cerrar Sesión",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

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
fun ProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        Spacer(Modifier.height(8.dp))
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.Black
                )
            )
        } else {
            Text(
                text = value.ifBlank { "No especificado" },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}