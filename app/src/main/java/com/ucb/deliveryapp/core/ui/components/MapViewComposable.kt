package com.ucb.deliveryapp.core.ui.components

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
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
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
import com.mapbox.maps.EdgeInsets
import com.ucb.deliveryapp.R
import kotlin.math.abs

@Composable
fun MapboxMapView(
    modifier: Modifier = Modifier,
    origin: Point? = null,
    destination: Point? = null,
    allowDestinationSelection: Boolean = false,
    onDestinationSelected: (Point) -> Unit = { _ -> },
    onRouteInfo: (etaMinutes: Int, distanceKm: Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val token = context.getString(R.string.mapbox_access_token)
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context, MapInitOptions(context)) }

    LaunchedEffect(origin, destination) {
        mapView.getMapboxMap().getStyle { style ->
            when {
                origin != null && destination != null -> {
                    val centerLng = (origin.longitude() + destination.longitude()) / 2
                    val centerLat = (origin.latitude() + destination.latitude()) / 2

                    val latDiff = abs(origin.latitude() - destination.latitude())
                    val lngDiff = abs(origin.longitude() - destination.longitude())
                    val maxDiff = maxOf(latDiff, lngDiff)

                    val zoom = when {
                        maxDiff > 1.0 -> 7.0
                        maxDiff > 0.5 -> 8.0
                        maxDiff > 0.2 -> 9.0
                        maxDiff > 0.1 -> 10.0
                        maxDiff > 0.05 -> 11.0
                        else -> 12.0
                    }

                    val cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(centerLng, centerLat))
                        .zoom(zoom)
                        .padding(EdgeInsets(150.0, 50.0, 50.0, 150.0))
                        .build()

                    mapView.getMapboxMap().setCamera(cameraOptions)
                }
                origin != null -> {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(origin)
                            .zoom(14.0)
                            .build()
                    )
                }
                else -> {
                    val bolivia = Point.fromLngLat(-68.1500, -16.5000)
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(bolivia)
                            .zoom(5.0)
                            .build()
                    )
                }
            }
        }
    }

    LaunchedEffect(origin, destination) {
        if (origin != null && destination != null) {
            requestAndDrawRoute(mapView, origin, destination, token, onRouteInfo)
        }
    }

    LaunchedEffect(allowDestinationSelection) {
        if (allowDestinationSelection) {
            mapView.gestures.addOnMapClickListener { point ->
                val selectedPoint = Point.fromLngLat(point.longitude(), point.latitude())
                onDestinationSelected(selectedPoint)
                true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            mv.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                mv.scalebar.enabled = true
                mv.location.enabled = true
                mv.location.updateSettings { enabled = true }
            }
        }
    )
}

private fun calculateZoom(minLng: Double, maxLng: Double, minLat: Double, maxLat: Double): Double {
    val latDiff = maxLat - minLat
    val lngDiff = maxLng - minLng

    val area = latDiff * lngDiff

    return when {
        area > 5.0 -> 6.0
        area > 2.0 -> 7.0
        area > 1.0 -> 8.0
        area > 0.5 -> 9.0
        area > 0.2 -> 10.0
        area > 0.1 -> 11.0
        area > 0.05 -> 12.0
        area > 0.02 -> 13.0
        area > 0.01 -> 14.0
        else -> 15.0
    }
}

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
            val coords = "${origin.longitude()},${origin.latitude()};${destination.longitude()},${destination.latitude()}"
            val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/$coords" +
                    "?overview=full&geometries=polyline&steps=false&access_token=$accessToken"

            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val body = resp.body?.string() ?: return@use
                val json = JSONObject(body)
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) return@use
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
                }
            }
        } catch (e: Exception) {
            Log.e("MapboxRoute", "Error: ${e.message}")
        }
    }
}

private fun drawRouteOnMap(mapView: MapView, points: List<Point>) {
    mapView.getMapboxMap().getStyle { style ->
        val sourceId = "route-source-compose"
        val layerId = "route-layer-compose"
        val feature = Feature.fromGeometry(LineString.fromLngLats(points))
        val featureCollection = FeatureCollection.fromFeature(feature)

        val existingSource = style.getSource(sourceId) as? GeoJsonSource
        if (existingSource == null) {
            style.addSource(geoJsonSource(sourceId) { featureCollection(featureCollection) })
        } else {
            existingSource.featureCollection(featureCollection)
        }

        if (style.getLayer(layerId) == null) {
            val routeLayer = LineLayer(layerId, sourceId)
                .lineWidth(6.0)
                .lineColor("#00A76D")
                .lineCap(LineCap.ROUND)
            style.addLayer(routeLayer)
        }
    }
}

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