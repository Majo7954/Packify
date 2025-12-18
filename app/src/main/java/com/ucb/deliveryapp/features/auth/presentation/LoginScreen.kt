package com.ucb.deliveryapp.features.auth.presentation

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
import com.ucb.deliveryapp.features.settings.presentation.PrivacyPolicyActivity
import com.ucb.deliveryapp.core.util.Result
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: UserViewModel = koinViewModel()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
        return email.matches(emailRegex.toRegex())
    }

    LaunchedEffect(loginState) {
        if (loginState is Result.Success) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            try {
                when {
                    errorMessage.contains(context.getString(R.string.email), ignoreCase = true) ||
                            errorMessage.contains(context.getString(R.string.correo), ignoreCase = true) -> {
                        emailError = true
                    }
                    errorMessage.contains(context.getString(R.string.contrasena), ignoreCase = true) ||
                            errorMessage.contains(context.getString(R.string.password), ignoreCase = true) -> {
                        passwordError = true
                    }
                    else -> {
                        emailError = true
                        passwordError = true
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

    LaunchedEffect(email) {
        if (emailError) emailError = false
    }

    LaunchedEffect(password) {
        if (passwordError) passwordError = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF00A76D))
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.inicio_sesion),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.bienvenido_a))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.app_name))
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

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
                            stringResource(R.string.campo_correo_electronico),
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
                            stringResource(R.string.ingresa_contrasena),
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
                        text = stringResource(R.string.aceptar_iniciar_sesion),
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
                    emailError = false
                    passwordError = false

                    when {
                        email.isBlank() || password.isBlank() -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.completa_campos),
                                    duration = SnackbarDuration.Short
                                )
                            }
                            if (email.isBlank()) emailError = true
                            if (password.isBlank()) passwordError = true
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
                        password.length < 6 -> {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.contrasena_valida),
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
                        Text(stringResource(R.string.iniciando_sesion))
                    }
                } else {
                    Text(
                        stringResource(R.string.iniciar_sesion),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.no_tienes_cuenta))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.registrate_aqui))
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