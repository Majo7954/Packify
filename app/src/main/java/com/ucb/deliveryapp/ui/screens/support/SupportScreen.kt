// kotlin+java/com/ucb/deliveryapp/ui/screens/support/SupportScreen.kt
package com.ucb.deliveryapp.ui.screens.support

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ucb.deliveryapp.R
import kotlinx.coroutines.launch

// Colores personalizados
val verdeDelivery = Color(0xFF00A76D)
val amarilloDelivery = Color(0xFFFAC10C)
val verdeClarito = Color(0xFF80D4B6) // Verde más clarito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(navController: NavController) {
    var message by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Imagen en lugar de texto "Soporte"
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
                // Título principal - Alineado a la izquierda y color negro
                Text(
                    "Soporte Técnico",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )

                // Información para el usuario - Con fondo verde clarito y texto blanco
                // INCLUYENDO la información de contacto dentro del mismo cuadro
                Card(
                    colors = CardDefaults.cardColors(containerColor = verdeClarito),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "¿Necesitas ayuda?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Describe el problema o consulta que tengas con la aplicación. " +
                                    "Nuestro equipo te responderá a la brevedad posible.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = Color.Black
                        )

                        Spacer(Modifier.height(16.dp))

                        // Información de contacto DENTRO del mismo cuadro verde
                        SelectionContainer {
                            Column {
                                Text(
                                    "Información de Contacto",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Email: soporte@packify.com\n" +
                                            "Respuesta en: 24-48 horas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Campo de mensaje
                Column {
                    Text(
                        "Mensaje:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = {
                            Text(
                                "Ingrese tu mensaje de soporte...",
                                color = Color.Black
                            )
                        },
                        singleLine = false,
                        maxLines = 10,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.Black
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Botón de enviar - SIEMPRE VISIBLE con color amarillo y texto blanco grueso
                Button(
                    onClick = {
                        if (message.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Por favor, escribe tu mensaje de soporte",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "✅ Mensaje enviado. Te contactaremos pronto.",
                                    duration = SnackbarDuration.Long
                                )
                                message = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amarilloDelivery,
                        contentColor = Color.White,
                        disabledContainerColor = amarilloDelivery,
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        if (message.isBlank()) "Escribe un mensaje para enviar" else "Enviar Mensaje",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Texto informativo sobre privacidad - CENTRADO
                Text(
                    "Al enviar este mensaje, aceptas nuestra Política de Privacidad. " +
                            "Tu información será usada solo para brindarte soporte técnico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}