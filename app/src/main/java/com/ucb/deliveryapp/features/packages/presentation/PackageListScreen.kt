package com.ucb.deliveryapp.features.packages.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.core.util.Result
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.model.PackageStatus
import com.ucb.deliveryapp.core.datastore.LoginDataStore
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.TimeUnit
import com.ucb.deliveryapp.navigation.Routes

val verdeDelivery = Color(0xFF00A76D)
val amarilloDelivery = Color(0xFFFAC10C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageListScreen(navController: NavController) {
    val context = LocalContext.current

    val viewModel: PackageViewModel = koinViewModel()

    val coroutineScope = rememberCoroutineScope()

    val loginDataStore = remember { LoginDataStore(context) }

    val packagesState by viewModel.packagesState.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()

    var userId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userId = loginDataStore.getUserId()
        println("🔄 DEBUG: UserId obtenido: $userId")

        if (userId != null) {
            viewModel.loadUserPackages(userId!!)
        } else {
            println("DEBUG: No se pudo obtener el userId")
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
                    IconButton(onClick = {
                        navController.navigate(Routes.MENU) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }) {
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
                        is Result.Success -> {
                            val sortedPackages = sortPackagesByStateAndDate(state.data)

                            val (activePackages, inactivePackages) = separateActiveInactivePackages(sortedPackages)

                            val packagesWithAutoStatus = sortedPackages.map { pkg ->
                                applyAutoStatusTransition(pkg)
                            }

                            println("DEBUG: ${packagesWithAutoStatus.size} paquetes total")
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
                        is Result.Error -> {
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

private fun sortPackagesByStateAndDate(packages: List<Package>): List<Package> {
    return packages.sortedWith(compareByDescending<Package> {
        when (it.status) {
            PackageStatus.IN_TRANSIT -> 4
            PackageStatus.PENDING -> 3
            PackageStatus.DELIVERED -> 2
            PackageStatus.CANCELLED -> 1
            else -> 0
        }
    }.thenByDescending {
        it.createdAtMillis
    })
}

private fun separateActiveInactivePackages(packages: List<Package>): Pair<List<Package>, List<Package>> {
    val activePackages = packages.filter {
        it.status == PackageStatus.PENDING || it.status == PackageStatus.IN_TRANSIT
    }
    val inactivePackages = packages.filter {
        it.status == PackageStatus.DELIVERED || it.status == PackageStatus.CANCELLED
    }
    return Pair(activePackages, inactivePackages)
}

private fun applyAutoStatusTransition(packageItem: Package): Package {
    if (packageItem.status == PackageStatus.DELIVERED ||
        packageItem.status == PackageStatus.CANCELLED) {
        return packageItem
    }

    val currentTime = System.currentTimeMillis()
    val createdAt = packageItem.createdAtMillis
    val hoursSinceCreation = TimeUnit.SECONDS.toHours(currentTime - createdAt)

    val newStatus = when (packageItem.status) {
        PackageStatus.PENDING -> {
            if (hoursSinceCreation >= 4) {
                PackageStatus.IN_TRANSIT
            } else {
                PackageStatus.PENDING
            }
        }
        PackageStatus.IN_TRANSIT -> {
            val daysSinceCreation = TimeUnit.SECONDS.toDays(currentTime - createdAt)
            if (daysSinceCreation >= 2) {
                PackageStatus.DELIVERED
            } else {
                PackageStatus.IN_TRANSIT
            }
        }
        else -> packageItem.status
    }

    return if (newStatus != packageItem.status) {
        packageItem.copy(status = newStatus)
    } else {
        packageItem
    }
}

private fun getTimeUntilNextStatus(packageItem: Package): String {
    if (packageItem.status == PackageStatus.DELIVERED ||
        packageItem.status == PackageStatus.CANCELLED) {
        return ""
    }

    val currentTime = System.currentTimeMillis()
    val createdAt = packageItem.createdAtMillis
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
        shape = RoundedCornerShape(12.dp)
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
                        fontStyle = if (timeInfo.contains("~")) FontStyle.Normal
                        else FontStyle.Italic
                    )
                }
            }
        }
    }
}

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

private fun getStatusTextShort(status: String): String {
    return when (status) {
        PackageStatus.PENDING -> "Pendiente"
        PackageStatus.IN_TRANSIT -> "En tránsito"
        PackageStatus.DELIVERED -> "Entregado"
        PackageStatus.CANCELLED -> "Cancelado"
        else -> "Desconocido"
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        PackageStatus.PENDING -> Color(0xFFFF9800)
        PackageStatus.IN_TRANSIT -> Color(0xFF2196F3)
        PackageStatus.DELIVERED -> Color(0xFF4CAF50)
        PackageStatus.CANCELLED -> Color(0xFFF44336)
        else -> Color.Gray
    }
}


@Composable
fun PackageItem(
    packageItem: Package,
    onPackageClick: () -> Unit
) {
    PackageItemSimplified(packageItem = packageItem, onPackageClick = onPackageClick)
}

private fun getStatusText(status: String): String {
    return when (status) {
        PackageStatus.PENDING -> "⏳ Pendiente"
        PackageStatus.IN_TRANSIT -> "🚚 En tránsito"
        PackageStatus.DELIVERED -> "✓ Entregado"
        PackageStatus.CANCELLED -> "✗ Cancelado"
        else -> "Desconocido"
    }
}