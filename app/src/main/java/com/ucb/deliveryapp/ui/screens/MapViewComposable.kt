// MapboxMapView.kt - VERSIÓN CORREGIDA
package com.ucb.deliveryapp.ui.screens

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun MapboxMapView(
    modifier: Modifier = Modifier,
    origin: Point? = null,
    destination: Point? = null,
    showMarkers: Boolean = true,
    allowDestinationSelection: Boolean = false,
    onDestinationSelected: (Point) -> Unit = { _ -> },
    onRouteInfo: (etaMinutes: Int, distanceKm: Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val token = context.getString(com.ucb.deliveryapp.R.string.mapbox_access_token)
    val mapInitOptions = remember { MapInitOptions(context) }
    val mapView = remember { MapView(context, mapInitOptions) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Manejo del ciclo de vida
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Cuando cambian origin/destination, pedimos ruta
    LaunchedEffect(origin, destination) {
        if (origin != null && destination != null) {
            requestAndDrawRoute(mapView, origin, destination, token, onRouteInfo)
        }
    }

    // Configurar clics en el mapa para selección
    LaunchedEffect(allowDestinationSelection) {
        if (allowDestinationSelection) {
            mapView.gestures.addOnMapClickListener { point ->
                // ✅ CORREGIDO: Usar las propiedades correctas del punto
                val selectedPoint = Point.fromLngLat(point.longitude(), point.latitude())
                onDestinationSelected(selectedPoint)
                true
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            mv.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                mv.scalebar.enabled = true
                mv.location.enabled = true
                mv.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
        }
    )
}

// Solicitar ruta y dibujarla en el MapView
private fun requestAndDrawRoute(
    mapView: MapView,
    origin: Point,
    destination: Point,
    accessToken: String,
    onRouteInfo: (etaMinutes: Int, distanceKm: Double) -> Unit
) {
    val client = OkHttpClient()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // ✅ CORREGIDO: Usar longitude() y latitude() correctamente
            val coords = "${origin.longitude()},${origin.latitude()};${destination.longitude()},${destination.latitude()}"
            val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/$coords" +
                    "?overview=full&geometries=polyline&steps=false&access_token=$accessToken"

            Log.d("MapboxRoute", "Solicitando ruta: $url")

            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("MapboxRoute", "Error HTTP: ${resp.code}")
                    throw Exception("HTTP ${resp.code}")
                }
                val body = resp.body?.string() ?: throw Exception("Empty body")
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) throw Exception("No routes")
                val route0 = routes.getJSONObject(0)
                val durationSec = route0.getDouble("duration")
                val distanceM = route0.getDouble("distance")
                val geometry = route0.getString("geometry")

                val coordsList = decodePolyline(geometry)

                CoroutineScope(Dispatchers.Main).launch {
                    drawRouteOnMap(mapView, coordsList)
                    val mins = (durationSec / 60.0).roundToInt()
                    val km = distanceM / 1000.0
                    onRouteInfo(mins, km)
                    Log.d("MapboxRoute", "Ruta dibujada: ${mins} min, ${km} km")
                }
            }
        } catch (e: Exception) {
            Log.e("MapboxRoute", "Error fetching route: ${e.message}", e)
        }
    }
}

// Dibujar la ruta en el MapView
private fun drawRouteOnMap(mapView: MapView, points: List<Point>) {
    mapView.getMapboxMap().getStyle { style ->
        val sourceId = "route-source-compose"
        val layerId = "route-layer-compose"

        val feature = Feature.fromGeometry(LineString.fromLngLats(points))
        val featureCollection = FeatureCollection.fromFeature(feature)

        // Crear o actualizar el GeoJsonSource
        val existingSource = style.getSource(sourceId) as? GeoJsonSource
        if (existingSource == null) {
            style.addSource(
                geoJsonSource(sourceId) {
                    featureCollection(featureCollection)
                }
            )
        } else {
            existingSource.featureCollection(featureCollection)
        }

        // Agregar capa si no existe
        if (style.getLayer(layerId) == null) {
            val routeLayer = LineLayer(layerId, sourceId)
                .lineWidth(6.0)
                .lineColor("#00A76D")
                .lineCap(LineCap.ROUND)

            style.addLayer(routeLayer)
        }
    }
}

// Decodificador polyline Mapbox/Google
private fun decodePolyline(encoded: String, precision: Int = 5): List<Point> {
    val coords = mutableListOf<Point>()
    var index = 0
    var lat = 0
    var lng = 0
    val factor = Math.pow(10.0, precision.toDouble()).toInt()

    while (index < encoded.length) {
        var result = 1
        var shift = 0
        var b: Int
        do {
            b = encoded[index++].code - 63 - 1
            result += b shl shift
            shift += 5
        } while (b >= 0x1f)
        lat += if (result and 1 != 0) (result shr 1).inv() else (result shr 1)

        result = 1
        shift = 0
        do {
            b = encoded[index++].code - 63 - 1
            result += b shl shift
            shift += 5
        } while (b >= 0x1f)
        lng += if (result and 1 != 0) (result shr 1).inv() else (result shr 1)

        coords.add(Point.fromLngLat(lng.toDouble() / factor, lat.toDouble() / factor))
    }
    return coords
}