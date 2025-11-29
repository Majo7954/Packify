// kotlin+java/com/ucb/deliveryapp/ui/screens/profile/ProfileScreen.kt
package com.ucb.deliveryapp.ui.screens.profile

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

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
                    Text(
                        "Mi Perfil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver al menú"
                        )
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(
                            onClick = { isEditing = true },
                            enabled = !loadingState
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar perfil"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de información del usuario
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Campo de nombre de usuario
                    ProfileField(
                        label = "Nombre de usuario:",
                        value = username,
                        isEditing = isEditing,
                        onValueChange = { username = it },
                        placeholder = "Ingresa tu nombre de usuario"
                    )

                    Spacer(Modifier.height(16.dp))

                    // Campo de email (solo lectura)
                    ProfileField(
                        label = "Correo electrónico:",
                        value = email,
                        isEditing = false, // Email no editable por ahora
                        onValueChange = { /* No editable */ },
                        placeholder = "Correo electrónico"
                    )

                    Spacer(Modifier.height(8.dp))

                    // Información adicional
                    Text(
                        "ID: ${currentUser?.id?.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botones de acción
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón cancelar
                    OutlinedButton(
                        onClick = {
                            isEditing = false
                            // Restaurar valores originales
                            currentUser?.let { user ->
                                username = user.username
                                email = user.email
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loadingState
                    ) {
                        Text("Cancelar")
                    }

                    // Botón guardar
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
                        enabled = !loadingState && username.isNotBlank()
                    ) {
                        if (loadingState) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Guardar",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar")
                        }
                    }
                }
            }

            // Información sobre edición
            if (!isEditing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "💡 Información",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Presiona el icono de edición para modificar tu información de perfil. " +
                                    "El correo electrónico no se puede modificar por seguridad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                singleLine = true
            )
        } else {
            Text(
                text = value.ifBlank { "No especificado" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}