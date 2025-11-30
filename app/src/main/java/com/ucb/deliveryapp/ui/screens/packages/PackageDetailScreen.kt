// PackageDetailScreen.kt - VERSIÓN CORREGIDA
package com.ucb.deliveryapp.ui.screens.packages

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import com.ucb.deliveryapp.viewmodel.getPackageViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ✅ ELIMINA esta línea duplicada:
// val verdeDelivery = Color(0xFF00A76D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDetailScreen(navController: NavController, packageId: String) {
    val context = LocalContext.current
    val packageViewModel: PackageViewModel = viewModel(factory = getPackageViewModelFactory(context))
    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(context.applicationContext as Application)
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val packageState by packageViewModel.selectedPackageState.collectAsState()
    val loadingState by packageViewModel.loadingState.collectAsState()

    // Observar el usuario actual como en ProfileScreen
    val currentUser by userViewModel.currentUser.collectAsState()

    // Cargar el paquete y el usuario cuando se abre la pantalla
    LaunchedEffect(packageId) {
        packageViewModel.loadPackageById(packageId)
        userViewModel.loadCurrentUser()
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
                    // ✅ Usa el color directamente o importa de otro archivo
                    containerColor = Color(0xFF00A76D) // Verde delivery
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                if (loadingState) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (val state = packageState) {
                        is com.ucb.deliveryapp.util.Result.Success -> {
                            val packageItem = state.data
                            PackageDetailContent(
                                packageItem = packageItem,
                                currentUserName = currentUser?.username ?: "Usuario"
                            )
                        }
                        is com.ucb.deliveryapp.util.Result.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Error al cargar el paquete: ${state.exception.message}")
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Paquete no encontrado")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ... (el resto del código se mantiene igual, sin cambios)
@Composable
fun PackageDetailContent(packageItem: Package, currentUserName: String) {
    // Extraer información de las notas
    val (precioCotizado, tipoEnvio) = extractInfoFromNotes(packageItem.notes)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Nº ${packageItem.trackingNumber}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = getStatusColor(packageItem.status).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = getStatusText(packageItem.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = getStatusColor(packageItem.status),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Información de Envío",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                InfoRow("👤 Remitente:", currentUserName)
                InfoRow("👤 Destinatario:", packageItem.recipientName)
                InfoRow("📍 Dirección:", packageItem.recipientAddress)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Detalles del Paquete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                InfoRow("⚖️ Peso:", "${packageItem.weight} kg")
                InfoRow("🚚 Prioridad:", getPriorityText(packageItem.priority))

                if (precioCotizado.isNotBlank()) {
                    InfoRow("💰 Precio Cotizado:", precioCotizado)
                }

                if (tipoEnvio.isNotBlank()) {
                    InfoRow("🌍 Tipo de Envío:", tipoEnvio)
                }

                InfoRow("📅 Fecha estimada:", formatDate(packageItem.estimatedDeliveryDate))
                InfoRow("🕐 Fecha de creación:", formatDate(packageItem.createdAt))

                if (packageItem.deliveredAt != null) {
                    InfoRow("✅ Entregado el:", formatDate(packageItem.deliveredAt))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun extractInfoFromNotes(notes: String?): Pair<String, String> {
    var precioCotizado = ""
    var tipoEnvio = ""

    if (notes.isNullOrBlank()) {
        return Pair(precioCotizado, tipoEnvio)
    }

    val lines = notes.split("\n")
    lines.forEach { line ->
        when {
            line.contains("Precio cotizado:") -> {
                precioCotizado = line.substringAfter("Precio cotizado:").trim()
            }
            line.contains("Envío dentro del departamento") -> {
                tipoEnvio = "Dentro del departamento"
            }
            line.contains("Envío nacional") -> {
                tipoEnvio = "Nacional"
            }
        }
    }

    return Pair(precioCotizado, tipoEnvio)
}

private fun getPriorityText(priority: String): String {
    return when (priority) {
        "normal" -> "Normal"
        "express" -> "Express"
        "urgent" -> "Urgente"
        else -> priority
    }
}

private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
}

private fun getStatusText(status: String): String {
    return when (status) {
        "pending" -> "⏳ Pendiente"
        "in_transit" -> "🚚 En tránsito"
        "delivered" -> "✓ Entregado"
        "cancelled" -> "✗ Cancelado"
        else -> "Desconocido"
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        "pending" -> Color(0xFFFF9800)
        "in_transit" -> Color(0xFF2196F3)
        "delivered" -> Color(0xFF4CAF50)
        "cancelled" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}