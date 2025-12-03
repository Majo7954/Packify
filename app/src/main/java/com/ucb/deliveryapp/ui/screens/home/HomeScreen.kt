// HomeScreen.kt - VERSIÓN CON VALIDACIONES COMPLETAS PARA PLAY STORE
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
import com.ucb.deliveryapp.ui.navigation.Routes

// ==================== CLASES PARA VALIDACIÓN ====================

/**
 * Resultado de validación con mensajes específicos
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Datos del formulario para validar
 */
data class PackageFormData(
    val origin: String,
    val destination: String,
    val weight: String,
    val size: String = "",
    val quotedPrice: String = "",
    val withinDepartment: Boolean = false
)

// ==================== FUNCIONES DE VALIDACIÓN ====================

/**
 * Valida formato de coordenadas (latitud, longitud)
 */
private fun isValidCoordinateFormat(coordinate: String): Boolean {
    return try {
        val parts = coordinate.split(",")
        if (parts.size != 2) return false

        val lat = parts[0].trim().toDouble()
        val lng = parts[1].trim().toDouble()

        // Coordenadas válidas: latitud entre -90 y 90, longitud entre -180 y 180
        lat in -90.0..90.0 && lng in -180.0..180.0
    } catch (e: Exception) {
        false
    }
}

/**
 * Valida que sea un número positivo
 */
private fun isValidPositiveNumber(value: String, maxValue: Double = Double.MAX_VALUE): Boolean {
    return try {
        val num = value.toDouble()
        num > 0 && num <= maxValue
    } catch (e: NumberFormatException) {
        false
    }
}

/**
 * Valida que sea un número decimal válido (para precio)
 */
private fun isValidDecimalNumber(value: String): Boolean {
    return try {
        value.toDouble()
        true
    } catch (e: NumberFormatException) {
        false
    }
}

/**
 * Valida que el tamaño no exceda límites razonables
 */
private fun isValidSize(size: String): Boolean {
    return size.length <= 50 // Máximo 50 caracteres
}

/**
 * Valida todos los campos del formulario de paquete
 */
private fun validatePackageForm(data: PackageFormData): ValidationResult {
    // 1. Validar origen (obligatorio y formato coordenadas)
    if (data.origin.isBlank()) {
        return ValidationResult.Error("El origen es obligatorio")
    }
    if (!isValidCoordinateFormat(data.origin)) {
        return ValidationResult.Error("Formato de origen inválido.\nEjemplo: -16.5000, -68.1500")
    }

    // 2. Validar destino (obligatorio y formato coordenadas)
    if (data.destination.isBlank()) {
        return ValidationResult.Error("El destino es obligatorio")
    }
    if (!isValidCoordinateFormat(data.destination)) {
        return ValidationResult.Error("Formato de destino inválido.\nEjemplo: -16.5000, -68.1500")
    }

    // 3. Validar que origen y destino no sean iguales
    if (data.origin.trim() == data.destination.trim()) {
        return ValidationResult.Error("El origen y destino no pueden ser iguales")
    }

    // 4. Validar peso (obligatorio, positivo, límite 100kg)
    if (data.weight.isBlank()) {
        return ValidationResult.Error("El peso es obligatorio")
    }
    if (!isValidPositiveNumber(data.weight, 100.0)) {
        return ValidationResult.Error("El peso debe ser un número positivo\nMáximo 100 kg")
    }

    // 5. Validar tamaño (opcional pero con límites)
    if (data.size.isNotBlank() && !isValidSize(data.size)) {
        return ValidationResult.Error("El tamaño es demasiado largo\nMáximo 50 caracteres")
    }

    // 6. Validar precio cotizado (opcional pero válido)
    if (data.quotedPrice.isNotBlank()) {
        if (!isValidDecimalNumber(data.quotedPrice)) {
            return ValidationResult.Error("El precio debe ser un número válido")
        }
        try {
            val price = data.quotedPrice.toDouble()
            if (price < 0) {
                return ValidationResult.Error("El precio no puede ser negativo")
            }
            if (price > 10000) { // Límite razonable de precio
                return ValidationResult.Error("El precio máximo es $10,000")
            }
        } catch (e: Exception) {
            return ValidationResult.Error("Formato de precio inválido")
        }
    }

    return ValidationResult.Success
}

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

    // Estados para validación en tiempo real
    var originError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var sizeError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

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
                    // Limpiar error al obtener ubicación automática
                    originError = null
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
                originError = null
            }
            isGettingLocation = false
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Eliminar el bloque de showConfirmationScreen (ya no se usa)
    // if (showConfirmationScreen) { ... }

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
                                // Validar destino automáticamente
                                if (isValidCoordinateFormat(destination)) {
                                    destinationError = null
                                }
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

                    // ✅ UBICACIÓN ACTUAL CON VALIDACIÓN
                    CurrentLocationCardWithValidation(
                        location = origin,
                        error = originError,
                        snackbarHostState = snackbarHostState,
                        scope = scope,
                        onLocationChange = { newOrigin ->
                            origin = newOrigin
                            // Validar en tiempo real
                            if (newOrigin.isNotBlank() && !isValidCoordinateFormat(newOrigin)) {
                                originError = "Formato inválido. Usa: latitud, longitud"
                            } else {
                                originError = null
                                originPoint = parseCoordinatesFromString(newOrigin)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ DESTINO CON VALIDACIÓN
                    DestinationSelectionCardWithValidation(
                        destination = destination,
                        error = destinationError,
                        onTextChange = { newDest ->
                            destination = newDest
                            // Validar en tiempo real
                            if (newDest.isNotBlank() && !isValidCoordinateFormat(newDest)) {
                                destinationError = "Formato inválido. Usa: latitud, longitud"
                            } else {
                                destinationError = null
                                destinationPoint = parseCoordinatesFromString(newDest)
                            }
                        },
                        onClear = {
                            destination = ""
                            destinationPoint = null
                            destinationError = null
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
                                // Validar antes de calcular ruta
                                if (destinationPoint != null && originPoint != null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ Calculando ruta...")
                                    }
                                } else {
                                    val validation = validatePackageForm(
                                        PackageFormData(
                                            origin = origin,
                                            destination = destination,
                                            weight = if (weight.isNotBlank()) weight else "1",
                                            size = size,
                                            quotedPrice = quotedPrice,
                                            withinDepartment = withinDepartment
                                        )
                                    )

                                    when (validation) {
                                        is ValidationResult.Success -> {
                                            val parsedDest = parseCoordinatesFromString(destination)
                                            if (originPoint != null && parsedDest != null) {
                                                destinationPoint = parsedDest
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("✅ Calculando ruta...")
                                                }
                                            }
                                        }
                                        is ValidationResult.Error -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("❌ ${validation.message}")
                                            }
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
                                destinationError = null
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

                    // ✅ PESO CON VALIDACIÓN
                    EditFieldCardWithValidation(
                        label = "Peso (kg)",
                        value = weight,
                        error = weightError,
                        onValueChange = { newWeight ->
                            weight = newWeight
                            // Validar en tiempo real
                            if (newWeight.isNotBlank()) {
                                if (!isValidPositiveNumber(newWeight, 100.0)) {
                                    weightError = "Peso inválido. Máx: 100 kg"
                                } else {
                                    weightError = null
                                }
                            } else {
                                weightError = null
                            }
                        },
                        onClear = {
                            weight = ""
                            weightError = null
                        },
                        isNumber = true,
                        isRequired = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ TAMAÑO CON VALIDACIÓN
                    EditFieldCardWithValidation(
                        label = "Tamaño",
                        value = size,
                        error = sizeError,
                        onValueChange = { newSize ->
                            size = newSize
                            // Validar en tiempo real
                            if (newSize.isNotBlank() && !isValidSize(newSize)) {
                                sizeError = "Máximo 50 caracteres"
                            } else {
                                sizeError = null
                            }
                        },
                        onClear = {
                            size = ""
                            sizeError = null
                        },
                        isNumber = false,
                        isRequired = false
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ PRECIO COTIZADO CON VALIDACIÓN
                    EditFieldCardWithValidation(
                        label = "Precio Cotizado ($)",
                        value = quotedPrice,
                        error = priceError,
                        onValueChange = { newPrice ->
                            quotedPrice = newPrice
                            // Validar en tiempo real
                            if (newPrice.isNotBlank()) {
                                if (!isValidDecimalNumber(newPrice)) {
                                    priceError = "Precio inválido"
                                } else {
                                    try {
                                        val price = newPrice.toDouble()
                                        if (price < 0) {
                                            priceError = "No puede ser negativo"
                                        } else if (price > 10000) {
                                            priceError = "Máximo $10,000"
                                        } else {
                                            priceError = null
                                        }
                                    } catch (e: Exception) {
                                        priceError = "Número inválido"
                                    }
                                }
                            } else {
                                priceError = null
                            }
                        },
                        onClear = {
                            quotedPrice = ""
                            priceError = null
                        },
                        isNumber = true,
                        isRequired = false
                    )

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

                    // ✅ BOTÓN DE CONFIRMAR CON VALIDACIÓN COMPLETA
                    Button(
                        onClick = {
                            // Validar todos los campos
                            val validation = validatePackageForm(
                                PackageFormData(
                                    origin = origin,
                                    destination = destination,
                                    weight = weight,
                                    size = size,
                                    quotedPrice = quotedPrice,
                                    withinDepartment = withinDepartment
                                )
                            )

                            when (validation) {
                                is ValidationResult.Success -> {
                                    showConfirmationDialog = true
                                }
                                is ValidationResult.Error -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("❌ ${validation.message}")
                                    }
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
                            Text("Confirmar Envío", color = Color.Black, fontWeight = FontWeight.Medium)
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
            title = {
                Text("Confirmar Envío", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("¿Estás seguro de que quieres registrar este paquete?",
                        color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Información detallada del paquete
                    Text("Detalles del Paquete:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00A76D))

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("• Origen: $origin", style = MaterialTheme.typography.bodySmall,
                        color = Color.Black)
                    Text("• Destino: $destination", style = MaterialTheme.typography.bodySmall,
                        color = Color.Black)
                    Text("• Peso: $weight kg", style = MaterialTheme.typography.bodySmall,
                        color = Color.Black)

                    if (size.isNotBlank()) {
                        Text("• Tamaño: $size", style = MaterialTheme.typography.bodySmall,
                            color = Color.Black)
                    }

                    if (quotedPrice.isNotBlank()) {
                        Text("• Precio cotizado: $$quotedPrice", style = MaterialTheme.typography.bodySmall,
                            color = Color.Black)
                    }

                    Text("• Tipo: ${if (withinDepartment) "Dentro del departamento" else "Nacional"}",
                        style = MaterialTheme.typography.bodySmall)

                    routeInfoText?.let { info ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ruta estimada: $info",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00A76D))
                    }

                    // ✅ NOTA PARA PLAY STORE (transparencia)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Nota: Al confirmar, aceptas nuestros términos de servicio y políticas de privacidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                                scope.launch {
                                    delay(1000)
                                    isLoading = false
                                    // Navegar a ConfirmationScreen usando navController
                                    navController.navigate(Routes.CONFIRMATION) {
                                        // Limpiar el stack de navegación para que no pueda volver atrás con back
                                        popUpTo(Routes.HOME) { inclusive = false }
                                    }
                                    snackbarHostState.showSnackbar("Paquete creado exitosamente")
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error al crear el paquete: ${e.message}")
                                }
                            }
                        } ?: run {
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: No se pudo identificar al usuario")
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
                        Text("Confirmar Envío")
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
            },
            containerColor = Color.White
        )
    }
}

// ==================== COMPONENTES CON VALIDACIÓN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentLocationCardWithValidation(
    location: String,
    error: String?,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    onLocationChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) Color(0xFFFFEBEE) else Color(0xFF80D4B6)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📍 Tu ubicación actual",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) Color(0xFFD32F2F) else Color.Black
                )
                if (error != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚠️", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Obteniendo ubicación...", color = Color.Gray) },
                    singleLine = true,
                    isError = error != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = if (error != null) Color(0xFFD32F2F) else Color.White,
                        unfocusedBorderColor = if (error != null) Color(0xFFD32F2F) else Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        errorBorderColor = Color(0xFFD32F2F),
                        errorContainerColor = Color(0xFFFFEBEE),
                        errorCursorColor = Color(0xFFD32F2F),
                        errorTextColor = Color(0xFFD32F2F),
                        errorPlaceholderColor = Color.Gray
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
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ubicación actual",
                            tint = if (error != null) Color(0xFFD32F2F) else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationSelectionCardWithValidation(
    destination: String,
    error: String?,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSelectOnMap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) Color(0xFFFFEBEE) else Color(0xFF80D4B6)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🎯 Lugar de destino",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) Color(0xFFD32F2F) else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                if (error != null) {
                    Text("⚠️", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(
                    onClick = onSelectOnMap,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Seleccionar en mapa",
                        tint = if (error != null) Color(0xFFD32F2F) else Color.Black
                    )
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: -16.5000, -68.1500", color = Color.Gray) },
                    singleLine = true,
                    isError = error != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = if (error != null) Color(0xFFD32F2F) else Color.White,
                        unfocusedBorderColor = if (error != null) Color(0xFFD32F2F) else Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        errorBorderColor = Color(0xFFD32F2F),
                        errorContainerColor = Color(0xFFFFEBEE),
                        errorCursorColor = Color(0xFFD32F2F),
                        errorTextColor = Color(0xFFD32F2F),
                        errorPlaceholderColor = Color.Gray
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
                            tint = if (error != null) Color(0xFFD32F2F) else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFieldCardWithValidation(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    isNumber: Boolean = false,
    isRequired: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) Color(0xFFFFEBEE) else Color(0xFF80D4B6)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isRequired) "$label *" else label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) Color(0xFFD32F2F) else Color.Black
                )
                if (error != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚠️", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

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
                    placeholder = {
                        Text(
                            if (isRequired) "Ingresa $label" else "$label (opcional)",
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    isError = error != null || (isNumber && value.isNotBlank() && !value.matches(Regex("^\\d*\\.?\\d*$"))),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = if (error != null) Color(0xFFD32F2F) else Color.White,
                        unfocusedBorderColor = if (error != null) Color(0xFFD32F2F) else Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                        errorBorderColor = Color(0xFFD32F2F),
                        errorContainerColor = Color(0xFFFFEBEE),
                        errorCursorColor = Color(0xFFD32F2F),
                        errorTextColor = Color(0xFFD32F2F),
                        errorPlaceholderColor = Color.Gray
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
                            contentDescription = "Limpiar",
                            tint = if (error != null) Color(0xFFD32F2F) else Color.Black
                        )
                    }
                }
            }
        }
    }
}

// ==================== COMPONENTES AUXILIARES ====================

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

// ==================== FUNCIONES AUXILIARES ====================

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