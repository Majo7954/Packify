// HomeScreen.kt
package com.ucb.deliveryapp.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackagePriority
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.ui.screens.MapboxMapView
import com.mapbox.geojson.Point
import com.ucb.deliveryapp.ui.screens.packages.PackageListActivity
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.getPackageViewModelFactory
import com.google.firebase.Timestamp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToMenu: () -> Unit, navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // LoginDataStore para obtener el userId
    val loginDataStore = remember { LoginDataStore(context) }

    // Obtener el ViewModel
    val packageViewModel: PackageViewModel = viewModel(
        factory = getPackageViewModelFactory(context)
    )

    var origin by remember { mutableStateOf("") }      // texto ingresado por usuario
    var destination by remember { mutableStateOf("") } // texto ingresado por usuario
    var weight by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var quotedPrice by remember { mutableStateOf("") }
    var withinDepartment by remember { mutableStateOf(false) }

    // Estado para el diálogo de confirmación
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showConfirmationScreen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Estado para el scroll
    val scrollState = rememberScrollState()

    // ✅ MEJOR MANEJO DE PERMISOS PARA PLAY STORE
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var shouldShowPermissionRationale by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        locationPermissionGranted = allGranted

        if (!allGranted) {
            shouldShowPermissionRationale = permissions.any { !it.value &&
                    ContextCompat.checkSelfPermission(context, it.key) == PackageManager.PERMISSION_DENIED }
        }
    }

    // Estado para userId dinámico
    var currentUserId by remember { mutableStateOf<String?>(null) }

    // Estados para ruta / ETA
    var routeInfoText by remember { mutableStateOf<String?>(null) }
    var originPoint by remember { mutableStateOf<Point?>(null) }
    var destinationPoint by remember { mutableStateOf<Point?>(null) }

    // ✅ EFECTO MEJORADO PARA PERMISOS
    LaunchedEffect(Unit) {
        // Obtener userId
        currentUserId = loginDataStore.getUserId() ?: "default_user"

        // Verificar permisos existentes
        locationPermissionGranted = hasLocationPermission(context)

        // Solicitar permisos si no se tienen
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // MOSTRAR PANTALLA DE CONFIRMACIÓN SI ESTÁ ACTIVA - DEBE IR ANTES DEL SCAFFOLD
    if (showConfirmationScreen) {
        ConfirmationScreen(
            navController = navController,
            onNavigateToPackages = {
                val intent = Intent(context, PackageListActivity::class.java)
                context.startActivity(intent)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            // ✅ TOP BAR CON COLOR VERDE #00A76D
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Página Principal",
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToMenu) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Menú",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00A76D) // ✅ VERDE SOLICITADO
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White) // ✅ FONDO BLANCO SOLICITADO
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // ✅ TÍTULO "INGRESA TU PAQUETE" ARRIBA DEL MAPA
                    Text(
                        text = "Ingresa tu paquete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // ✅ MEJOR MANEJO DE PERMISOS - EXPLICA EL USO
                    if (!locationPermissionGranted) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF80D4B6) // ✅ VERDE CLARITO SOLICITADO
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Configuración",
                                    tint = Color.Black // ✅ ICONO NEGRO
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Permisos de ubicación",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black // ✅ TEXTO NEGRO
                                    )
                                    Text(
                                        "Necesarios para mostrar tu ubicación en el mapa y optimizar rutas de entrega",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black // ✅ TEXTO NEGRO
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (shouldShowPermissionRationale) {
                                            // Abrir configuración si el usuario denegó permisos permanentemente
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            locationPermissionLauncher.launch(arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00A76D) // ✅ VERDE PRINCIPAL
                                    )
                                ) {
                                    Text(
                                        "Activar",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // ✅ MAPA CON MAPBOX Y MANEJO DE FALLBACK
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (locationPermissionGranted) {
                            // MapboxMapView toma origin/destination como Points; onRouteInfo recibe ETA y distancia
                            MapboxMapView(
                                modifier = Modifier.fillMaxSize(),
                                origin = originPoint,
                                destination = destinationPoint,
                                onRouteInfo = { etaMinutes, distanceKm ->
                                    routeInfoText = "ETA ≈ ${etaMinutes} min • ${"%.2f".format(distanceKm)} km"
                                }
                            )
                        } else {
                            // Fallback cuando no hay permisos
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF80D4B6)), // ✅ VERDE CLARITO SOLICITADO
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Configurar ubicación",
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.Black // ✅ ICONO NEGRO
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Activa los permisos de ubicación",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black // ✅ TEXTO NEGRO
                                    )
                                    Text(
                                        "Para ver el mapa y tu ubicación actual",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black // ✅ TEXTO NEGRO
                                    )
                                }
                            }
                        }
                    }

                    // Mostrar info de ruta si existe
                    routeInfoText?.let { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.Black // ✅ TEXTO NEGRO
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ CAMPOS DEL FORMULARIO CON NUEVOS COLORES
                    EditFieldCard(
                        label = "Lugar de origen (lat,lng o lng,lat)",
                        value = origin,
                        onValueChange = { origin = it },
                        onClear = {
                            origin = ""
                            originPoint = null
                            routeInfoText = null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EditFieldCard(
                        label = "Lugar de destino (lat,lng o lng,lat)",
                        value = destination,
                        onValueChange = { destination = it },
                        onClear = {
                            destination = ""
                            destinationPoint = null
                            routeInfoText = null
                        }
                    )

                    // Botón para convertir los strings a Points y pedir ruta
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                // Intentar parsear los campos a coordenadas
                                val parsedOrigin = parseCoordinatesFromString(origin)
                                val parsedDest = parseCoordinatesFromString(destination)
                                if (parsedOrigin != null && parsedDest != null) {
                                    originPoint = parsedOrigin
                                    destinationPoint = parsedDest
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No se pudieron parsear las coordenadas. Usa formato 'lat,lng' o 'lng,lat' (ej: -16.5,-68.1).")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A76D) // ✅ VERDE PRINCIPAL
                            )
                        ) {
                            Text("Calcular ruta", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                // limpiar
                                origin = ""
                                destination = ""
                                originPoint = null
                                destinationPoint = null
                                routeInfoText = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Limpiar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resto de campos editables
                    EditFieldCard(
                        label = "Peso (kg)",
                        value = weight,
                        onValueChange = { weight = it },
                        onClear = { weight = "" },
                        isNumber = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EditFieldCard(
                        label = "Tamaño",
                        value = size,
                        onValueChange = { size = it },
                        onClear = { size = "" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EditFieldCard(
                        label = "Precio Cotizado",
                        value = quotedPrice,
                        onValueChange = { quotedPrice = it },
                        onClear = { quotedPrice = "" },
                        isNumber = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ SWITCH CON NUEVOS COLORES
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Envío dentro del departamento:",
                            color = Color.Black // ✅ TEXTO NEGRO
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = withinDepartment,
                            onCheckedChange = { withinDepartment = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ BOTÓN SIEMPRE ACTIVADO CON COLOR AMARILLO #FAC10C
                    Button(
                        onClick = {
                            // ✅ VALIDAR CAMPOS SOLO AL HACER CLICK
                            if (validateInput(origin, destination, weight)) {
                                showConfirmationDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("❌ Completa origen, destino y peso correctamente")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        enabled = true, // ✅ SIEMPRE ACTIVADO
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFAC10C) // ✅ AMARILLO SOLICITADO
                        )
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Creando paquete...",
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                "Confirmar Envío",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )

    // ✅ DIALOGO DE CONFIRMACIÓN MEJORADO
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoading) {
                    showConfirmationDialog = false
                }
            },
            title = { Text("Confirmar Envío") },
            text = {
                Column {
                    Text("¿Estás seguro de que quieres registrar este paquete?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Origen: $origin\nDestino: $destination\nPeso: $weight kg",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        isLoading = true

                        currentUserId?.let { userId ->
                            try {
                                val newPackage = createPackageFromForm(
                                    origin, destination, weight, size,
                                    quotedPrice, withinDepartment, userId
                                )
                                packageViewModel.createPackage(newPackage)

                                // Simular éxito después de crear
                                scope.launch {
                                    delay(1000) // Simular tiempo de procesamiento
                                    isLoading = false
                                    showConfirmationScreen = true
                                    snackbarHostState.showSnackbar("✅ Paquete creado exitosamente")
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("❌ Error al crear el paquete: ${e.message}")
                                }
                            }
                        } ?: run {
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Error: No se pudo identificar al usuario")
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirmar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false },
                    enabled = !isLoading
                ) { Text("Cancelar") }
            }
        )
    }
}

// Función para verificar permisos de ubicación
private fun hasLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFieldCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    isNumber: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF80D4B6) // ✅ VERDE CLARITO SOLICITADO
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black // ✅ TEXTO NEGRO
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        if (isNumber) {
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onValueChange(newValue)
                            }
                        } else {
                            onValueChange(newValue)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Ingresa $label",
                            color = Color.Gray // ✅ PLACEHOLDER GRIS
                        )
                    },
                    singleLine = true,
                    isError = isNumber && value.isNotBlank() && !value.matches(Regex("^\\d*\\.?\\d*$")),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black, // ✅ TEXTO NEGRO AL ESCRIBIR
                        unfocusedTextColor = Color.Black, // ✅ TEXTO NEGRO
                        focusedBorderColor = Color.White, // ✅ BORDE BLANCO AL FOCUS
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f), // ✅ BORDE BLANCO
                        focusedContainerColor = Color.White, // ✅ FONDO BLANCO AL FOCUS
                        unfocusedContainerColor = Color.White, // ✅ FONDO BLANCO
                        cursorColor = Color.Black, // ✅ CURSOR NEGRO
                        focusedPlaceholderColor = Color.Gray, // ✅ PLACEHOLDER GRIS
                        unfocusedPlaceholderColor = Color.Gray, // ✅ PLACEHOLDER GRIS
                        errorBorderColor = Color.Red, // ✅ BORDE ROJO EN ERROR
                        errorContainerColor = Color.White, // ✅ FONDO BLANCO EN ERROR
                        errorCursorColor = Color.Black, // ✅ CURSOR NEGRO EN ERROR
                        errorTextColor = Color.Black, // ✅ TEXTO NEGRO EN ERROR
                        errorPlaceholderColor = Color.Gray // ✅ PLACEHOLDER GRIS EN ERROR
                    )
                )
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpiar $label",
                            tint = Color.Black // ✅ ICONO NEGRO
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------
   Helper: parsear coordenadas
   - acepta "lat,lng" o "lng,lat"
   - intenta detectar orden por rangos válidos
   --------------------------- */
private fun parseCoordinatesFromString(s: String?): Point? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.trim().replace("\\s+".toRegex(), "")
    val parts = cleaned.split(",")
    if (parts.size < 2) return null
    val a = parts[0].toDoubleOrNull() ?: return null
    val b = parts[1].toDoubleOrNull() ?: return null

    // if a in [-90,90] -> likely latitude, so order is lat,lng
    return if (a in -90.0..90.0 && b in -180.0..180.0) {
        Point.fromLngLat(b, a) // Point expects (lng, lat)
    } else if (a in -180.0..180.0 && b in -90.0..90.0) {
        Point.fromLngLat(a, b)
    } else {
        // ambos válidos como long/lat ranges, fallback assume lat,lng
        Point.fromLngLat(b, a)
    }
}

// ✅ FUNCIÓN MEJORADA CON MANEJO DE ERRORES
private fun createPackageFromForm(
    origin: String,
    destination: String,
    weight: String,
    size: String,
    quotedPrice: String,
    withinDepartment: Boolean,
    userId: String
): Package {

    val priority = if (withinDepartment) PackagePriority.NORMAL else PackagePriority.EXPRESS

    val notes = buildString {
        append("Origen: $origin")
        append("\nDestino: $destination")
        if (size.isNotBlank()) {
            append("\nTamaño: $size")
        }
        if (quotedPrice.isNotBlank()) {
            append("\nPrecio cotizado: $quotedPrice")
        }
        append(if (withinDepartment) "\nEnvío dentro del departamento" else "\nEnvío nacional")
    }

    // ✅ MANEJO SEGURO DE CONVERSIÓN DE PESO
    val weightValue = try {
        weight.toDouble()
    } catch (e: NumberFormatException) {
        0.0 // Valor por defecto seguro
    }

    return Package(
        trackingNumber = generateTrackingNumber(),
        senderName = "Usuario Actual",
        recipientName = "Destinatario en $destination",
        recipientAddress = destination,
        recipientPhone = "Por definir",
        weight = weightValue,
        status = PackageStatus.PENDING,
        priority = priority,
        estimatedDeliveryDate = Timestamp.now(),
        createdAt = Timestamp.now(),
        deliveredAt = null,
        notes = notes,
        userId = userId
    )
}

// Generar número de seguimiento único
private fun generateTrackingNumber(): String {
    val timestamp = System.currentTimeMillis().toString().takeLast(8)
    val random = (1000..9999).random()
    return "UCB${timestamp}${random}"
}

@Composable
fun CreatePackageComposeScreen(
    onNavigateToMenu: () -> Unit,
    navController: NavController
) {
    HomeScreen(
        onNavigateToMenu = onNavigateToMenu,
        navController = navController
    )
}

// Validar campos obligatorios
private fun validateInput(
    origin: String,
    destination: String,
    weight: String
): Boolean {
    return origin.isNotBlank() &&
            destination.isNotBlank() &&
            weight.isNotBlank() &&
            try {
                weight.toDouble() > 0
            } catch (e: NumberFormatException) {
                false
            }
}