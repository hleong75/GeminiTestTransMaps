package com.example.geminitesttransmaps.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
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
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    val hasLocationPermission = remember { mutableStateOf(hasLocationPermission(context)) }
    var isLocationEnabled by remember { mutableStateOf(false) }
    var isTrackingEnabled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission.value = granted
        if (granted) {
            mapboxMap?.let { mapInstance ->
                mapStyle?.let { style ->
                    if (enableUserLocation(context, mapInstance, style)) {
                        isLocationEnabled = true
                        isTrackingEnabled = true
                    }
                }
            }
        } else {
            onLocationPermissionDenied?.invoke()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { mapInstance ->
            mapboxMap = mapInstance
        }
    }

    LaunchedEffect(styleUri, mapboxMap) {
        val mapInstance = mapboxMap ?: return@LaunchedEffect
        mapInstance.setStyle(Style.Builder().fromUri(styleUri)) { style ->
            mapStyle = style
            ensureStopLayer(style)
            updateStopSource(style, stops)
            if (hasLocationPermission.value) {
                if (enableUserLocation(context, mapInstance, style)) {
                    isLocationEnabled = true
                    isTrackingEnabled = true
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
                text = "Offline map",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        FloatingActionButton(
            onClick = {
                if (hasLocationPermission.value) {
                    mapboxMap?.let { mapInstance ->
                        if (isLocationEnabled) {
                            val nextMode = if (isTrackingEnabled) {
                                CameraMode.NONE
                            } else {
                                CameraMode.TRACKING
                            }
                            mapInstance.locationComponent.cameraMode = nextMode
                            isTrackingEnabled = nextMode == CameraMode.TRACKING
                        } else {
                            mapStyle?.let { style ->
                                if (enableUserLocation(context, mapInstance, style)) {
                                    isLocationEnabled = true
                                    isTrackingEnabled = true
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
    if (style.getSource(MapScreenDefaults.STOP_SOURCE_ID) == null) {
        style.addSource(
            GeoJsonSource(
                MapScreenDefaults.STOP_SOURCE_ID,
                FeatureCollection.fromFeatures(emptyList()),
            ),
        )
    }
    if (style.getLayer(MapScreenDefaults.STOP_LAYER_ID) == null) {
        style.addLayer(
            CircleLayer(
                MapScreenDefaults.STOP_LAYER_ID,
                MapScreenDefaults.STOP_SOURCE_ID,
            ).withProperties(
                circleColor(MapScreenDefaults.STOP_CIRCLE_COLOR),
                circleRadius(MapScreenDefaults.STOP_CIRCLE_RADIUS),
                circleStrokeColor(MapScreenDefaults.STOP_STROKE_COLOR),
                circleStrokeWidth(MapScreenDefaults.STOP_STROKE_WIDTH),
            ),
        )
    }
}

private fun updateStopSource(style: Style, stops: List<StopEntity>) {
    val features = stops.map { stop ->
        Feature.fromGeometry(Point.fromLngLat(stop.stopLon, stop.stopLat))
    }
    style.getSourceAs<GeoJsonSource>(MapScreenDefaults.STOP_SOURCE_ID)
        ?.setGeoJson(FeatureCollection.fromFeatures(features))
}

private fun enableUserLocation(context: Context, map: MapboxMap, style: Style): Boolean {
    if (!hasLocationPermission(context)) {
        return false
    }
    val locationComponent = map.locationComponent
    if (!locationComponent.isLocationComponentActivated) {
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style).build(),
        )
    }
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
            onCreate(Bundle())
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

private object MapScreenDefaults {
    const val STOP_SOURCE_ID = "stops-source"
    const val STOP_LAYER_ID = "stops-layer"
    const val STOP_CIRCLE_COLOR = "#1E88E5"
    const val STOP_STROKE_COLOR = "#FFFFFF"
    const val STOP_CIRCLE_RADIUS = 5f
    const val STOP_STROKE_WIDTH = 1.5f
}
