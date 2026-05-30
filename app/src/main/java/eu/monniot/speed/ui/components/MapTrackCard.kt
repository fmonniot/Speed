package eu.monniot.speed.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import eu.monniot.speed.data.DataPoint
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

// MapTrackCard — renders a session's GPS track on an OSM raster map via MapLibre.
// Shows start/end circle markers, a top-speed chip overlay, and an optional scrub playhead.
// NOTE: No @Preview — MapLibre cannot render in the preview renderer.
@Composable
fun MapTrackCard(
    points: List<DataPoint>,
    topSpeedLabel: String,
    playheadIndex: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val geoPoints = remember(points) { points.filter { it.latitude != null && it.longitude != null } }

    // Capture theme colors before leaving Compose scope.
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    val errorArgb = MaterialTheme.colorScheme.error.toArgb()
    val tertiaryArgb = MaterialTheme.colorScheme.tertiary.toArgb()
    val whiteArgb = androidx.compose.ui.graphics.Color.White.toArgb()

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick),
    ) {
        Box {
            if (geoPoints.isEmpty()) {
                // Empty state
                Text(
                    text = "No GPS track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                MapContent(
                    geoPoints = geoPoints,
                    primaryArgb = primaryArgb,
                    errorArgb = errorArgb,
                    tertiaryArgb = tertiaryArgb,
                    whiteArgb = whiteArgb,
                    playheadIndex = playheadIndex,
                    points = points,
                )
            }

            // Top-speed chip overlay — top start corner.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
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
private fun MapContent(
    geoPoints: List<DataPoint>,
    primaryArgb: Int,
    errorArgb: Int,
    tertiaryArgb: Int,
    whiteArgb: Int,
    playheadIndex: Int?,
    points: List<DataPoint>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Hold a reference to the loaded style so the playhead LaunchedEffect can access it.
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

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

    // Update playhead marker when playheadIndex changes.
    LaunchedEffect(playheadIndex) {
        val style = loadedStyle ?: return@LaunchedEffect
        val source = style.getSourceAs<GeoJsonSource>("playhead") ?: return@LaunchedEffect

        if (playheadIndex != null && playheadIndex in points.indices) {
            val dp = points[playheadIndex]
            val lat = dp.latitude ?: return@LaunchedEffect
            val lon = dp.longitude ?: return@LaunchedEffect
            source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(lon, lat)))
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { mv ->
            mv.getMapAsync { map ->
                val styleJson = """
                    {"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}
                """.trimIndent()

                map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                    loadedStyle = style

                    // Build the track line geometry.
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

                    // Playhead marker — tertiary color, initially hidden at first point.
                    val playheadSource = GeoJsonSource(
                        "playhead",
                        Feature.fromGeometry(startPoint),
                    )
                    style.addSource(playheadSource)

                    val playheadLayer = CircleLayer("playhead-layer", "playhead").apply {
                        setProperties(
                            PropertyFactory.circleColor(tertiaryArgb),
                            PropertyFactory.circleRadius(6f),
                            PropertyFactory.circleStrokeColor(whiteArgb),
                            PropertyFactory.circleStrokeWidth(1.5f),
                        )
                    }
                    style.addLayer(playheadLayer)

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
