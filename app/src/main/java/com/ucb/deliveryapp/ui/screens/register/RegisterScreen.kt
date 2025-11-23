package com.ucb.deliveryapp.ui.screens.register

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ucb.deliveryapp.data.entity.User
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit,
                   viewModel: UserViewModel = viewModel()) {

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Registro", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Nombre de usuario") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirmar contraseña") }, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (password == confirm) {
                    viewModel.register(User(username = username, email = email, password = password))
                    onRegisterSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Registrarse") }
    }
}
