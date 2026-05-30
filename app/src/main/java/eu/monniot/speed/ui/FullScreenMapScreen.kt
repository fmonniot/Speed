package eu.monniot.speed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.ui.components.SpeedTopBar
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

// FullScreenMapScreen — F2 full-screen interactive pan/zoom map for a ride's GPS track.
// Expanded version of MapTrackCard (F1). No @Preview — MapLibre cannot render in the preview renderer.
@Composable
fun FullScreenMapScreen(
    points: List<DataPoint>,
    topSpeedLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val geoPoints = remember(points) { points.filter { it.latitude != null && it.longitude != null } }

    // Capture theme colors before leaving Compose scope.
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val errorArgb = MaterialTheme.colorScheme.error.toArgb()
    val whiteArgb = androidx.compose.ui.graphics.Color.White.toArgb()

    Scaffold(
        modifier = modifier,
        topBar = {
            SpeedTopBar(title = "Map", onBack = onBack)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (geoPoints.isEmpty()) {
                Text(
                    text = "No GPS track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                FullScreenMapContent(
                    geoPoints = geoPoints,
                    primaryArgb = primaryArgb,
                    errorArgb = errorArgb,
                    whiteArgb = whiteArgb,
                )
            }

            // Top-speed chip overlay — top start corner, inset below the top bar.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = topSpeedLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight(600),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenMapContent(
    geoPoints: List<DataPoint>,
    primaryArgb: Int,
    errorArgb: Int,
    whiteArgb: Int,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        val mv = MapView(context)
        mv.onCreate(null)
        mv
    }

    // Forward Android lifecycle events to MapView.
    DisposableEffect(lifecycleOwner) {
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
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { mv ->
            mv.getMapAsync { map ->
                val styleJson = """{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}"""

                map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                    val linePoints = geoPoints.map { dp ->
                        Point.fromLngLat(dp.longitude!!, dp.latitude!!)
                    }

                    // Track polyline source + layer.
                    val trackSource = GeoJsonSource(
                        "track",
                        Feature.fromGeometry(LineString.fromLngLats(linePoints)),
                    )
                    style.addSource(trackSource)

                    val trackLayer = LineLayer("track-layer", "track").apply {
                        setProperties(
                            PropertyFactory.lineColor(primaryArgb),
                            PropertyFactory.lineWidth(4f),
                        )
                    }
                    style.addLayer(trackLayer)

                    // Start marker (first point) — primary color.
                    val startPoint = linePoints.first()
                    val startSource = GeoJsonSource(
                        "start-marker",
                        Feature.fromGeometry(startPoint),
                    )
                    style.addSource(startSource)

                    val startLayer = CircleLayer("start-layer", "start-marker").apply {
                        setProperties(
                            PropertyFactory.circleColor(primaryArgb),
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleStrokeColor(whiteArgb),
                            PropertyFactory.circleStrokeWidth(1.5f),
                        )
                    }
                    style.addLayer(startLayer)

                    // End marker (last point) — error color.
                    val endPoint = linePoints.last()
                    val endSource = GeoJsonSource(
                        "end-marker",
                        Feature.fromGeometry(endPoint),
                    )
                    style.addSource(endSource)

                    val endLayer = CircleLayer("end-layer", "end-marker").apply {
                        setProperties(
                            PropertyFactory.circleColor(errorArgb),
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleStrokeColor(whiteArgb),
                            PropertyFactory.circleStrokeWidth(1.5f),
                        )
                    }
                    style.addLayer(endLayer)

                    // Fit camera to track bounds.
                    if (geoPoints.size >= 2) {
                        val boundsBuilder = LatLngBounds.Builder()
                        geoPoints.forEach { dp ->
                            boundsBuilder.include(LatLng(dp.latitude!!, dp.longitude!!))
                        }
                        val bounds = boundsBuilder.build()
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
                    } else if (geoPoints.size == 1) {
                        val dp = geoPoints.first()
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(dp.latitude!!, dp.longitude!!),
                                14.0,
                            ),
                        )
                    }
                }
            }
        },
    )
}
