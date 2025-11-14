// HomeScreen.kt - VERSIÓN CORREGIDA
package com.ucb.deliveryapp.ui.screens.home

import android.content.Intent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackagePriority
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.ui.screens.GoogleMapView
import com.ucb.deliveryapp.ui.screens.packages.PackageListActivity
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.getViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  // Obtener el ViewModel
  val packageViewModel: PackageViewModel = viewModel(
    factory = getViewModelFactory(context)
  )

  var origin by remember { mutableStateOf("") }
  var destination by remember { mutableStateOf("") }
  var weight by remember { mutableStateOf("") }
  var size by remember { mutableStateOf("") }
  var quotedPrice by remember { mutableStateOf("") }
  var withinDepartment by remember { mutableStateOf(false) }
  var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

  // Estado para el diálogo de confirmación
  var showConfirmationDialog by remember { mutableStateOf(false) }

  // Estado para el scroll
  val scrollState = rememberScrollState()

  // CORREGIDO: Usar StateFlow en lugar de LiveData
  val packages by packageViewModel.packages.collectAsState()

  // Cargar paquetes cuando se inicia la pantalla
  LaunchedEffect(Unit) {
    packageViewModel.loadUserPackages(1) // Usar el ID de usuario real
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Página Principal") },
        actions = {
          IconButton(onClick = {
            val intent = Intent(context, PackageListActivity::class.java)
            context.startActivity(intent)
          }) {
            Icon(Icons.Default.List, contentDescription = "Mis Paquetes")
          }
        }
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    content = { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(scrollState)
      ) {
        Column(
          modifier = Modifier.padding(12.dp)
        ) {
          // MAPA GOOGLE
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(280.dp)
              .clip(RoundedCornerShape(12.dp))
          ) {
            GoogleMapView(
              modifier = Modifier.fillMaxSize(),
              packages = packages, // Ahora funciona correctamente
              onLocationSelected = { location ->
                selectedLocation = location
              }
            )
          }

          // Información de ubicación
          if (selectedLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
              )
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.List, contentDescription = "Ubicación")
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("Ubicación seleccionada", style = MaterialTheme.typography.labelMedium)
                  Text(
                    "Lat: ${"%.6f".format(selectedLocation!!.latitude)}, Lng: ${"%.6f".format(selectedLocation!!.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Resto del código se mantiene igual...
          EditFieldCard(
            label = "Lugar de origen", value = origin,
            onValueChange = { origin = it }, onClear = { origin = "" }
          )
          Spacer(modifier = Modifier.height(8.dp))
          EditFieldCard(
            label = "Lugar de destino", value = destination,
            onValueChange = { destination = it }, onClear = { destination = "" }
          )
          Spacer(modifier = Modifier.height(8.dp))
          EditFieldCard(
            label = "Peso (kg)", value = weight,
            onValueChange = { weight = it }, onClear = { weight = "" }, isNumber = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          EditFieldCard(
            label = "Tamaño", value = size,
            onValueChange = { size = it }, onClear = { size = "" }
          )
          Spacer(modifier = Modifier.height(8.dp))
          EditFieldCard(
            label = "Precio Cotizado", value = quotedPrice,
            onValueChange = { quotedPrice = it }, onClear = { quotedPrice = "" }, isNumber = true
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            enabled = validateInput(origin, destination, weight)
          ) {
            Text("Confirmar Envío")
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = {
              val intent = Intent(context, PackageListActivity::class.java)
              context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp)
          ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ver mis Paquetes")
          }
        }
      }
    }
  )

  // Diálogo de confirmación
  if (showConfirmationDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmationDialog = false },
      title = { Text("Confirmar Envío") },
      text = { Text("¿Estás seguro de que quieres registrar este paquete?") },
      confirmButton = {
        TextButton(
          onClick = {
            showConfirmationDialog = false
            val newPackage = createPackageFromForm(
              origin, destination, weight, size, quotedPrice, withinDepartment
            )
            packageViewModel.createPackage(newPackage)
            scope.launch {
              snackbarHostState.showSnackbar("✅ Paquete registrado exitosamente")
            }
            // Limpiar formulario
            origin = ""; destination = ""; weight = ""; size = ""; quotedPrice = ""; withinDepartment = false
          }
        ) { Text("Confirmar") }
      },
      dismissButton = {
        TextButton({ showConfirmationDialog = false }) { Text("Cancelar") }
      }
    )
  }
}

// Las funciones auxiliares se mantienen igual...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFieldCard(
  label: String, value: String, onValueChange: (String) -> Unit,
  onClear: () -> Unit, isNumber: Boolean = false
) {
  Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(text = label, style = MaterialTheme.typography.bodySmall)
      Spacer(Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = value, onValueChange = { newValue ->
            if (isNumber && newValue.isNotEmpty() && !newValue.matches(Regex("^\\d*\\.?\\d*$"))) return@OutlinedTextField
            onValueChange(newValue)
          },
          modifier = Modifier.weight(1f), singleLine = true,
          placeholder = { Text("Ingresa $label") }
        )
        if (value.isNotBlank()) {
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, contentDescription = "Limpiar")
          }
        }
      }
    }
  }
}

private fun createPackageFromForm(
  origin: String, destination: String, weight: String,
  size: String, quotedPrice: String, withinDepartment: Boolean
): Package {
  val priority = if (withinDepartment) PackagePriority.NORMAL else PackagePriority.EXPRESS
  val notes = buildString {
    append("Origen: $origin\nDestino: $destination")
    if (size.isNotBlank()) append("\nTamaño: $size")
    if (quotedPrice.isNotBlank()) append("\nPrecio cotizado: $quotedPrice")
    append(if (withinDepartment) "\nEnvío departamental" else "\nEnvío nacional")
  }
  return Package(
    trackingNumber = "UCB${System.currentTimeMillis().toString().takeLast(8)}${(1000..9999).random()}",
    senderName = "Usuario Actual", recipientName = "Destinatario en $destination",
    recipientAddress = destination, recipientPhone = "Por definir",
    weight = weight.toDouble(), status = PackageStatus.PENDING, priority = priority,
    estimatedDeliveryDate = System.currentTimeMillis() + (if (withinDepartment) 3 else 7) * 24 * 60 * 60 * 1000,
    notes = notes, userId = 1
  )
}

private fun validateInput(origin: String, destination: String, weight: String): Boolean {
  return origin.isNotBlank() && destination.isNotBlank() && weight.isNotBlank() &&
    try { weight.toDouble() > 0 } catch (e: NumberFormatException) { false }
}
