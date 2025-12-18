package com.ucb.deliveryapp.features.home.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mapbox.geojson.Point
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.core.datastore.LoginDataStore
import com.ucb.deliveryapp.core.ui.components.MapboxMapView
import com.ucb.deliveryapp.features.packages.domain.model.Package
import com.ucb.deliveryapp.features.packages.domain.model.PackagePriority
import com.ucb.deliveryapp.features.packages.domain.model.PackageStatus
import com.ucb.deliveryapp.features.packages.presentation.PackageViewModel
import com.ucb.deliveryapp.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import kotlin.math.*

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

data class PackageFormData(
    val origin: String,
    val destination: String,
    val weight: String,
    val size: String = "",
    val withinDepartment: Boolean = false
)

object MapboxGeocodingService {

    suspend fun reverseGeocode(context: Context, point: Point): String {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ Mantener getString SOLO para el token técnico (no UI)
                val accessToken = context.getString(R.string.mapbox_access_token)
                val lng = point.longitude()
                val lat = point.latitude()

                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json" +
                        "?access_token=$accessToken" +
                        "&types=place,locality,neighborhood,address,poi" +
                        "&language=es"

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    parseGeocodingResponse(responseBody)
                } else {
                    "Ubicación desconocida"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Ubicación desconocida"
            }
        }
    }

    private fun parseGeocodingResponse(jsonString: String): String {
        return try {
            val json = JSONObject(jsonString)
            val features = json.getJSONArray("features")

            if (features.length() > 0) {
                var bestResult = ""
                var bestRelevance = -1.0

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val relevance = feature.optDouble("relevance", 0.0)
                    val placeName = feature.optString("place_name_es", feature.optString("place_name", ""))
                    val placeType = feature.getJSONArray("place_type").optString(0, "")

                    val typeScore = when (placeType) {
                        "address" -> 4
                        "poi" -> 3
                        "place" -> 2
                        "locality" -> 1
                        else -> 0
                    }

                    val totalScore = relevance + typeScore

                    if (totalScore > bestRelevance && placeName.isNotBlank()) {
                        bestRelevance = totalScore
                        bestResult = simplifyAddress(placeName)
                    }
                }

                if (bestResult.isNotBlank()) bestResult else "Ubicación desconocida"
            } else {
                "Ubicación desconocida"
            }
        } catch (e: Exception) {
            "Error obteniendo dirección"
        }
    }

    private fun simplifyAddress(fullAddress: String): String {
        val parts = fullAddress.split(",").map { it.trim() }

        return when {
            parts.size > 2 && parts.last().equals("Bolivia", ignoreCase = true) -> {
                parts.dropLast(1).joinToString(", ")
            }
            parts.size > 3 -> {
                parts.take(3).joinToString(", ")
            }
            else -> fullAddress
        }
    }
}

private fun calculatePrice(distanceKm: Double, weightKg: Double): Double {
    val baseRatePerKm = 2.50
    val ratePerKg = 5.0
    val minimumPrice = 20.0

    val distancePrice = distanceKm * baseRatePerKm
    val weightPrice = weightKg * ratePerKg
    val totalPrice = distancePrice + weightPrice

    return maxOf(totalPrice, minimumPrice)
}

private fun isValidCoordinateFormat(coordinate: String): Boolean {
    return try {
        val parts = coordinate.split(",")
        if (parts.size != 2) return false

        val lat = parts[0].trim().toDouble()
        val lng = parts[1].trim().toDouble()

        lat in -90.0..90.0 && lng in -180.0..180.0
    } catch (e: Exception) {
        false
    }
}

private fun isValidPositiveNumber(value: String, maxValue: Double = Double.MAX_VALUE): Boolean {
    return try {
        val num = value.toDouble()
        num > 0 && num <= maxValue
    } catch (e: NumberFormatException) {
        false
    }
}

private fun isValidSize(size: String): Boolean {
    return size.length <= 50
}

private fun validatePackageForm(
    originPoint: Point?,
    destinationPoint: Point?,
    weight: String,
    size: String
): ValidationResult {
    if (originPoint == null) {
        return ValidationResult.Error("No se pudo obtener tu ubicación actual")
    }

    if (destinationPoint == null) {
        return ValidationResult.Error("Selecciona un destino en el mapa")
    }

    val distance = calculateDistance(originPoint, destinationPoint)
    if (distance < 0.001) {
        return ValidationResult.Error("El origen y destino deben ser lugares diferentes")
    }

    if (weight.isBlank()) {
        return ValidationResult.Error("El peso es obligatorio")
    }
    if (!isValidPositiveNumber(weight, 100.0)) {
        return ValidationResult.Error("El peso debe ser entre 0.1 y 100 kg")
    }

    if (size.isNotBlank() && !isValidSize(size)) {
        return ValidationResult.Error("El tamaño no puede superar los 50 caracteres")
    }

    return ValidationResult.Success
}

private fun calculateDistance(point1: Point, point2: Point): Double {
    val lat1 = Math.toRadians(point1.latitude())
    val lon1 = Math.toRadians(point1.longitude())
    val lat2 = Math.toRadians(point2.latitude())
    val lon2 = Math.toRadians(point2.longitude())

    val dlon = lon2 - lon1
    val dlat = lat2 - lat1
    val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return 6371 * c
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMenu: () -> Unit,
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val loginDataStore = remember { LoginDataStore(context) }
    val packageViewModel: PackageViewModel = koinViewModel()

    var currentLocation by remember { mutableStateOf<Point?>(null) }
    var isGettingLocation by remember { mutableStateOf(false) }

    var internalOrigin by remember { mutableStateOf("") }
    var internalDestination by remember { mutableStateOf("") }

    var weight by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var quotedPrice by remember { mutableStateOf("") }
    var withinDepartment by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showMapForDestinationSelection by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showMapForOriginSelection by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var originError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var sizeError by remember { mutableStateOf<String?>(null) }

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var shouldShowPermissionRationale by remember { mutableStateOf(false) }

    var routeInfoText by remember { mutableStateOf<String?>(null) }
    var calculatedDistance by remember { mutableStateOf<Double?>(null) }
    var calculatedEta by remember { mutableStateOf<Int?>(null) }
    var originPoint by remember { mutableStateOf<Point?>(null) }
    var destinationPoint by remember { mutableStateOf<Point?>(null) }

    var originAddress by remember { mutableStateOf<String?>(null) }
    var destinationAddress by remember { mutableStateOf<String?>(null) }
    var isGeocodingOrigin by remember { mutableStateOf(false) }
    var isGeocodingDestination by remember { mutableStateOf(false) }

    var locationUpdateCounter by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(locationPermissionGranted, locationUpdateCounter) {
        if (locationPermissionGranted) {
            delay(30000)
            locationUpdateCounter++
        }
    }

    LaunchedEffect(locationPermissionGranted, locationUpdateCounter) {
        if (locationPermissionGranted) {
            isGettingLocation = true
            delay(500)

            getCurrentLocationWithChecks(context)?.let { location ->
                val newLocation = Point.fromLngLat(location.longitude, location.latitude)
                currentLocation = newLocation
                originPoint = newLocation
                internalOrigin = "${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}"
                originError = null

                if (originAddress == null) {
                    isGeocodingOrigin = true
                    originAddress = MapboxGeocodingService.reverseGeocode(context, newLocation)
                    isGeocodingOrigin = false
                }

                scope.launch {
                    snackbarHostState.showSnackbar("Ubicación actualizada")
                }
            } ?: run {
                scope.launch {
                    snackbarHostState.showSnackbar("No se pudo obtener ubicación GPS. Usando selección manual.")
                }
            }
            isGettingLocation = false
        }
    }

    LaunchedEffect(calculatedDistance, weight) {
        if (calculatedDistance != null && weight.isNotBlank()) {
            try {
                val weightValue = weight.toDouble()
                val distanceValue = calculatedDistance ?: 0.0
                val price = calculatePrice(distanceValue, weightValue)
                quotedPrice = "%.2f".format(price)
            } catch (e: Exception) {
                quotedPrice = ""
            }
        } else {
            quotedPrice = ""
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        locationPermissionGranted = allGranted

        if (allGranted) {
            scope.launch { locationUpdateCounter++ }
        } else {
            shouldShowPermissionRationale = permissions.any {
                !it.value && ContextCompat.checkSelfPermission(context, it.key) == PackageManager.PERMISSION_DENIED
            }
        }
    }

    var currentUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        currentUserId = loginDataStore.getUserId() ?: "default_user"

        val hasPermission = hasLocationPermission(context)
        locationPermissionGranted = hasPermission

        if (hasPermission) locationUpdateCounter++
    }

    LaunchedEffect(internalOrigin) {
        if (internalOrigin.isNotBlank() && isValidCoordinateFormat(internalOrigin)) {
            val point = parseCoordinatesFromString(internalOrigin)
            if (point != null) {
                if (originAddress == null || originPoint != point) {
                    isGeocodingOrigin = true
                    originAddress = MapboxGeocodingService.reverseGeocode(context, point)
                    isGeocodingOrigin = false
                }
                originPoint = point
                currentLocation = point
            }
        }
    }

    LaunchedEffect(internalDestination) {
        if (internalDestination.isNotBlank() && isValidCoordinateFormat(internalDestination)) {
            val point = parseCoordinatesFromString(internalDestination)
            if (point != null) {
                if (destinationAddress == null || destinationPoint != point) {
                    isGeocodingDestination = true
                    destinationAddress = MapboxGeocodingService.reverseGeocode(context, point)
                    isGeocodingDestination = false
                }
                destinationPoint = point
            }
        }
    }

    LaunchedEffect(originPoint) {
        if (originPoint != null) {
            currentLocation = originPoint

            if (originAddress == null && !isGeocodingOrigin) {
                isGeocodingOrigin = true
                originAddress = MapboxGeocodingService.reverseGeocode(context, originPoint!!)
                isGeocodingOrigin = false
            }
        }
    }

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
                                internalDestination = "${"%.6f".format(point.latitude())}, ${"%.6f".format(point.longitude())}"
                                destinationAddress = null
                                showMapForDestinationSelection = false
                                destinationError = null

                                scope.launch {
                                    try {
                                        isGeocodingDestination = true
                                        val address = MapboxGeocodingService.reverseGeocode(context, point)
                                        destinationAddress = address
                                        isGeocodingDestination = false
                                        snackbarHostState.showSnackbar("Destino seleccionado: ${address.take(30)}...")
                                    } catch (e: Exception) {
                                        isGeocodingDestination = false
                                        destinationAddress = "Ubicación seleccionada"
                                        snackbarHostState.showSnackbar("Destino seleccionado")
                                    }
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

    if (showMapForOriginSelection) {
        AlertDialog(
            onDismissRequest = { showMapForOriginSelection = false },
            title = {
                Text(
                    "Selecciona el origen en el mapa",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00A76D)
                )
            },
            text = {
                Column {
                    Text(
                        "Toca en el mapa para seleccionar el punto de origen",
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
                            allowDestinationSelection = true,
                            onDestinationSelected = { point ->
                                originPoint = point
                                currentLocation = point
                                internalOrigin = "${"%.6f".format(point.latitude())}, ${"%.6f".format(point.longitude())}"
                                originAddress = null
                                showMapForOriginSelection = false
                                originError = null

                                locationPermissionGranted = false

                                scope.launch {
                                    try {
                                        isGeocodingOrigin = true
                                        val address = MapboxGeocodingService.reverseGeocode(context, point)
                                        originAddress = address
                                        isGeocodingOrigin = false
                                        snackbarHostState.showSnackbar("Origen seleccionado: ${address.take(30)}...")
                                    } catch (e: Exception) {
                                        isGeocodingOrigin = false
                                        originAddress = "Origen seleccionado"
                                        snackbarHostState.showSnackbar("Origen seleccionado")
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMapForOriginSelection = false },
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
                title = { Text("Página Principal", color = Color.White) },
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

                    if (!locationPermissionGranted && originPoint == null) {
                        PermissionWarningCard(
                            onActivate = {
                                if (shouldShowPermissionRationale) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            showActivateButton = originPoint == null
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { }
                    ) {
                        if (locationPermissionGranted || originPoint != null) {
                            if (isGettingLocation && originPoint == null) {
                                LoadingMapPlaceholder()
                            } else {
                                MapboxMapView(
                                    modifier = Modifier.fillMaxSize(),
                                    origin = originPoint,
                                    destination = destinationPoint,
                                    onRouteInfo = { etaMinutes, distanceKm ->
                                        calculatedEta = etaMinutes
                                        calculatedDistance = distanceKm
                                        routeInfoText = "ETA ≈ ${etaMinutes} min • ${"%.2f".format(distanceKm)} km"
                                    }
                                )
                            }
                        } else {
                            NoPermissionMapPlaceholder()
                        }
                    }

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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (originError != null) Color(0xFFFFEBEE) else Color(0xFF80D4B6)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ubicación de origen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (originError != null) Color(0xFFD32F2F) else Color.Black
                                )
                                if (isGeocodingOrigin) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            if (originPoint != null) {
                                originAddress?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (locationPermissionGranted) "Usando tu ubicación actual" else "Ubicación seleccionada manualmente",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00A76D)
                                    )
                                } ?: run {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Obteniendo dirección...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Ubicación no disponible",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { showMapForOriginSelection = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00A76D)
                                    )
                                ) {
                                    Text("Seleccionar origen en mapa", color = Color.White)
                                }

                                if (locationPermissionGranted && originPoint != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                locationUpdateCounter++
                                                snackbarHostState.showSnackbar("Actualizando ubicación...")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00A76D)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = "Actualizar ubicación",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Actualizar",
                                            color = Color.White,
                                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                                        )
                                    }
                                }
                            }

                            if (!locationPermissionGranted && originPoint == null) {
                                Text(
                                    text = "Permisos de ubicación desactivados. Selecciona manualmente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            if (originError != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = originError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (destinationError != null) Color(0xFFFFEBEE) else Color(0xFF80D4B6)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lugar de destino",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (destinationError != null) Color(0xFFD32F2F) else Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                if (destinationError != null) {
                                    Text("⚠️", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                if (isGeocodingDestination) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (originPoint != null) {
                                            showMapForDestinationSelection = true
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Selecciona un origen primero")
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp),
                                    enabled = !isGeocodingDestination
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Seleccionar en mapa",
                                        tint = if (destinationError != null) Color(0xFFD32F2F) else Color.Black
                                    )
                                }
                            }

                            destinationAddress?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium
                                )
                            } ?: run {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isGeocodingDestination) "Obteniendo dirección..."
                                    else if (destinationError != null) "Selecciona un destino en el mapa"
                                    else "No seleccionado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (destinationError != null) Color(0xFFD32F2F) else Color.Gray
                                )
                            }

                            if (destinationError != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = destinationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD32F2F)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        if (originPoint != null) {
                                            showMapForDestinationSelection = true
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Selecciona un origen primero")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00A76D)
                                    )
                                ) {
                                    Text("Seleccionar destino en el mapa", color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (destinationPoint != null && originPoint != null) {
                                    scope.launch { snackbarHostState.showSnackbar("Calculando ruta...") }
                                } else {
                                    val validation = validatePackageForm(
                                        originPoint = originPoint,
                                        destinationPoint = destinationPoint,
                                        weight = if (weight.isNotBlank()) weight else "1",
                                        size = size
                                    )

                                    when (validation) {
                                        is ValidationResult.Success -> {
                                            if (originPoint != null && destinationPoint != null) {
                                                scope.launch { snackbarHostState.showSnackbar("Calculando ruta...") }
                                            }
                                        }
                                        is ValidationResult.Error -> {
                                            scope.launch { snackbarHostState.showSnackbar(validation.message) }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A76D)),
                            enabled = destinationPoint != null && originPoint != null
                        ) {
                            Text("Calcular ruta", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                internalDestination = ""
                                destinationPoint = null
                                destinationAddress = null
                                destinationError = null
                                routeInfoText = null
                                calculatedDistance = null
                                calculatedEta = null
                                scope.launch { snackbarHostState.showSnackbar("Destino limpio") }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00A76D)),
                            enabled = destinationPoint != null
                        ) {
                            Text("Limpiar", color = Color(0xFF00A76D))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    EditFieldCardWithValidation(
                        label = "Peso (kg)",
                        value = weight,
                        error = weightError,
                        onValueChange = { newWeight ->
                            weight = newWeight
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

                    EditFieldCardWithValidation(
                        label = "Tamaño (descripción)",
                        value = size,
                        error = sizeError,
                        onValueChange = { newSize ->
                            size = newSize
                            sizeError = if (newSize.isNotBlank() && !isValidSize(newSize)) "Máximo 50 caracteres" else null
                        },
                        onClear = {
                            size = ""
                            sizeError = null
                        },
                        isNumber = false,
                        isRequired = false
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF80D4B6))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Precio Calculado (Bs.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black
                                )
                                if (calculatedDistance != null && weight.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${"%.1f".format(calculatedDistance)} km × Bs. 2.50/km + ${weight}kg × Bs. 5/kg)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = quotedPrice,
                                onValueChange = { },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        if (calculatedDistance == null || weight.isBlank())
                                            "Calcula la ruta e ingresa el peso"
                                        else
                                            "Calculando...",
                                        color = Color.Gray
                                    )
                                },
                                singleLine = true,
                                readOnly = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = Color.Transparent,
                                    focusedPlaceholderColor = Color.Gray,
                                    unfocusedPlaceholderColor = Color.Gray,
                                    disabledTextColor = Color.Black,
                                    disabledContainerColor = Color.White,
                                    disabledBorderColor = Color.White.copy(alpha = 0.7f),
                                    disabledPlaceholderColor = Color.Gray
                                ),
                                enabled = false
                            )

                            if (quotedPrice.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Precio mínimo garantizado: Bs. 20.00",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF00A76D),
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Envío dentro del departamento:", color = Color.Black)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = withinDepartment,
                            onCheckedChange = { withinDepartment = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val validation = validatePackageForm(
                                originPoint = originPoint,
                                destinationPoint = destinationPoint,
                                weight = weight,
                                size = size
                            )

                            when (validation) {
                                is ValidationResult.Success -> {
                                    if (quotedPrice.isBlank() && calculatedDistance != null && weight.isNotBlank()) {
                                        try {
                                            val weightValue = weight.toDouble()
                                            val distanceValue = calculatedDistance ?: 0.0
                                            val price = calculatePrice(distanceValue, weightValue)
                                            quotedPrice = "%.2f".format(price)
                                        } catch (e: Exception) {
                                            scope.launch { snackbarHostState.showSnackbar("Error calculando el precio") }
                                            return@Button
                                        }
                                    }

                                    if (quotedPrice.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Calcula la ruta primero") }
                                        return@Button
                                    }

                                    showConfirmationDialog = true
                                }
                                is ValidationResult.Error -> {
                                    scope.launch { snackbarHostState.showSnackbar(validation.message) }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        enabled = originPoint != null && destinationPoint != null && weight.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFAC10C))
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

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmationDialog = false },
            title = { Text("Confirmar Envío", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Estás seguro de que quieres registrar este paquete?", color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Detalles del Paquete:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00A76D)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("• Origen: ${originAddress ?: "Ubicación actual"}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                    Text("• Destino: ${destinationAddress ?: "Destino seleccionado"}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                    Text("• Peso: $weight kg", style = MaterialTheme.typography.bodySmall, color = Color.Black)

                    if (size.isNotBlank()) {
                        Text("• Tamaño: $size", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                    }

                    if (quotedPrice.isNotBlank()) {
                        Text("• Precio: Bs. $quotedPrice", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                    }

                    Text(
                        "• Tipo: ${if (withinDepartment) "Dentro del departamento" else "Nacional"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    routeInfoText?.let { info ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ruta estimada: $info",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00A76D)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Nota: Al confirmar, aceptas nuestros términos de servicio y políticas de privacidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
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
                                    originPoint!!,
                                    destinationPoint!!,
                                    weight, size,
                                    quotedPrice, withinDepartment, userId
                                )
                                packageViewModel.createPackage(newPackage)
                                scope.launch {
                                    delay(1000)
                                    isLoading = false
                                    navController.navigate(Routes.CONFIRMATION) {
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
                            scope.launch { snackbarHostState.showSnackbar("Error: No se pudo identificar al usuario") }
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
                            if (isRequired) "Ingresa $label" else "$label",
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

@Composable
private fun PermissionWarningCard(
    onActivate: () -> Unit,
    showActivateButton: Boolean = true
) {
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
            if (showActivateButton) {
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

private suspend fun getCurrentLocationWithChecks(context: Context): Location? {
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
        Log.e("LocationHelper", "SecurityException: Permisos revocados", e)
        null
    } catch (e: Exception) {
        Log.e("LocationHelper", "Error obteniendo ubicación", e)
        null
    }
}

private fun hasLocationPermission(context: Context): Boolean {
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
    originPoint: Point,
    destinationPoint: Point,
    weight: String,
    size: String,
    quotedPrice: String,
    withinDepartment: Boolean,
    userId: String
): Package {
    val priority = if (withinDepartment) PackagePriority.NORMAL else PackagePriority.EXPRESS

    val notes = buildString {
        append("Origen: ${originPoint.latitude()}, ${originPoint.longitude()}")
        append("\nDestino: ${destinationPoint.latitude()}, ${destinationPoint.longitude()}")
        if (size.isNotBlank()) append("\nTamaño: $size")
        if (quotedPrice.isNotBlank()) append("\nPrecio calculado: Bs. $quotedPrice")
        append(if (withinDepartment) "\nEnvío dentro del departamento" else "\nEnvío nacional")
    }

    val weightValue = weight.toDoubleOrNull() ?: 0.0
    val now = System.currentTimeMillis()
    val estimated = now + if (withinDepartment) 24L * 60 * 60 * 1000 else 48L * 60 * 60 * 1000

    return Package(
        id = "",
        trackingNumber = generateTrackingNumber(),
        senderName = "Usuario Actual",
        recipientName = "Destinatario",
        recipientAddress = "${destinationPoint.latitude()}, ${destinationPoint.longitude()}",
        recipientPhone = "Por definir",
        weight = weightValue,
        status = PackageStatus.PENDING,
        priority = priority,
        estimatedDeliveryAtMillis = estimated,
        createdAtMillis = now,
        deliveredAtMillis = null,
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