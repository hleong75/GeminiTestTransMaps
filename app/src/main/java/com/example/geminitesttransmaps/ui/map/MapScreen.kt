package com.example.geminitesttransmaps.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.geminitesttransmaps.data.local.StopEntity
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapboxMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

@Composable
fun MapScreen(
    styleUri: String,
    stops: List<StopEntity>,
    modifier: Modifier = Modifier,
    onLocationPermissionDenied: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var mapLibreMap by remember { mutableStateOf<MapboxMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    val hasLocationPermission = remember { mutableStateOf(hasLocationPermission(context)) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission.value = granted
        if (granted) {
            mapLibreMap?.let { map ->
                mapStyle?.let { style ->
                    if (enableUserLocation(context, map, style)) {
                        isLocationEnabled = true
                    }
                }
            }
        } else {
            onLocationPermissionDenied?.invoke()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
        }
    }

    LaunchedEffect(styleUri, mapLibreMap) {
        mapLibreMap?.setStyle(Style.Builder().fromUri(styleUri)) { style ->
            mapStyle = style
            ensureStopLayer(style)
            updateStopSource(style, stops)
            if (hasLocationPermission.value) {
                val map = mapLibreMap ?: return@setStyle
                if (enableUserLocation(context, map, style)) {
                    isLocationEnabled = true
                }
            }
        }
    }

    LaunchedEffect(stops, mapStyle) {
        mapStyle?.let { style -> updateStopSource(style, stops) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = "Carte hors-ligne",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        FloatingActionButton(
            onClick = {
                if (hasLocationPermission.value) {
                    mapLibreMap?.let { map ->
                        if (isLocationEnabled) {
                            map.locationComponent.cameraMode = CameraMode.TRACKING
                        } else {
                            mapStyle?.let { style ->
                                if (enableUserLocation(context, map, style)) {
                                    isLocationEnabled = true
                                }
                            }
                        }
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Text(text = "GPS", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun ensureStopLayer(style: Style) {
    if (style.getSource(STOP_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(STOP_SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
    }
    if (style.getLayer(STOP_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(STOP_LAYER_ID, STOP_SOURCE_ID).withProperties(
                circleColor("#1E88E5"),
                circleRadius(5f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(1.5f),
            ),
        )
    }
}

private fun updateStopSource(style: Style, stops: List<StopEntity>) {
    val features = stops.map { stop ->
        Feature.fromGeometry(Point.fromLngLat(stop.stopLon, stop.stopLat))
    }
    style.getSourceAs<GeoJsonSource>(STOP_SOURCE_ID)
        ?.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun enableUserLocation(context: Context, map: MapboxMap, style: Style): Boolean {
    if (!hasLocationPermission(context)) {
        return false
    }
    val locationComponent = map.locationComponent
    locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style).build(),
    )
    locationComponent.isLocationComponentEnabled = true
    locationComponent.cameraMode = CameraMode.TRACKING
    locationComponent.renderMode = RenderMode.COMPASS
    return true
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return mapView
}

private const val STOP_SOURCE_ID = "stops-source"
private const val STOP_LAYER_ID = "stops-layer"
