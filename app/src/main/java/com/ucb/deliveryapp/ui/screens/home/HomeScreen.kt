// HomeScreen.kt
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.ucb.deliveryapp.R
import com.ucb.deliveryapp.data.entity.Package
import com.ucb.deliveryapp.data.entity.PackagePriority
import com.ucb.deliveryapp.data.entity.PackageStatus
import com.ucb.deliveryapp.ui.screens.MapLibreView
import com.ucb.deliveryapp.ui.screens.packages.PackageListActivity
import com.ucb.deliveryapp.viewmodel.PackageViewModel
import com.ucb.deliveryapp.viewmodel.getViewModelFactory
import java.util.*

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

    // Estado para el diálogo de confirmación
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Estado para el scroll
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Página Principal") },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, PackageListActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Mis Paquetes"
                        )
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
                    .verticalScroll(scrollState) // AGREGADO: Scroll vertical
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        MapLibreView(modifier = Modifier.fillMaxSize())
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campos editables
                    EditFieldCard(
                        label = "Lugar de origen",
                        value = origin,
                        onValueChange = { origin = it },
                        onClear = { origin = "" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EditFieldCard(
                        label = "Lugar de destino",
                        value = destination,
                        onValueChange = { destination = it },
                        onClear = { destination = "" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

                    // Botón de confirmar envío - AHORA GUARDA EN LA BD
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón para ver mis paquetes
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, PackageListActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver mis Paquetes")
                    }

                    // Espacio adicional al final para mejor scroll
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )

    // Diálogo de confirmación
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
                        // Crear y guardar el paquete en la base de datos
                        val newPackage = createPackageFromForm(
                            origin = origin,
                            destination = destination,
                            weight = weight,
                            size = size,
                            quotedPrice = quotedPrice,
                            withinDepartment = withinDepartment
                        )

                        packageViewModel.createPackage(newPackage) // CORREGIDO: insertPackage

                        scope.launch {
                            snackbarHostState.showSnackbar("✅ Paquete registrado exitosamente")
                        }

                        // Limpiar formulario después de guardar
                        origin = ""
                        destination = ""
                        weight = ""
                        size = ""
                        quotedPrice = ""
                        withinDepartment = false
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

// NUEVO: Composable para campos editables (versión simplificada)
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
                // Versión simplificada sin KeyboardOptions
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        // Validación básica para campos numéricos
                        if (isNumber) {
                            // Permite solo números y punto decimal
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

// Función para crear un Package desde el formulario
private fun createPackageFromForm(
    origin: String,
    destination: String,
    weight: String,
    size: String,
    quotedPrice: String,
    withinDepartment: Boolean
): Package {

    // Determinar prioridad basada en el tipo de envío
    val priority = if (withinDepartment) PackagePriority.NORMAL else PackagePriority.EXPRESS

    // Crear notas combinando la información
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
        senderName = "Usuario Actual", // Puedes cambiar esto según tu sistema
        recipientName = "Destinatario en $destination",
        recipientAddress = destination,
        recipientPhone = "Por definir",
        weight = weight.toDouble(),
        status = PackageStatus.PENDING,
        priority = priority,
        estimatedDeliveryDate = System.currentTimeMillis() + getEstimatedDeliveryDays(withinDepartment) * 24 * 60 * 60 * 1000,
        notes = notes,
        userId = 1 // Cambia esto según tu sistema de usuarios
    )
}

// Generar número de seguimiento único
private fun generateTrackingNumber(): String {
    val timestamp = System.currentTimeMillis().toString().takeLast(8)
    val random = (1000..9999).random()
    return "UCB${timestamp}${random}"
}

// Calcular días estimados de entrega
private fun getEstimatedDeliveryDays(withinDepartment: Boolean): Long {
    return if (withinDepartment) 3 else 7
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