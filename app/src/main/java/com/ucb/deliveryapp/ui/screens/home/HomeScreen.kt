// HomeScreen.kt
package com.ucb.deliveryapp.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToMenu: () -> Unit) {
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

    // Estado para el scroll
    val scrollState = rememberScrollState()

    // Estados para permisos de ubicación
    var locationPermissionGranted by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.all { it.value }
        if (!locationPermissionGranted) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Los permisos de ubicación son necesarios para una mejor experiencia"
                )
            }
        }
    }

    // Estado para userId dinámico
    var currentUserId by remember { mutableStateOf<String?>(null) }

    // Estados para ruta / ETA
    var routeInfoText by remember { mutableStateOf<String?>(null) }
    var originPoint by remember { mutableStateOf<Point?>(null) }
    var destinationPoint by remember { mutableStateOf<Point?>(null) }

    // Obtener el userId del usuario logueado
    LaunchedEffect(Unit) {
        // Obtener userId
        val userIdString = loginDataStore.getUserId()
        currentUserId = userIdString ?: "default_user"

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
            onNavigateToPackages = {
                val intent = Intent(context, PackageListActivity::class.java)
                context.startActivity(intent)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Página Principal") },
                actions = {
                    IconButton(onClick = onNavigateToMenu) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Menú"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Mostrar mensaje si no hay permisos de ubicación
                    if (!locationPermissionGranted) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Advertencia",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Se necesitan permisos de ubicación para mostrar el mapa correctamente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // MAPA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        // MapboxMapView toma origin/destination como Points; onRouteInfo recibe ETA y distancia
                        MapboxMapView(
                            modifier = Modifier.fillMaxSize(),
                            origin = originPoint,
                            destination = destinationPoint,
                            onRouteInfo = { etaMinutes, distanceKm ->
                                routeInfoText = "ETA ≈ ${etaMinutes} min • ${"%.2f".format(distanceKm)} km"
                            }
                        )
                    }

                    // Mostrar info de ruta si existe
                    routeInfoText?.let { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campos editables: origen y destino (simple: acepta "lat,lng" o "lng,lat" o texto libre)
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Calcular ruta")
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Envío dentro del departamento:")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = withinDepartment, onCheckedChange = { withinDepartment = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BOTÓN CONFIRMAR ENVÍO
                    Button(
                        onClick = {
                            if (validateInput(origin, destination, weight)) {
                                showConfirmationDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Completa origen, destino y peso correctamente")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        enabled = validateInput(origin, destination, weight)
                    ) {
                        Text("Confirmar Envío")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )

    // Diálogo de confirmación de envío
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirmar Envío") },
            text = {
                Text("¿Estás seguro de que quieres registrar este paquete?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false

                        // Usar userId String
                        currentUserId?.let { userId ->
                            // Crear y guardar el paquete en la base de datos
                            val newPackage = createPackageFromForm(
                                origin = origin,
                                destination = destination,
                                weight = weight,
                                size = size,
                                quotedPrice = quotedPrice,
                                withinDepartment = withinDepartment,
                                userId = userId
                            )

                            packageViewModel.createPackage(newPackage)

                            // MOSTRAR PANTALLA DE CONFIRMACIÓN
                            showConfirmationScreen = true

                            // Limpiar formulario después de guardar
                            origin = ""
                            destination = ""
                            weight = ""
                            size = ""
                            quotedPrice = ""
                            withinDepartment = false
                            originPoint = null
                            destinationPoint = null
                            routeInfoText = null
                        } ?: run {
                            scope.launch {
                                snackbarHostState.showSnackbar("❌ Error: No se pudo identificar al usuario")
                            }
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text("Cancelar")
                }
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

/* ---------------------------
   EditFieldCard (SIN CAMBIOS)
   --------------------------- */
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

/* ---------------------------
   Funciones auxiliares (sin cambios)
   --------------------------- */

// Función para crear Package con userId String y Timestamp
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

    return Package(
        trackingNumber = generateTrackingNumber(),
        senderName = "Usuario Actual",
        recipientName = "Destinatario en $destination",
        recipientAddress = destination,
        recipientPhone = "Por definir",
        weight = weight.toDouble(),
        status = PackageStatus.PENDING,
        priority = priority,
        estimatedDeliveryDate = Timestamp.now(),
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
