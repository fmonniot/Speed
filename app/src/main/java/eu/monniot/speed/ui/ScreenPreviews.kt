package eu.monniot.speed.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.monniot.speed.Routes
import eu.monniot.speed.SpeedAppShell
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.sensor.SatelliteInfo
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.data.Units
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.util.LocalUnits

// App-shell-level previews of the redesigned screens (G1). Each tab is rendered inside
// SpeedAppShell so the bottom nav + active pill show alongside the screen. Individual screens
// also carry their own @PreviewLightDark previews in their respective files.

private val sampleSessions = listOf(
    SessionSummary(
        sessionId = "1",
        startTimeMs = System.currentTimeMillis() - 1L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 1L * 24 * 3600 * 1000 + 3_600_000L,
        pointCount = 1400,
        maxSpeedMs = 52.8f,
        distanceM = 54_800f,
        avgSpeedMs = 19.4f,
        maxLateralG = 1.24f,
        maxLeanDeg = 52f,
    ),
    SessionSummary(
        sessionId = "2",
        startTimeMs = System.currentTimeMillis() - 9L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 9L * 24 * 3600 * 1000 + 5_400_000L,
        pointCount = 2200,
        maxSpeedMs = 44.4f,
        distanceM = 88_300f,
        avgSpeedMs = 22.1f,
        maxLateralG = 0.91f,
        maxLeanDeg = 38f,
    ),
)

private val sampleServiceState = ServiceState(
    isSensorsEnabled = true,
    currentAccuracyM = 3.5f,
    satellites = SatelliteInfo(usedInFix = 12, visible = 16),
)

@PreviewLightDark
@Composable
private fun PreviewRideTab() {
    RaceLoggerTheme {
        CompositionLocalProvider(LocalUnits provides Units.METRIC) {
            SpeedAppShell(currentRoute = Routes.RIDE, onTabSelected = {}) {
                RideHomeScreen(
                    serviceState = sampleServiceState,
                    gpsRateHz = 10,
                    lastRide = sampleSessions.first(),
                    thisWeekCount = 2,
                    lifetimeDistanceM = 8_412_000f,
                    onRecord = {},
                    onOpenSummary = {},
                    onOpenTrips = {},
                    onOpenStats = {},
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewTripsTab() {
    RaceLoggerTheme {
        CompositionLocalProvider(LocalUnits provides Units.METRIC) {
            SpeedAppShell(currentRoute = Routes.TRIPS, onTabSelected = {}) {
                TripsScreen(
                    sessions = sampleSessions,
                    initialFilter = TripsFilter.ALL,
                    onOpenSummary = {},
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewStatsTab() {
    RaceLoggerTheme {
        CompositionLocalProvider(LocalUnits provides Units.METRIC) {
            SpeedAppShell(currentRoute = Routes.STATS, onTabSelected = {}) {
                StatsScreen(
                    sessions = sampleSessions,
                    segmentCount = 14,
                    onOpenSummary = {},
                    onOpenTrips = {},
                    onOpenSegments = {},
                )
            }
        }
    }
}
