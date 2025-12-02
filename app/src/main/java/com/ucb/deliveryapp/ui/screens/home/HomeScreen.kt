// HomeScreen.kt - VERSIÓN COMPLETA
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
import androidx.compose.material.icons.filled.LocationOn
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
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackagePriority
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.data.local.LoginDataStore
import com.ucb.deliveryapp.ui.screens.MapboxMapView
import com.mapbox.geojson.Point
import androidx.navigation.NavController
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.getPackageViewModelFactory
import com.google.firebase.Timestamp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToMenu: () -> Unit, navController: NavController) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val loginDataStore = remember { LoginDataStore(context) }
    val packageViewModel: PackageViewModel = viewModel(factory = getPackageViewModelFactory(context))

    var currentLocation by remember { mutableStateOf<Point?>(null) }
    var isGettingLocation by remember { mutableStateOf(false) }

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var quotedPrice by remember { mutableStateOf("") }
    var withinDepartment by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showConfirmationScreen by remember { mutableStateOf(false) }
    var showMapForDestinationSelection by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // ✅ MEJOR MANEJO DE PERMISOS
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var shouldShowPermissionRationale by remember { mutableStateOf(false) }

    // ✅ ESTADOS PARA MAPBOX
    var routeInfoText by remember { mutableStateOf<String?>(null) }
    var originPoint by remember { mutableStateOf<Point?>(null) }
    var destinationPoint by remember { mutableStateOf<Point?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        locationPermissionGranted = allGranted

        if (allGranted) {
            scope.launch {
                getCurrentLocationWithChecks(context)?.let { location ->
                    currentLocation = Point.fromLngLat(location.longitude, location.latitude)
                    originPoint = currentLocation
                    origin = "${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}"
                }
            }
        } else {
            shouldShowPermissionRationale = permissions.any { !it.value &&
                    ContextCompat.checkSelfPermission(context, it.key) == PackageManager.PERMISSION_DENIED }
        }
    }

    var currentUserId by remember { mutableStateOf<String?>(null) }

    // ✅ EFECTO MEJORADO
    LaunchedEffect(Unit) {
        currentUserId = loginDataStore.getUserId() ?: "default_user"

        locationPermissionGranted = hasLocationPermission(context)

        if (locationPermissionGranted) {
            isGettingLocation = true
            getCurrentLocationWithChecks(context)?.let { location ->
                currentLocation = Point.fromLngLat(location.longitude, location.latitude)
                originPoint = currentLocation
                origin = "${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}"
            }
            isGettingLocation = false
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    if (showConfirmationScreen) {
        // Si tienes ConfirmationScreen, descomenta esto:
        // ConfirmationScreen(navController = navController, onNavigateToPackages = {})
        // Por ahora, solo retorna un texto
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Paquete creado exitosamente")
        }
        return
    }

    // ✅ DIALOGO PARA SELECCIÓN EN MAPA
    if (showMapForDestinationSelection) {
        AlertDialog(
            onDismissRequest = { showMapForDestinationSelection = false },
            title = {
                Text(
                    "📍 Selecciona el destino en el mapa",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A76D)
                )
            },
            text = {
                Column {
                    Text(
                        "Toca en el mapa para seleccionar el punto de entrega",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        MapboxMapView(
                            modifier = Modifier.fillMaxSize(),
                            origin = originPoint,
                            allowDestinationSelection = true,
                            onDestinationSelected = { point ->
                                destinationPoint = point
                                destination = "${"%.6f".format(point.latitude())}, ${"%.6f".format(point.longitude())}"
                                showMapForDestinationSelection = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("📍 Destino seleccionado")
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMapForDestinationSelection = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF00A76D)
                    )
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
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
                            Icons.Default.List,
                            contentDescription = "Menú",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00A76D)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White)
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "Ingresa tu paquete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    if (!locationPermissionGranted) {
                        PermissionWarningCard(
                            onActivate = {
                                if (shouldShowPermissionRationale) {
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
                            }
                        )
                    }

                    // ✅ MAPA PRINCIPAL
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (locationPermissionGranted) {
                            if (isGettingLocation) {
                                LoadingMapPlaceholder()
                            } else {
                                MapboxMapView(
                                    modifier = Modifier.fillMaxSize(),
                                    origin = originPoint,
                                    destination = destinationPoint,
                                    onRouteInfo = { etaMinutes, distanceKm ->
                                        routeInfoText = "ETA ≈ ${etaMinutes} min • ${"%.2f".format(distanceKm)} km"
                                    }
                                )
                            }
                        } else {
                            NoPermissionMapPlaceholder()
                        }
                    }

                    // ✅ INFO DE RUTA
                    routeInfoText?.let { info ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF00A76D),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ UBICACIÓN ACTUAL
                    CurrentLocationCard(
                        location = origin,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ DESTINO
                    DestinationSelectionCard(
                        destination = destination,
                        onTextChange = { destination = it },
                        onClear = {
                            destination = ""
                            destinationPoint = null
                            routeInfoText = null
                        },
                        onSelectOnMap = {
                            if (originPoint != null) {
                                showMapForDestinationSelection = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("📍 Espera a obtener tu ubicación primero")
                                }
                            }
                        }
                    )

                    // ✅ BOTONES PARA CALCULAR RUTA
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (destinationPoint != null && originPoint != null) {
                                    // La ruta se calcula automáticamente cuando ambos puntos están definidos
                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ Calculando ruta...")
                                    }
                                } else {
                                    val parsedDest = parseCoordinatesFromString(destination)
                                    if (originPoint != null && parsedDest != null) {
                                        destinationPoint = parsedDest
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("❌ Selecciona un destino primero")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A76D)
                            )
                        ) {
                            Text("Calcular ruta", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                destination = ""
                                destinationPoint = null
                                routeInfoText = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Destino limpiado")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00A76D)
                            )
                        ) {
                            Text("Limpiar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ RESTANTE DEL FORMULARIO
                    EditFieldCard("Peso (kg)", weight, onValueChange = { weight = it }, onClear = { weight = "" }, isNumber = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    EditFieldCard("Tamaño", size, onValueChange = { size = it }, onClear = { size = "" })
                    Spacer(modifier = Modifier.height(8.dp))
                    EditFieldCard("Precio Cotizado", quotedPrice, onValueChange = { quotedPrice = it }, onClear = { quotedPrice = "" }, isNumber = true)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Envío dentro del departamento:",
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = withinDepartment,
                            onCheckedChange = { withinDepartment = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
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
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFAC10C)
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
                                Text("Creando paquete...", color = Color.White)
                            }
                        } else {
                            Text("Confirmar Envío", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )

    // ✅ DIALOGO DE CONFIRMACIÓN
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmationDialog = false },
            title = { Text("Confirmar Envío", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Estás seguro de que quieres registrar este paquete?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Origen: $origin\nDestino: $destination\nPeso: $weight kg",
                        style = MaterialTheme.typography.bodySmall)
                    routeInfoText?.let { info ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ruta: $info",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00A76D))
                    }
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
                                scope.launch {
                                    delay(1000)
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
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ✅ COMPONENTES AUXILIARES - AGREGAR AL FINAL DEL ARCHIVO

@Composable
private fun PermissionWarningCard(onActivate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF80D4B6))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Permisos de ubicación",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    "Necesarios para mostrar tu ubicación en el mapa y optimizar rutas de entrega",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A76D)
                )
            ) {
                Text("Activar", color = Color.White)
            }
        }
    }
}

@Composable
private fun LoadingMapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF80D4B6)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Obteniendo tu ubicación...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun NoPermissionMapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF80D4B6)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Configurar ubicación",
                modifier = Modifier.size(48.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Activa los permisos de ubicación",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            Text(
                "Para ver el mapa y tu ubicación actual",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentLocationCard(
    location: String,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF80D4B6))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📍 Tu ubicación actual",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Obteniendo ubicación...", color = Color.Gray) },
                    singleLine = true,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                    )
                )
                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("📍 Ubicación actual: $location")
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Ubicación actual", tint = Color.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationSelectionCard(
    destination: String,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSelectOnMap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF80D4B6))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🎯 Lugar de destino",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSelectOnMap,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Seleccionar en mapa",
                        tint = Color.Black
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: -16.5000, -68.1500", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                    )
                )
                if (destination.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpiar",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF80D4B6))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ingresa $label", color = Color.Gray) },
                    singleLine = true,
                    isError = isNumber && value.isNotBlank() && !value.matches(Regex("^\\d*\\.?\\d*$")),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        errorBorderColor = Color.Red,
                        errorContainerColor = Color.White,
                        errorCursorColor = Color.Black,
                        errorTextColor = Color.Black,
                        errorPlaceholderColor = Color.Gray
                    )
                )
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Black)
                    }
                }
            }
        }
    }
}

// ✅ FUNCIÓN CORREGIDA PARA OBTENER UBICACIÓN
private suspend fun getCurrentLocationWithChecks(context: android.content.Context): Location? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFineLocation && !hasCoarseLocation) {
        return null
    }

    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val priority = Priority.PRIORITY_HIGH_ACCURACY
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            priority,
            cancellationTokenSource.token
        ).await()
    } catch (e: SecurityException) {
        android.util.Log.e("LocationHelper", "SecurityException: Permisos revocados", e)
        null
    } catch (e: Exception) {
        android.util.Log.e("LocationHelper", "Error obteniendo ubicación", e)
        null
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

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

// ✅ FUNCIÓN PARA CREAR PAQUETE
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

    val weightValue = try {
        weight.toDouble()
    } catch (e: NumberFormatException) {
        0.0
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

private fun generateTrackingNumber(): String {
    val timestamp = System.currentTimeMillis().toString().takeLast(8)
    val random = (1000..9999).random()
    return "UCB${timestamp}${random}"
}

private fun validateInput(origin: String, destination: String, weight: String): Boolean {
    return origin.isNotBlank() && destination.isNotBlank() && weight.isNotBlank() &&
            try {
                weight.toDouble() > 0
            } catch (e: NumberFormatException) {
                false
            }
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