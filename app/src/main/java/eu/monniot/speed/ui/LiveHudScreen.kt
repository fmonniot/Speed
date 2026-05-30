package eu.monniot.speed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Units
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.ui.components.SpeedFullWidthButton
import eu.monniot.speed.ui.components.WavyLine
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.ui.theme.SpeedTextStyles
import eu.monniot.speed.util.Conversions
import eu.monniot.speed.util.FormatUtils
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat

@Composable
fun LiveHudScreen(
    serviceState: ServiceState,
    gpsRateHz: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current

    // Track session top speed locally so the WavyLine progress can be computed.
    var topSoFar by remember { mutableStateOf(0f) }
    if (serviceState.currentSpeedMs > topSoFar) {
        topSoFar = serviceState.currentSpeedMs
    }
    val wavyProgress = if (topSoFar > 0f) {
        (serviceState.currentSpeedMs / topSoFar).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
            .padding(horizontal = SpeedDimens.screenPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1 — Recording banner
            RecordingBanner(
                elapsedSeconds = serviceState.elapsedSeconds,
                pointCount = serviceState.pointCount,
                gpsRateHz = gpsRateHz,
            )

            // 2 — Speed hero card
            SpeedHeroCard(
                serviceState = serviceState,
                units = units,
                topSoFar = topSoFar,
                wavyProgress = wavyProgress,
                modifier = Modifier.padding(top = 8.dp),
            )

            // 3 — Four metric tiles in a 2×2 grid
            MetricTilesGrid(
                serviceState = serviceState,
                units = units,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4 — Stop button pinned near bottom
            SpeedFullWidthButton(
                onClick = onStop,
                label = "Stop",
                icon = Icons.Rounded.Stop,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Recording banner
// ---------------------------------------------------------------------------

@Composable
private fun RecordingBanner(
    elapsedSeconds: Int,
    pointCount: Int,
    gpsRateHz: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(SpeedDimens.radiusBanner),
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: recording indicator + elapsed time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Recording · ${FormatUtils.formatDuration(elapsedSeconds)}",
                fontSize = 13.sp,
                fontWeight = FontWeight(600),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        // Right: GPS rate + point count
        Text(
            text = "$gpsRateHz Hz · $pointCount pts",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
        )
    }
}

// ---------------------------------------------------------------------------
// Speed hero card
// ---------------------------------------------------------------------------

@Composable
private fun SpeedHeroCard(
    serviceState: ServiceState,
    units: Units,
    topSoFar: Float,
    wavyProgress: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(SpeedDimens.radiusHeroCard),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
        ) {
            // Label
            Text(
                text = "CURRENT SPEED",
                fontSize = 13.sp,
                fontWeight = FontWeight(600),
                letterSpacing = 0.06.em,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )

            // Giant numeral + unit suffix
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = UnitFormat.speedValue(serviceState.currentSpeedMs, units),
                    style = SpeedTextStyles.heroLiveSpeed,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = UnitFormat.speedUnit(units),
                    fontSize = 18.sp,
                    fontWeight = FontWeight(500),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }

            // Wavy progress line
            WavyLine(
                progress = wavyProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
            )

            // Footer: avg (placeholder) + top speed so far
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    // TODO(E2): running average not yet in ServiceState
                    text = "avg —",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = "top ${UnitFormat.speedValue(topSoFar, units)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Metric tiles grid
// ---------------------------------------------------------------------------

@Composable
private fun MetricTilesGrid(
    serviceState: ServiceState,
    units: Units,
    modifier: Modifier = Modifier,
) {
    val gForceValue = UnitFormat.lateralGValue(serviceState.currentG) // TODO(E1): true lateral G; currentG is total accel magnitude in G as a stand-in
    val accelValue = String.format(
        java.util.Locale.US,
        "%+.2f",
        Conversions.ms2ToG(serviceState.currentAccelMs2),
    )
    val altitudeValue = serviceState.latestPoint?.altitude?.let {
        UnitFormat.altitudeValue(it.toFloat(), units)
    } ?: "—"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // G-force tile (stand-in for lateral G until E1)
            MetricTile(
                label = "G-FORCE",
                value = gForceValue,
                unit = "g lat",
                icon = Icons.Rounded.Speed,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )

            // Lean tile — TODO(E1): lean angle + direction
            MetricTile(
                label = "LEAN",
                value = "—", // TODO(E1): lean angle + direction
                unit = "right",
                icon = Icons.Filled.TwoWheeler,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Acceleration tile
            MetricTile(
                label = "ACCEL",
                value = accelValue,
                unit = "g",
                icon = Icons.Rounded.TrendingUp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            // Altitude tile
            MetricTile(
                label = "ALTITUDE",
                value = altitudeValue,
                unit = UnitFormat.altitudeUnit(units),
                icon = Icons.Rounded.Terrain,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Single metric tile — DECORATIVE (no onClick)
// ---------------------------------------------------------------------------

@Composable
private fun MetricTile(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(SpeedDimens.radiusMediumTonal),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            // Top row: label + icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight(500),
                    color = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor.copy(alpha = 0.85f),
                )
            }

            // Value + unit suffix
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = value,
                    style = SpeedTextStyles.cardStatLarge,
                    color = contentColor,
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun LiveHudScreenPreview() {
    RaceLoggerTheme {
        val fakePoint = DataPoint(
            id = 0,
            sessionId = "preview-session",
            elapsedRealtimeNs = 0L,
            wallClockMs = 0L,
            latitude = 45.0,
            longitude = 6.0,
            altitude = 1247.0,
            gpsSpeedMs = 39.4f,
            gpsAccuracyM = 3.5f,
            satellitesUsed = 12,
            satellitesVisible = 15,
            accelX = 0f,
            accelY = 0f,
            accelZ = 9.81f,
            accelMagnitude = 9.81f,
            derivedSpeedMs = 39.4f,
            derivedAccelMs2 = 4.1f,
        )
        val fakeState = ServiceState(
            isRecording = true,
            elapsedSeconds = 767,
            pointCount = 7642,
            currentSpeedMs = 39.4f, // ≈ 142 km/h
            currentAccelMs2 = 4.1f,
            currentG = 0.84f,
            latestPoint = fakePoint,
        )
        LiveHudScreen(
            serviceState = fakeState,
            gpsRateHz = 5,
            onStop = {},
        )
    }
}
