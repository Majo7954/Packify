package com.ucb.deliveryapp.ui.screens.login

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
import androidx.compose.ui.draw.clip
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
import com.ucb.deliveryapp.ui.screens.PrivacyPolicyActivity
import com.ucb.deliveryapp.util.Result
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val factory = UserViewModelFactory(context.applicationContext as Application)
    val viewModel: UserViewModel = viewModel(factory = factory)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Estados para errores de validación
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    // Recolectar todos los estados del ViewModel
    val loginState by viewModel.loginState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    // Snackbar para mostrar errores
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Función para validar email
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(emailRegex.toRegex())
    }

    // Efecto para login exitoso
    LaunchedEffect(loginState) {
        if (loginState is Result.Success) {
            onLoginSuccess()
        }
    }

    // Efecto para mostrar errores con Snackbar y resaltar campos
    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            try {
                // Determinar qué campo resaltar basado en el mensaje de error
                when {
                    errorMessage.contains("correo", ignoreCase = true) ||
                            errorMessage.contains("email", ignoreCase = true) -> {
                        emailError = true
                    }
                    errorMessage.contains("contraseña", ignoreCase = true) ||
                            errorMessage.contains("password", ignoreCase = true) -> {
                        passwordError = true
                    }
                    else -> {
                        emailError = true
                        passwordError = true
                    }
                }

                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = errorMessage.take(100), // ✅ LIMITA longitud para evitar crashes
                        duration = SnackbarDuration.Short
                    )
                }

                // Limpiar el error después de mostrarlo
                viewModel.clearError()
            } catch (e: Exception) {
                // ✅ EVITA CRASHES en caso de error inesperado
                e.printStackTrace()
            }
        }
    }

    // Resetear errores cuando el usuario empiece a escribir
    LaunchedEffect(email) {
        if (emailError) emailError = false
    }

    LaunchedEffect(password) {
        if (passwordError) passwordError = false
    }

    // Scaffold para manejar el Snackbar
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
            // Título "Inicio de sesión"
            Text(
                text = "Inicio de sesión",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Subtítulo "Bienvenido a Packify"
            Text(
                text = buildAnnotatedString {
                    append("Bienvenido a ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Packify")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

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
                            "Escribe aquí el correo electrónico",
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
                        errorBorderColor = Color.Transparent, // Transparente porque usamos el borde externo
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
                            "Ingresa aquí tu contraseña",
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
                        errorBorderColor = Color.Transparent, // Transparente porque usamos el borde externo
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

            // ✅ TÉRMINOS Y CONDICIONES - MÁS DESTACADO (OBLIGATORIO PLAY STORE)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, Color.White, MaterialTheme.shapes.small)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable {
                        val intent = Intent(context, PrivacyPolicyActivity::class.java)
                        context.startActivity(intent)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Texto "Al iniciar sesión aceptas nuestros" ARRIBA
                    Text(
                        text = "Al iniciar sesión aceptas nuestros",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    // Texto "Términos y Política de Privacidad" ABAJO
                    Text(
                        text = "Términos y Política de Privacidad",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botón de login SIEMPRE AMARILLO
            Button(
                onClick = {
                    // Resetear errores
                    emailError = false
                    passwordError = false

                    // Validaciones
                    when {
                        email.isBlank() || password.isBlank() -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Por favor, completa todos los campos",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            if (email.isBlank()) emailError = true
                            if (password.isBlank()) passwordError = true
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
                        password.length < 6 -> { // ✅ MÍNIMO 6 CARACTERES (SEGURIDAD)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "La contraseña debe tener al menos 6 caracteres",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            passwordError = true
                        }
                        else -> {
                            viewModel.login(email, password)
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
                        Text("Iniciando sesión...")
                    }
                } else {
                    Text(
                        "Iniciar Sesión",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Texto "No tienes cuenta" con "Regístrate aquí" resaltado
            Text(
                text = buildAnnotatedString {
                    append("¿No tienes cuenta? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Regístrate aquí")
                    }
                },
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !loadingState,
                        onClick = { onNavigateToRegister() }
                    )
            )
        }
    }
}