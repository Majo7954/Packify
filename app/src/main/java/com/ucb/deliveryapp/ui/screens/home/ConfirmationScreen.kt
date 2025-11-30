// ConfirmationScreen.kt
package com.ucb.deliveryapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ucb.deliveryapp.ui.navigation.Routes

// Colores personalizados
val verdeDelivery = Color(0xFF00A76D)
val amarilloDelivery = Color(0xFFFAC10C)

@Composable
fun ConfirmationScreen(
    navController: NavController, // ✅ AGREGAR NavController como parámetro
    onNavigateToPackages: () -> Unit
) {
    val context = LocalContext.current

    // ✅ FONDO VERDE PARA TODA LA PANTALLA
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(verdeDelivery)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ BLOQUE BLANCO EN EL MEDIO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ✅ ÍCONO DE CHECK EN FONDO VERDE
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(verdeDelivery, RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmado",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White // ✅ Check blanco
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ TEXTO PRINCIPAL EN NEGRO
                    Text(
                        "Tu pedido ha sido realizado con éxito.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color.Black, // ✅ Texto negro
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ TEXTO "REALICE EL SEGUIMIENTO" EN NEGRO
                    Text(
                        "Realice el seguimiento:",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = Color.Black, // ✅ Texto negro
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ BOTÓN AMARILLO QUE NAVEGA A PACKAGE LIST SCREEN
                    Button(
                        onClick = {
                            // ✅ NAVEGAR A PACKAGE LIST SCREEN (COMPOSE) - igual que en MenuScreen
                            navController.navigate(Routes.PACKAGES) {
                                // Limpiar el stack de navegación para que no pueda volver atrás
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = amarilloDelivery,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Ver mis paquetes",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }

            // ✅ ESPACIO EN BLANCO ABAJO DEL BLOQUE
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}