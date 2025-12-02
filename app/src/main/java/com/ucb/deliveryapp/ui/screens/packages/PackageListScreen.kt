// PackageListScreen.kt - VERSIÓN CON ORDENACIÓN INTELIGENTE Y TRANSICIONES AUTOMÁTICAS
package com.ucb.deliveryapp.ui.screens.packages

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.getPackageViewModelFactory
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Colores personalizados
val verdeDelivery = Color(0xFF00A76D)
val amarilloDelivery = Color(0xFFFAC10C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageListScreen(navController: NavController) {
    val context = LocalContext.current

    // CORREGIR: Usar el factory correcto
    val viewModel: PackageViewModel = viewModel(
        factory = getPackageViewModelFactory(context)
    )

    val coroutineScope = rememberCoroutineScope()

    // DataStore para obtener el userId
    val loginDataStore = remember { LoginDataStore(context) }

    // Estados
    val packagesState by viewModel.packagesState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()

    // Estado para el userId
    var userId by remember { mutableStateOf<String?>(null) }

    // Cargar userId y luego paquetes
    LaunchedEffect(Unit) {
        // Obtener userId del DataStore
        userId = loginDataStore.getUserId()
        println("🔄 DEBUG: UserId obtenido: $userId")

        if (userId != null) {
            viewModel.loadUserPackages(userId!!)
        } else {
            println("❌ DEBUG: No se pudo obtener el userId")
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navegar a crear paquete - CORREGIDO
                    navController.navigate("create_package")
                },
                containerColor = amarilloDelivery,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Añadir paquete")
            }
        }
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
            ) {
                Text(
                    "Mis Paquetes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    textAlign = TextAlign.Start
                )

                Spacer(Modifier.height(16.dp))

                if (userId == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No se pudo identificar al usuario",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Por favor, inicia sesión nuevamente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else if (loadingState) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = verdeDelivery)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Cargando paquetes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    when (val state = packagesState) {
                        is com.ucb.deliveryapp.util.Result.Success -> {
                            // ✅ 1. APLICAR ORDENACIÓN INTELIGENTE
                            val sortedPackages = sortPackagesByStateAndDate(state.data)

                            // ✅ 2. SEPARAR EN ACTIVOS E INACTIVOS
                            val (activePackages, inactivePackages) = separateActiveInactivePackages(sortedPackages)

                            // ✅ 3. APLICAR TRANSICIONES AUTOMÁTICAS (solo visual)
                            val packagesWithAutoStatus = sortedPackages.map { pkg ->
                                applyAutoStatusTransition(pkg)
                            }

                            println("✅ DEBUG: ${packagesWithAutoStatus.size} paquetes total")
                            println("   🟢 Activos: ${activePackages.size}")
                            println("   ⚪ Inactivos: ${inactivePackages.size}")

                            if (packagesWithAutoStatus.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "No tienes paquetes registrados",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Black
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "¡Presiona el botón + para añadir uno nuevo!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 🟢 SECCIÓN DE PAQUETES ACTIVOS
                                    if (activePackages.isNotEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 12.dp)
                                            ) {
                                                Text(
                                                    "📦 Paquetes Activos (${activePackages.size})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00A76D)
                                                )
                                                Text(
                                                    "Los estados cambian automáticamente con el tiempo",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        items(activePackages) { pkg ->
                                            PackageItemWithTimeInfo(
                                                packageItem = applyAutoStatusTransition(pkg),
                                                onPackageClick = {
                                                    navController.navigate("package_detail/${pkg.id}")
                                                }
                                            )
                                        }
                                    }

                                    // ⚪ SECCIÓN DE PAQUETES INACTIVOS (solo si hay)
                                    if (inactivePackages.isNotEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 12.dp)
                                            ) {
                                                Divider(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    color = Color.LightGray,
                                                    thickness = 1.dp
                                                )
                                                Text(
                                                    "📄 Historial (${inactivePackages.size})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        items(inactivePackages) { pkg ->
                                            PackageItemSimplified(
                                                packageItem = pkg,
                                                onPackageClick = {
                                                    navController.navigate("package_detail/${pkg.id}")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is com.ucb.deliveryapp.util.Result.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Error al cargar los paquetes",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        state.exception.message ?: "Intenta nuevamente",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = verdeDelivery)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== FUNCIONES DE ORDENACIÓN Y TRANSICIÓN ====================

/**
 * Ordena los paquetes por estado y fecha de creación
 * Orden: EN TRÁNSITO → PENDIENTE → ENTREGADO → CANCELADO
 * Dentro de cada estado: más recientes primero
 */
private fun sortPackagesByStateAndDate(packages: List<Package>): List<Package> {
    return packages.sortedWith(compareByDescending<Package> {
        // PRIORIDAD 1: Estado (valor numérico para ordenar)
        when (it.status) {
            PackageStatus.IN_TRANSIT -> 4  // Primero
            PackageStatus.PENDING -> 3      // Segundo
            PackageStatus.DELIVERED -> 2    // Tercero
            PackageStatus.CANCELLED -> 1    // Último
            else -> 0
        }
    }.thenByDescending {
        // PRIORIDAD 2: Fecha de creación (más recientes primero)
        it.createdAt.seconds
    })
}

/**
 * Separa paquetes en activos (pendiente/en tránsito) e inactivos (entregado/cancelado)
 */
private fun separateActiveInactivePackages(packages: List<Package>): Pair<List<Package>, List<Package>> {
    val activePackages = packages.filter {
        it.status == PackageStatus.PENDING || it.status == PackageStatus.IN_TRANSIT
    }
    val inactivePackages = packages.filter {
        it.status == PackageStatus.DELIVERED || it.status == PackageStatus.CANCELLED
    }
    return Pair(activePackages, inactivePackages)
}

/**
 * Aplica transición automática de estado basado en el tiempo
 * PENDIENTE → EN TRÁNSITO después de 4 horas
 * EN TRÁNSITO → ENTREGADO después de 2 días
 */
private fun applyAutoStatusTransition(packageItem: Package): Package {
    // Si ya está entregado o cancelado, no cambiar
    if (packageItem.status == PackageStatus.DELIVERED ||
        packageItem.status == PackageStatus.CANCELLED) {
        return packageItem
    }

    val currentTime = Timestamp.now().seconds
    val createdAt = packageItem.createdAt.seconds
    val hoursSinceCreation = TimeUnit.SECONDS.toHours(currentTime - createdAt)

    val newStatus = when (packageItem.status) {
        PackageStatus.PENDING -> {
            // Después de 4 horas, pasa a EN TRÁNSITO
            if (hoursSinceCreation >= 4) {
                PackageStatus.IN_TRANSIT
            } else {
                PackageStatus.PENDING
            }
        }
        PackageStatus.IN_TRANSIT -> {
            // Después de 2 días (48 horas), pasa a ENTREGADO
            val daysSinceCreation = TimeUnit.SECONDS.toDays(currentTime - createdAt)
            if (daysSinceCreation >= 2) {
                PackageStatus.DELIVERED
            } else {
                PackageStatus.IN_TRANSIT
            }
        }
        else -> packageItem.status
    }

    // Solo retornar paquete modificado si cambió el estado
    return if (newStatus != packageItem.status) {
        packageItem.copy(status = newStatus)
    } else {
        packageItem
    }
}

/**
 * Obtiene información de tiempo para el próximo cambio de estado
 */
private fun getTimeUntilNextStatus(packageItem: Package): String {
    if (packageItem.status == PackageStatus.DELIVERED ||
        packageItem.status == PackageStatus.CANCELLED) {
        return ""
    }

    val currentTime = Timestamp.now().seconds
    val createdAt = packageItem.createdAt.seconds
    val hoursSinceCreation = TimeUnit.SECONDS.toHours(currentTime - createdAt)

    return when (packageItem.status) {
        PackageStatus.PENDING -> {
            val remainingHours = 4 - hoursSinceCreation
            if (remainingHours > 0) {
                "En preparación (~${remainingHours.toInt()}h)"
            } else {
                "Listo para envío"
            }
        }
        PackageStatus.IN_TRANSIT -> {
            val daysSinceCreation = TimeUnit.SECONDS.toDays(currentTime - createdAt)
            val remainingDays = 2 - daysSinceCreation
            if (remainingDays > 0) {
                "En camino (~${remainingDays.toInt()}d)"
            } else {
                "Próximo a entregar"
            }
        }
        else -> ""
    }
}

// ==================== COMPONENTES DE UI ====================

/**
 * Item de paquete con información de tiempo para próximos cambios
 */
@Composable
fun PackageItemWithTimeInfo(
    packageItem: Package,
    onPackageClick: () -> Unit
) {
    val timeInfo = getTimeUntilNextStatus(packageItem)

    Card(
        onClick = onPackageClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NÚMERO DE PAQUETE
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "📦 Paquete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = packageItem.trackingNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // BADGE DE ESTADO
                Box(
                    modifier = Modifier
                        .background(
                            color = getStatusColor(packageItem.status).copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono según estado
                        when (packageItem.status) {
                            PackageStatus.PENDING -> {
                                Text("⏳", modifier = Modifier.padding(end = 4.dp))
                            }
                            PackageStatus.IN_TRANSIT -> {
                                Text("🚚", modifier = Modifier.padding(end = 4.dp))
                            }
                            PackageStatus.DELIVERED -> {
                                Text("✓", modifier = Modifier.padding(end = 4.dp))
                            }
                            PackageStatus.CANCELLED -> {
                                Text("✗", modifier = Modifier.padding(end = 4.dp))
                            }
                        }

                        Text(
                            text = getStatusTextShort(packageItem.status),
                            style = MaterialTheme.typography.labelMedium,
                            color = getStatusColor(packageItem.status),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ✅ INDICADOR DE TIEMPO PARA PRÓXIMO ESTADO
            if (timeInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕐", modifier = Modifier.padding(end = 4.dp))
                    Text(
                        text = timeInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontStyle = if (timeInfo.contains("~")) androidx.compose.ui.text.font.FontStyle.Normal
                        else androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

/**
 * Item de paquete simplificado (para historial)
 */
@Composable
fun PackageItemSimplified(
    packageItem: Package,
    onPackageClick: () -> Unit
) {
    Card(
        onClick = onPackageClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NÚMERO DE PAQUETE
            Column {
                Text(
                    text = "📦 Paquete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = packageItem.trackingNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // BADGE DE ESTADO
            Box(
                modifier = Modifier
                    .background(
                        color = getStatusColor(packageItem.status).copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono según estado
                    when (packageItem.status) {
                        PackageStatus.PENDING -> {
                            Text("⏳", modifier = Modifier.padding(end = 4.dp))
                        }
                        PackageStatus.IN_TRANSIT -> {
                            Text("🚚", modifier = Modifier.padding(end = 4.dp))
                        }
                        PackageStatus.DELIVERED -> {
                            Text("✓", modifier = Modifier.padding(end = 4.dp))
                        }
                        PackageStatus.CANCELLED -> {
                            Text("✗", modifier = Modifier.padding(end = 4.dp))
                        }
                    }

                    Text(
                        text = getStatusTextShort(packageItem.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = getStatusColor(packageItem.status),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==================== FUNCIONES AUXILIARES ====================

/**
 * Texto abreviado para estado
 */
private fun getStatusTextShort(status: String): String {
    return when (status) {
        PackageStatus.PENDING -> "Pendiente"
        PackageStatus.IN_TRANSIT -> "En tránsito"
        PackageStatus.DELIVERED -> "Entregado"
        PackageStatus.CANCELLED -> "Cancelado"
        else -> "Desconocido"
    }
}

/**
 * Color según estado
 */
private fun getStatusColor(status: String): Color {
    return when (status) {
        PackageStatus.PENDING -> Color(0xFFFF9800) // Naranja
        PackageStatus.IN_TRANSIT -> Color(0xFF2196F3) // Azul
        PackageStatus.DELIVERED -> Color(0xFF4CAF50) // Verde
        PackageStatus.CANCELLED -> Color(0xFFF44336) // Rojo
        else -> Color.Gray
    }
}

/**
 * Función original para mantener compatibilidad
 */
@Composable
fun PackageItem(
    packageItem: Package,
    onPackageClick: () -> Unit
) {
    PackageItemSimplified(packageItem = packageItem, onPackageClick = onPackageClick)
}

/**
 * Función original para mantener compatibilidad
 */
private fun getStatusText(status: String): String {
    return when (status) {
        PackageStatus.PENDING -> "⏳ Pendiente"
        PackageStatus.IN_TRANSIT -> "🚚 En tránsito"
        PackageStatus.DELIVERED -> "✓ Entregado"
        PackageStatus.CANCELLED -> "✗ Cancelado"
        else -> "Desconocido"
    }
}