package com.ucb.deliveryapp.features.settings.presentation

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.auth.presentation.UserViewModel
import com.ucb.deliveryapp.navigation.Routes
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import com.ucb.deliveryapp.ui.theme.rojoDelivery
import com.ucb.deliveryapp.ui.theme.verdeDelivery
import com.ucb.deliveryapp.ui.theme.verdeClarito
import com.ucb.deliveryapp.ui.theme.amarilloDelivery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit) {
    val context = LocalContext.current
    val viewModel: UserViewModel = koinViewModel()
    val coroutineScope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val currentUser by viewModel.currentUser.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val profileUpdateState by viewModel.profileUpdateState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            username = user.username
            email = user.email
        }
    }

    LaunchedEffect(profileUpdateState) {
        when (profileUpdateState) {
            is Result.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.perfil_actualizado_exitosamente_snackbar),
                        duration = SnackbarDuration.Short
                    )
                }
                isEditing = false
                viewModel.resetProfileUpdateState()
            }
            is Result.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.error_actualizar_perfil_snackbar),
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nombre),
                            contentDescription = stringResource(R.string.logo),
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
                            contentDescription = stringResource(R.string.volver_al_menu),
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
                Text(
                    if (isEditing) stringResource(R.string.editar_perfil) else stringResource(R.string.perfil),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    textAlign = TextAlign.Start
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileField(
                        label = stringResource(R.string.nombre_de_usuario_dos_puntos),
                        value = username,
                        isEditing = isEditing,
                        onValueChange = { username = it },
                        placeholder = stringResource(R.string.ingresa_tu_nombre_de_usuario)
                    )

                    ProfileField(
                        label = stringResource(R.string.correo_electronico_dos_puntos),
                        value = email,
                        isEditing = false,
                        onValueChange = {  },
                        placeholder = stringResource(R.string.correo_electronico)
                    )

                    Text(
                        text = context.getString(R.string.id_mostrado, currentUser?.id?.take(8) ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }

                if (!isEditing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = verdeClarito)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.info_emoji_titulo),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.info_texto),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isEditing = false
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
                                    stringResource(R.string.cancelar),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    if (username.isBlank()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.nombre_usuario_vacio),
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
                                        contentDescription = stringResource(R.string.guardar),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.guardar),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
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
                                stringResource(R.string.editar_perfil),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

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
                                stringResource(R.string.cerrar_sesion),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.cerrar_sesion)) },
            text = { Text(stringResource(R.string.confirmar_cerrar_sesion)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            viewModel.logout()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(stringResource(R.string.cancelar))
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
                textStyle = TextStyle(
                    color = Color.Black
                )
            )
        } else {
            Text(
                text = value.ifBlank { stringResource(R.string.no_especificado) },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}