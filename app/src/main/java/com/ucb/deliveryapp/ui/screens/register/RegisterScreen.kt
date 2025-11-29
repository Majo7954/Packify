package com.ucb.deliveryapp.ui.screens.register

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.ui.screens.PrivacyPolicyActivity
import com.ucb.deliveryapp.util.Result
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val factory = UserViewModelFactory(context.applicationContext as Application)
    val viewModel: UserViewModel = viewModel(factory = factory)

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Estados para errores de validación
    var usernameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }

    // Recolectar estados del ViewModel
    val registrationState by viewModel.registrationState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    // Snackbar para errores
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Función para validar email
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(emailRegex.toRegex())
    }

    // Efecto para registro exitoso
    LaunchedEffect(registrationState) {
        val currentState = registrationState
        if (currentState is Result.Success<*>) {
            if (currentState.data == true) {
                onRegisterSuccess()
                viewModel.resetRegistrationState()
            }
        }
    }

    // Efecto para mostrar errores del estado de registro
    LaunchedEffect(registrationState) {
        val currentState = registrationState
        if (currentState is Result.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Error en registro: ${currentState.exception.message ?: "Error desconocido"}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetRegistrationState()
            }
        }
    }

    // Efecto para mostrar errores del errorState y resaltar campos
    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            // Determinar qué campo resaltar basado en el mensaje de error
            when {
                errorMessage.contains("correo", ignoreCase = true) ||
                        errorMessage.contains("email", ignoreCase = true) -> {
                    emailError = true
                }
                errorMessage.contains("contraseña", ignoreCase = true) ||
                        errorMessage.contains("password", ignoreCase = true) -> {
                    passwordError = true
                    confirmPasswordError = true
                }
                errorMessage.contains("usuario", ignoreCase = true) -> {
                    usernameError = true
                }
                else -> {
                    usernameError = true
                    emailError = true
                    passwordError = true
                    confirmPasswordError = true
                }
            }

            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    // Resetear errores cuando el usuario empiece a escribir
    LaunchedEffect(username) {
        if (usernameError) usernameError = false
    }

    LaunchedEffect(email) {
        if (emailError) emailError = false
    }

    LaunchedEffect(password) {
        if (passwordError) passwordError = false
        if (confirmPasswordError && password == confirmPassword) confirmPasswordError = false
    }

    LaunchedEffect(confirmPassword) {
        if (confirmPasswordError && password == confirmPassword) confirmPasswordError = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF00A76D)) // Fondo verde
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título "Registro del usuario"
            Text(
                text = "Registro del usuario",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Campo de nombre de usuario
            Text(
                text = "Nombre del usuario",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Contenedor con borde rojo para el campo de username
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (usernameError) 2.dp else 0.dp,
                        color = if (usernameError) Color.Red else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = {
                        Text(
                            "Ingresa el nombre del usuario",
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loadingState,
                    isError = usernameError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorBorderColor = Color.Transparent,
                        errorContainerColor = Color.White,
                        errorCursorColor = Color.Black,
                        errorTextColor = Color.Black,
                        errorPlaceholderColor = Color.Gray
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // Campo de email
            Text(
                text = "Correo electrónico",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Contenedor con borde rojo para el campo de email
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (emailError) 2.dp else 0.dp,
                        color = if (emailError) Color.Red else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = {
                        Text(
                            "Ingresa el correo electrónico",
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loadingState,
                    isError = emailError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorBorderColor = Color.Transparent,
                        errorContainerColor = Color.White,
                        errorCursorColor = Color.Black,
                        errorTextColor = Color.Black,
                        errorPlaceholderColor = Color.Gray
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // Campo de contraseña
            Text(
                text = "Contraseña",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Contenedor con borde rojo para el campo de contraseña
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (passwordError) 2.dp else 0.dp,
                        color = if (passwordError) Color.Red else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text(
                            "Ingresa tu contraseña",
                            color = Color.Gray
                        )
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loadingState,
                    isError = passwordError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorBorderColor = Color.Transparent,
                        errorContainerColor = Color.White,
                        errorCursorColor = Color.Black,
                        errorTextColor = Color.Black,
                        errorPlaceholderColor = Color.Gray
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        val description = if (passwordVisible) "Ocultar contraseña"
                        else "Ver contraseña"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = Color.Black
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Campo de confirmar contraseña
            Text(
                text = "Confirmar contraseña",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Contenedor con borde rojo para el campo de confirmar contraseña
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (confirmPasswordError) 2.dp else 0.dp,
                        color = if (confirmPasswordError) Color.Red else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = {
                        Text(
                            "Confirma tu contraseña",
                            color = Color.Gray
                        )
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loadingState,
                    isError = confirmPasswordError,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorBorderColor = Color.Transparent,
                        errorContainerColor = Color.White,
                        errorCursorColor = Color.Black,
                        errorTextColor = Color.Black,
                        errorPlaceholderColor = Color.Gray
                    ),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        val description = if (confirmPasswordVisible) "Ocultar contraseña"
                        else "Ver contraseña"

                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = Color.Black
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Botón de registro SIEMPRE AMARILLO
            Button(
                onClick = {
                    // Resetear errores
                    usernameError = false
                    emailError = false
                    passwordError = false
                    confirmPasswordError = false

                    // Validaciones
                    when {
                        username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Por favor, completa todos los campos",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            if (username.isBlank()) usernameError = true
                            if (email.isBlank()) emailError = true
                            if (password.isBlank()) passwordError = true
                            if (confirmPassword.isBlank()) confirmPasswordError = true
                        }
                        password != confirmPassword -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Las contraseñas no coinciden",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            passwordError = true
                            confirmPasswordError = true
                        }
                        password.length < 6 -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "La contraseña debe tener al menos 6 caracteres",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            passwordError = true
                            confirmPasswordError = true
                        }
                        !isValidEmail(email) -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Por favor ingresa un email válido",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            emailError = true
                        }
                        else -> {
                            viewModel.register(User(
                                username = username,
                                email = email,
                                password = password
                            ))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !loadingState, // Solo deshabilitado durante carga
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFAC10C), // SIEMPRE amarillo
                    contentColor = Color.White, // Texto blanco
                    disabledContainerColor = Color(0xFFFAC10C) // Mismo color cuando está deshabilitado
                )
            ) {
                if (loadingState) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Creando cuenta...")
                    }
                } else {
                    Text(
                        "Registrarse",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Texto "¿Ya tienes cuenta?" con "Inicia sesión" resaltado
            Text(
                text = buildAnnotatedString {
                    append("¿Ya tienes cuenta? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Inicia sesión")
                    }
                },
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !loadingState,
                        onClick = { onNavigateToLogin() }
                    )
            )

            Spacer(Modifier.height(16.dp))

            // Enlace a Política de Privacidad
            Text(
                text = "Política de Privacidad",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clickable {
                        val intent = Intent(context, PrivacyPolicyActivity::class.java)
                        context.startActivity(intent)
                    }
            )
        }
    }
}