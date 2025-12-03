// PackageDetailScreen.kt - VERSIÓN CON BOTONES DE ACCIÓN
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
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.ui.screens.MapboxMapView
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModel
import com.ucb.deliveryapp.viewmodel.UserViewModelFactory
import com.ucb.deliveryapp.viewmodel.getPackageViewModelFactory
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    // Estados para los diálogos de confirmación
    var showDeliveredDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Observar el usuario actual
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
                    containerColor = Color(0xFF00A76D)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                                currentUserName = currentUser?.username ?: "Usuario",
                                navController = navController,
                                onDeliveredClick = { showDeliveredDialog = true },
                                onCancelClick = { showCancelDialog = true },
                                snackbarHostState = snackbarHostState,
                                scope = scope,
                                packageViewModel = packageViewModel
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

        // ✅ DIÁLOGO DE CONFIRMACIÓN PARA "ENTREGADO"
        if (showDeliveredDialog) {
            AlertDialog(
                onDismissRequest = { showDeliveredDialog = false },
                title = {
                    Text(
                        text = "Confirmar Entrega",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "¿Estás seguro de que el paquete ha sido entregado en perfectas condiciones?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "• Verifica que el paquete no tenga daños\n" +
                                    "• Confirma que el contenido esté completo\n" +
                                    "• Asegúrate de que sea el paquete correcto",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeliveredDialog = false
                            scope.launch {
                                when (val state = packageState) {
                                    is com.ucb.deliveryapp.util.Result.Success -> {
                                        packageViewModel.updatePackageStatus(
                                            state.data.id,
                                            PackageStatus.DELIVERED
                                        )
                                        snackbarHostState.showSnackbar("✅ Paquete marcado como entregado")
                                        // Refrescar los datos
                                        packageViewModel.loadPackageById(packageId)
                                    }
                                    else -> {
                                        snackbarHostState.showSnackbar("❌ Error al actualizar el estado")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A76D),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sí, confirmar entrega")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeliveredDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00A76D)
                        )
                    ) {
                        Text("Cancelar")
                    }
                },
                containerColor = Color.White
            )
        }

        // ✅ DIÁLOGO DE CONFIRMACIÓN PARA "CANCELAR"
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = {
                    Text(
                        text = "Cancelar Paquete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "¿Estás seguro de que deseas cancelar este paquete?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "• Se enviará un comprobante de cancelación a tu correo electrónico\n" +
                                    "• Pueden aplicarse cargos por cancelación según las políticas\n" +
                                    "• Esta acción no se puede deshacer",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelDialog = false
                            scope.launch {
                                when (val state = packageState) {
                                    is com.ucb.deliveryapp.util.Result.Success -> {
                                        packageViewModel.updatePackageStatus(
                                            state.data.id,
                                            PackageStatus.CANCELLED
                                        )
                                        snackbarHostState.showSnackbar("✅ Paquete cancelado exitosamente")
                                        // Refrescar los datos
                                        packageViewModel.loadPackageById(packageId)
                                    }
                                    else -> {
                                        snackbarHostState.showSnackbar("❌ Error al cancelar el paquete")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sí, cancelar paquete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCancelDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("No, mantener paquete")
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun PackageDetailContent(
    packageItem: Package,
    currentUserName: String,
    navController: NavController,
    onDeliveredClick: () -> Unit,
    onCancelClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    packageViewModel: PackageViewModel
) {
    // Extraer información de las notas
    val (precioCotizado, tipoEnvio, originPoint, destinationPoint) = extractInfoFromNotes(packageItem.notes)

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

        // MAPA CON RUTA
        if (originPoint != null && destinationPoint != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                var routeInfo by remember { mutableStateOf<String?>(null) }

                MapboxMapView(
                    modifier = Modifier.fillMaxSize(),
                    origin = originPoint,
                    destination = destinationPoint,
                    onRouteInfo = { etaMinutes, distanceKm ->
                        routeInfo = "ETA: ${etaMinutes} min • ${"%.1f".format(distanceKm)} km"
                    }
                )

                // Mostrar info de ruta si está disponible
                routeInfo?.let { info ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Fallback si no hay coordenadas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF80D4B6)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗺️ Mapa no disponible\n(Coordenadas no encontradas)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

                // Mostrar coordenadas si están disponibles
                if (originPoint != null) {
                    InfoRow("📍 Origen:", "${originPoint.latitude()}, ${originPoint.longitude()}")
                }
                if (destinationPoint != null) {
                    InfoRow("🎯 Destino:", "${destinationPoint.latitude()}, ${destinationPoint.longitude()}")
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

        // ✅ BOTONES DE ACCIÓN (solo mostrar si el paquete no está entregado ni cancelado)
        if (packageItem.status != PackageStatus.DELIVERED && packageItem.status != PackageStatus.CANCELLED) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Acciones del Paquete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // BOTÓN "ENTREGADO" (VERDE)
                        Button(
                            onClick = onDeliveredClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A76D),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Text(
                                text = "Marcar como Entregado",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // BOTÓN "CANCELAR" (ROJO)
                        Button(
                            onClick = onCancelClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Text(
                                text = "Cancelar Paquete",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ✅ NOTA INFORMATIVA
                    Text(
                        text = "Nota: Estas acciones actualizarán el estado del paquete y pueden generar notificaciones por correo electrónico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        } else {
            // ✅ MENSAJE CUANDO EL PAQUETE YA ESTÁ ENTREGADO O CANCELADO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (packageItem.status == PackageStatus.DELIVERED) {
                            "✅ Este paquete ya fue entregado"
                        } else {
                            "❌ Este paquete fue cancelado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ✅ ESPACIO FINAL PARA SCROLL
        Spacer(modifier = Modifier.height(32.dp))
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

// ✅ FUNCIÓN PARA EXTRAER COORDENADAS
private fun extractInfoFromNotes(notes: String?): Quadruple<String, String, Point?, Point?> {
    var precioCotizado = ""
    var tipoEnvio = ""
    var originPoint: Point? = null
    var destinationPoint: Point? = null

    if (notes.isNullOrBlank()) {
        return Quadruple(precioCotizado, tipoEnvio, originPoint, destinationPoint)
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
            line.contains("Origen:") -> {
                val originText = line.substringAfter("Origen:").trim()
                originPoint = parseCoordinatesFromString(originText)
            }
            line.contains("Destino:") -> {
                val destText = line.substringAfter("Destino:").trim()
                destinationPoint = parseCoordinatesFromString(destText)
            }
        }
    }

    return Quadruple(precioCotizado, tipoEnvio, originPoint, destinationPoint)
}

// ✅ CLASE PARA EL CUÁDRUPLE
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// ✅ FUNCIÓN PARA PARSEAR COORDENADAS
private fun parseCoordinatesFromString(s: String?): Point? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.trim().replace("\\s+".toRegex(), "")
    val parts = cleaned.split(",")
    if (parts.size < 2) return null
    val a = parts[0].toDoubleOrNull() ?: return null
    val b = parts[1].toDoubleOrNull() ?: return null

    return if (a in -90.0..90.0 && b in -180.0..180.0) {
        Point.fromLngLat(b, a)
    } else if (a in -180.0..180.0 && b in -90.0..90.0) {
        Point.fromLngLat(a, b)
    } else {
        Point.fromLngLat(b, a)
    }
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