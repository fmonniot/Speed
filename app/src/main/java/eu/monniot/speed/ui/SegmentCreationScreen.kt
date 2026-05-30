package eu.monniot.speed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.data.Units
import eu.monniot.speed.ui.components.MapTrackCard
import eu.monniot.speed.ui.components.SectionLabel
import eu.monniot.speed.ui.components.SpeedFullWidthButton
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ---- Haversine helper -------------------------------------------------------

/**
 * Haversine distance in metres between two lat/lon coordinates.
 */
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLon = (lon2 - lon1) * PI / 180.0
    val a = sin(dLat / 2).let { it * it } +
        cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
        sin(dLon / 2).let { it * it }
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return abs(r * c)
}

/**
 * Sum of haversine distances between consecutive points that have valid lat/lon.
 * Returns 0.0 when there are fewer than two valid-coordinate points.
 */
private fun subTrackDistanceMeters(points: List<DataPoint>): Float {
    var total = 0.0
    var prevLat: Double? = null
    var prevLon: Double? = null
    for (p in points) {
        val lat = p.latitude ?: continue
        val lon = p.longitude ?: continue
        val pLat = prevLat
        val pLon = prevLon
        if (pLat != null && pLon != null) {
            total += haversineMeters(pLat, pLon, lat, lon)
        }
        prevLat = lat
        prevLon = lon
    }
    return total.toFloat()
}

// ---- Ride label helper ------------------------------------------------------

private fun rideLabel(summary: SessionSummary, units: Units): String {
    val dayFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    val dateStr = dayFormat.format(summary.startTimeMs)
    val speed = summary.maxSpeedMs?.let { UnitFormat.speed(it, units) } ?: "—"
    return "$dateStr · $speed"
}

// ---- Screen -----------------------------------------------------------------

/**
 * Stateless screen (E6 §4.7 Add) — lets the user:
 *  1. Pick a source ride from [rides].
 *  2. Drag a RangeSlider to select a sub-stretch of that ride's GPS track.
 *  3. Name the segment and tap "Save segment".
 *
 * Layout:
 *  - SpeedTopBar("New segment", onBack = onCancel)
 *  - Scrollable Column:
 *      • SectionLabel "From ride" + clickable dropdown of rides
 *      • (after ride selected) Loading indicator or:
 *          - RangeSlider over the loaded points
 *          - MapTrackCard preview of the selected sub-track
 *          - Distance / point-count summary row
 *      • OutlinedTextField for segment name
 *      • SpeedFullWidthButton "Save segment"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentCreationScreen(
    rides: List<SessionSummary>,
    loadPoints: suspend (sessionId: String) -> List<DataPoint>,
    onCancel: () -> Unit,
    onSave: (name: String, sessionId: String, points: List<DataPoint>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current

    // --- state ---
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var points by remember { mutableStateOf<List<DataPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var startFraction by remember { mutableFloatStateOf(0f) }
    var endFraction by remember { mutableFloatStateOf(1f) }
    var name by remember { mutableStateOf("") }

    // Load points whenever the selected ride changes.
    LaunchedEffect(selectedSessionId) {
        val id = selectedSessionId
        if (id == null) {
            points = emptyList()
            startFraction = 0f
            endFraction = 1f
            return@LaunchedEffect
        }
        isLoading = true
        points = emptyList()
        startFraction = 0f
        endFraction = 1f
        points = loadPoints(id)
        isLoading = false
    }

    // Sub-track recomputed from slider fractions + points.
    val subTrack = remember(points, startFraction, endFraction) {
        if (points.isEmpty()) return@remember emptyList()
        val n = points.size
        val start = (startFraction * (n - 1)).roundToInt().coerceIn(0, n - 1)
        val end = (endFraction * (n - 1)).roundToInt().coerceIn(0, n - 1)
        val safeEnd = if (end <= start) (start + 1).coerceAtMost(n - 1) else end
        points.subList(start, safeEnd + 1)
    }

    val subTrackValidPoints = remember(subTrack) {
        subTrack.count { it.latitude != null && it.longitude != null }
    }

    val subTrackDistanceM = remember(subTrack) { subTrackDistanceMeters(subTrack) }

    val canSave = selectedSessionId != null &&
        subTrackValidPoints >= 2 &&
        name.isNotBlank()

    // --- UI ---
    Scaffold(
        topBar = {
            SpeedTopBar(title = "New segment", onBack = onCancel)
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpeedDimens.screenPadding)
                .padding(bottom = SpeedDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ---- Step 1: pick a ride ----------------------------------------
            SectionLabel("From ride")

            RideDropdown(
                rides = rides,
                selectedId = selectedSessionId,
                units = units,
                onSelect = { selectedSessionId = it },
            )

            // ---- No ride selected hint ----------------------------------------
            if (selectedSessionId == null) {
                Text(
                    text = "Pick a ride to mark a segment from its track.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // ---- Step 2: loading / range selector + map preview ---------------
            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    CircularProgressIndicator()
                }
            } else if (points.isNotEmpty()) {
                SectionLabel("Select stretch")

                RangeSlider(
                    value = startFraction..endFraction,
                    onValueChange = { range ->
                        startFraction = range.start
                        endFraction = range.endInclusive
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Map preview of the selected sub-track.
                MapTrackCard(
                    points = subTrack,
                    topSpeedLabel = "",
                    playheadIndex = null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Distance + point-count info row.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatChip(
                        label = "Distance",
                        value = UnitFormat.distance(subTrackDistanceM, units),
                        modifier = Modifier.weight(1f),
                    )
                    StatChip(
                        label = "Points",
                        value = "${subTrack.size}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ---- Step 3: name + save -----------------------------------------
            if (selectedSessionId != null) {
                SectionLabel("Name")

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Segment name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                SpeedFullWidthButton(
                    onClick = {
                        val id = selectedSessionId ?: return@SpeedFullWidthButton
                        onSave(name.trim(), id, subTrack)
                    },
                    label = "Save segment",
                    icon = Icons.Rounded.Check,
                    containerColor = if (canSave)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (canSave)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---- Ride dropdown ----------------------------------------------------------

@Composable
private fun RideDropdown(
    rides: List<SessionSummary>,
    selectedId: String?,
    units: Units,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = selectedId?.let { id ->
        rides.find { it.sessionId == id }?.let { rideLabel(it, units) }
    } ?: "Select a ride…"

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(SpeedDimens.radiusBorderedRow),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SpeedDimens.radiusBorderedRow))
                .clickable(enabled = rides.isNotEmpty()) { expanded = true },
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedId != null)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            if (rides.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No rides available") },
                    onClick = { expanded = false },
                    enabled = false,
                )
            } else {
                rides.forEach { ride ->
                    DropdownMenuItem(
                        text = { Text(rideLabel(ride, units)) },
                        onClick = {
                            onSelect(ride.sessionId)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// ---- Small stat chip --------------------------------------------------------

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(SpeedDimens.radiusSmallStat),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Previews ---------------------------------------------------------------

private val previewRides = listOf(
    SessionSummary(
        sessionId = "ride-1",
        startTimeMs = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L,
        endTimeMs = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L + 3_600_000L,
        pointCount = 1200,
        maxSpeedMs = 28.5f,
        distanceM = 42_000f,
    ),
    SessionSummary(
        sessionId = "ride-2",
        startTimeMs = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L,
        endTimeMs = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L + 2_700_000L,
        pointCount = 890,
        maxSpeedMs = 44.4f,
        distanceM = 65_000f,
    ),
)

@PreviewLightDark
@Composable
private fun SegmentCreationScreenPreview() {
    RaceLoggerTheme {
        SegmentCreationScreen(
            rides = previewRides,
            loadPoints = { emptyList() },
            onCancel = {},
            onSave = { _, _, _ -> },
        )
    }
}
