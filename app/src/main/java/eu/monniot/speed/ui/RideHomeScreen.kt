package eu.monniot.speed.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.data.Units
import eu.monniot.speed.sensor.SatelliteInfo
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.ui.components.SpeedExtendedFab
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.components.TonalStatCard
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.ui.theme.SpeedTextStyles
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.text.SimpleDateFormat
import java.util.Locale

// §4.1 — Ride · Ready/Home screen
@Composable
fun RideHomeScreen(
    serviceState: ServiceState,
    gpsRateHz: Int,
    lastRide: SessionSummary?,
    thisWeekCount: Int,
    onRecord: () -> Unit,
    onOpenSummary: (String) -> Unit,   // pass the lastRide.sessionId
    onOpenTrips: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current

    Scaffold(
        topBar = {
            SpeedTopBar(title = "Speed")
        },
        floatingActionButton = {
            SpeedExtendedFab(
                onClick = onRecord,
                label = "Record",
                icon = Icons.Rounded.FiberManualRecord,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpeedDimens.screenPadding)
                .padding(bottom = 88.dp), // give room for FAB
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Heading
            Text(
                text = "Ready to ride",
                style = MaterialTheme.typography.headlineMedium,
            )

            // Status subtext
            val statusText = if (serviceState.isSensorsEnabled) {
                "All sensors locked · sampling at $gpsRateHz Hz"
            } else {
                "Sensors off · sampling at $gpsRateHz Hz"
            }
            Text(
                text = statusText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Sensor chips row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                val gpsLabel = if (serviceState.currentAccuracyM != null) {
                    "GPS ${serviceState.satellites.usedInFix}"
                } else {
                    "GPS —"
                }
                SensorChip(icon = Icons.Rounded.GpsFixed, label = gpsLabel)

                val imuLabel = if (serviceState.isSensorsEnabled) "IMU on" else "IMU off"
                SensorChip(icon = Icons.Rounded.Sensors, label = imuLabel)

                SensorChip(
                    icon = Icons.Rounded.BatteryFull,
                    label = "—%", // TODO(E3): battery % not yet in ServiceState
                )
            }

            // Last-ride card
            Spacer(modifier = Modifier.height(18.dp))
            LastRideCard(
                lastRide = lastRide,
                units = units,
                onOpenSummary = onOpenSummary,
            )

            // Two summary stat cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                // "This week" card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenTrips),
                ) {
                    TonalStatCard(
                        value = "$thisWeekCount rides",
                        caption = "This week",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        label = "THIS WEEK",
                        icon = Icons.Rounded.DirectionsBike,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // "Total" card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenStats),
                ) {
                    TonalStatCard(
                        value = "—", // TODO(E3): lifetime distance
                        caption = "Total distance",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        label = "TOTAL",
                        icon = Icons.Rounded.Straighten,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// Private helper: decorative bordered sensor chip (non-interactive)
@Composable
private fun SensorChip(
    icon: ImageVector,
    label: String,
) {
    Surface(
        shape = RoundedCornerShape(SpeedDimens.radiusChip),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(SpeedDimens.chipHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Private helper: last-ride hero card
@Composable
private fun LastRideCard(
    lastRide: SessionSummary?,
    units: Units,
    onOpenSummary: (String) -> Unit,
) {
    val cardModifier = if (lastRide != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onOpenSummary(lastRide.sessionId) }
    } else {
        Modifier.fillMaxWidth()
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(SpeedDimens.radiusHeroCard),
        modifier = cardModifier,
    ) {
        if (lastRide == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
            ) {
                Text(
                    text = "No rides yet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                // Header row
                val dateFmt = SimpleDateFormat("d MMM", Locale.getDefault())
                val dateStr = dateFmt.format(lastRide.startTimeMs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LAST RIDE · $dateStr",
                        fontSize = 12.sp,
                        fontWeight = FontWeight(600),
                        letterSpacing = 0.08.em,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.TwoWheeler,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Session derived name (e.g. "Monday ride")
                val nameFmt = SimpleDateFormat("EEEE 'ride'", Locale.getDefault())
                val sessionName = nameFmt.format(lastRide.startTimeMs)
                Text(
                    text = sessionName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 6.dp),
                )

                // Three inline stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    // Top speed
                    InlineStat(
                        value = UnitFormat.speedValue(lastRide.maxSpeedMs ?: 0f, units),
                        caption = UnitFormat.speedUnit(units),
                    )
                    // Max lateral G
                    InlineStat(
                        value = "—", // TODO(E2/E3): real value
                        caption = "g lat",
                    )
                    // Distance
                    InlineStat(
                        value = "—", // TODO(E2): session distance
                        caption = UnitFormat.distanceUnit(units),
                    )
                }
            }
        }
    }
}

// Private helper: a single value+caption stat inside the last-ride card
@Composable
private fun InlineStat(
    value: String,
    caption: String,
) {
    Column {
        Text(
            text = value,
            style = SpeedTextStyles.cardStatLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = caption,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
    }
}

// --- Previews ---

@PreviewLightDark
@Composable
private fun RideHomeScreenPreview() {
    val fakeServiceState = ServiceState(
        isSensorsEnabled = true,
        currentAccuracyM = 3.5f,
        satellites = SatelliteInfo(usedInFix = 12, visible = 16),
        pointCount = 0,
    )
    val fakeLastRide = SessionSummary(
        sessionId = "preview-session-001",
        startTimeMs = 1_716_800_000_000L, // a Monday in May 2024
        endTimeMs = 1_716_803_600_000L,
        pointCount = 3600,
        maxSpeedMs = 22.5f, // ~81 km/h
    )

    RaceLoggerTheme {
        CompositionLocalProvider(LocalUnits provides Units.METRIC) {
            RideHomeScreen(
                serviceState = fakeServiceState,
                gpsRateHz = 5,
                lastRide = fakeLastRide,
                thisWeekCount = 4,
                onRecord = {},
                onOpenSummary = {},
                onOpenTrips = {},
                onOpenStats = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RideHomeScreenNoRidePreview() {
    val fakeServiceState = ServiceState(
        isSensorsEnabled = false,
        currentAccuracyM = null,
        satellites = SatelliteInfo(usedInFix = 0, visible = 0),
        pointCount = 0,
    )

    RaceLoggerTheme {
        CompositionLocalProvider(LocalUnits provides Units.METRIC) {
            RideHomeScreen(
                serviceState = fakeServiceState,
                gpsRateHz = 1,
                lastRide = null,
                thisWeekCount = 0,
                onRecord = {},
                onOpenSummary = {},
                onOpenTrips = {},
                onOpenStats = {},
            )
        }
    }
}
