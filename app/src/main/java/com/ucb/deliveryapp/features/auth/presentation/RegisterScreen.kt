package com.ucb.deliveryapp.features.auth.presentation

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.features.auth.data.remote.dto.UserDto
import com.ucb.deliveryapp.features.settings.presentation.PrivacyPolicyActivity
import com.ucb.deliveryapp.core.util.Result
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserViewModel = koinViewModel()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }

    val registrationState by viewModel.registrationState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(emailRegex.toRegex())
    }

    LaunchedEffect(registrationState) {
        val currentState = registrationState
        if (currentState is Result.Success<*>) {
            if (currentState.data == true) {
                onRegisterSuccess()
                viewModel.resetRegistrationState()
            }
        }
    }

    LaunchedEffect(registrationState) {
        val currentState = registrationState
        if (currentState is Result.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(
                        R.string.error_en_registro,
                        currentState.exception.message ?: context.getString(R.string.error_desconocido)
                    ),
                    duration = SnackbarDuration.Long
                )
                viewModel.resetRegistrationState()
            }
        }
    }

    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            try {
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
                        message = errorMessage.take(100),
                        duration = SnackbarDuration.Short
                    )
                }
                viewModel.clearError()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF00A76D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.registro_usuario),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.nombre_usuario),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

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
                        onValueChange = { newValue -> username = newValue.take(15) },
                        placeholder = {
                            Text(
                                stringResource(R.string.ingresa_nombre_usuario),
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

                Text(
                    text = stringResource(R.string.correo_electronico),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

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
                        onValueChange = { newValue ->
                            email = newValue
                                .trim()
                                .replace("\\s+".toRegex(), "")
                                .take(50)
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.ingresa_correo_electronico),
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

                Text(
                    text = stringResource(R.string.contrasena_mayus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

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
                        onValueChange = { newValue -> password = newValue.take(20) },
                        placeholder = {
                            Text(
                                stringResource(R.string.ingresa_tu_contrasena),
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

                            val description = if (passwordVisible) stringResource(R.string.ocultar_contrasena)
                            else stringResource(R.string.ver_contrasena)

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

                Text(
                    text = stringResource(R.string.confirmar_contrasena),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

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
                        onValueChange = { newValue -> confirmPassword = newValue.take(20) },
                        placeholder = {
                            Text(
                                stringResource(R.string.confirma_tu_contrasena),
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

                            val description = if (confirmPasswordVisible) stringResource(R.string.ocultar_contrasena)
                            else stringResource(R.string.ver_contrasena)

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

                Spacer(Modifier.height(16.dp))

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
                        Text(
                            text = stringResource(R.string.aceptar_registrarte),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.terminos_politica_privacidad),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        usernameError = false
                        emailError = false
                        passwordError = false
                        confirmPasswordError = false

                        when {
                            username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.completa_campos),
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
                                        message = context.getString(R.string.contrasenas_no_coinciden),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                passwordError = true
                                confirmPasswordError = true
                            }
                            password.length < 6 -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.contrasena_valida),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                passwordError = true
                                confirmPasswordError = true
                            }
                            !isValidEmail(email) -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.correo_valido),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                emailError = true
                            }
                            else -> {
                                viewModel.register(
                                    UserDto(
                                        username = username,
                                        email = email,
                                        password = password
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !loadingState,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFAC10C),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFFAC10C)
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
                            Text(stringResource(R.string.creando_cuenta))
                        }
                    } else {
                        Text(
                            stringResource(R.string.registrarse),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.ya_tienes_cuenta))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.inicia_sesion))
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
            }
        }
    }
}
