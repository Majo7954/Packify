// PackageListScreen.kt
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.getPackageViewModelFactory
import kotlinx.coroutines.launch

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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
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
                            val packages = state.data
                            println("✅ DEBUG: Se cargaron ${packages.size} paquetes")

                            if (packages.isEmpty()) {
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
                                    items(packages) { pkg ->
                                        PackageItem(
                                            packageItem = pkg,
                                            onPackageClick = {
                                                navController.navigate("package_detail/${pkg.id}")
                                            }
                                        )
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

// En PackageListScreen.kt - MEJORAR PackageItem
@Composable
fun PackageItem(
    packageItem: com.ucb.deliveryapp.data.entity.Package,
    onPackageClick: () -> Unit
) {
    Card(
        onClick = onPackageClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con tracking number y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 Nº ${packageItem.trackingNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                // Badge de estado
                Box(
                    modifier = Modifier
                        .background(
                            color = getStatusColor(packageItem.status).copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = getStatusText(packageItem.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(packageItem.status),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Información básica
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Para: ${packageItem.recipientName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "${packageItem.weight} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun getStatusText(status: String): String {
    return when (status) {
        com.ucb.deliveryapp.data.entity.PackageStatus.PENDING -> "⏳ Pendiente"
        com.ucb.deliveryapp.data.entity.PackageStatus.IN_TRANSIT -> "🚚 En tránsito"
        com.ucb.deliveryapp.data.entity.PackageStatus.DELIVERED -> "✓ Entregado"
        com.ucb.deliveryapp.data.entity.PackageStatus.CANCELLED -> "✗ Cancelado"
        else -> "Desconocido"
    }
}

private fun getStatusColor(status: String): Color {
    return when (status) {
        com.ucb.deliveryapp.data.entity.PackageStatus.PENDING -> Color(0xFFFF9800)
        com.ucb.deliveryapp.data.entity.PackageStatus.IN_TRANSIT -> Color(0xFF2196F3)
        com.ucb.deliveryapp.data.entity.PackageStatus.DELIVERED -> Color(0xFF4CAF50)
        com.ucb.deliveryapp.data.entity.PackageStatus.CANCELLED -> Color(0xFFF44336)
        else -> Color.Gray
    }
}