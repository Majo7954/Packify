package com.ucb.deliveryapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ucb.deliveryapp.data.entity.Package
import kotlinx.coroutines.launch

@Composable
fun GoogleMapView(
  modifier: Modifier = Modifier,
  packages: List<Package> = emptyList(),
  onLocationSelected: (LatLng) -> Unit = {}
) {
  val ucbLocation = LatLng(-17.3716723, -66.1439156) // UCB por defecto

  var cameraPositionState by remember {
    mutableStateOf(
      CameraPositionState(
        position = CameraPosition.fromLatLngZoom(ucbLocation, 15f)
      )
    )
  }

  val mapProperties by remember {
    mutableStateOf(
      MapProperties(
        isMyLocationEnabled = true,
        mapType = MapType.NORMAL
      )
    )
  }

  var mapLoaded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    GoogleMap(
      modifier = Modifier.fillMaxSize(),
      cameraPositionState = cameraPositionState,
      properties = mapProperties,
      onMapLoaded = {
        mapLoaded = true
      }
    ) {
      // Marcador centrado fijo
      Marker(
        state = MarkerState(position = cameraPositionState.position.target),
        title = "Ubicación de entrega",
        snippet = "Mueve el mapa para seleccionar la ubicación"
      )

      // Marcadores para paquetes existentes (opcional)
      packages.forEach { pkg ->
        Marker(
          state = MarkerState(position = ucbLocation), // Usar misma ubicación por simplicidad
          title = "Paquete: ${pkg.trackingNumber}",
          snippet = pkg.recipientName
        )
      }
    }
  }

  // Detectar cuando el mapa deja de moverse
  LaunchedEffect(cameraPositionState.isMoving) {
    if (!cameraPositionState.isMoving && mapLoaded) {
      val centerLocation = cameraPositionState.position.target
      onLocationSelected(centerLocation)
    }
  }
}
