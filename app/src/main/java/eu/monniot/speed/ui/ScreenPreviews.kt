package eu.monniot.speed.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination
import eu.monniot.speed.MainDestination
import eu.monniot.speed.SpeedAppShell
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.sensor.SatelliteInfo
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.ui.theme.RaceLoggerTheme

@Composable
private fun mockDestination(route: String): NavDestination = remember(route) {
    NavDestination("mock").apply {
        this.route = route
    }
}

@Preview(showBackground = true, name = "Race Screen - Ready")
@Composable
fun PreviewRaceScreenReady() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentDestination = mockDestination(MainDestination.RACE.route),
            onNavigate = {}
        ) { innerPadding ->
            RaceScreenContent(
                serviceState = ServiceState(
                    isRecording = false,
                    isSensorsEnabled = false,
                    currentSpeedMs = 0f,
                    currentAccelMs2 = 0f,
                    currentG = 1.0f,
                    currentAccuracyM = 3.5f,
                    satellites = SatelliteInfo(usedInFix = 0, visible = 4)
                ),
                onStart = {},
                onStop = {},
                onToggleSensors = {},
                showPastSessions = false,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, name = "Race Screen - Recording")
@Composable
fun PreviewRaceScreenRecording() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentDestination = mockDestination(MainDestination.RACE.route),
            onNavigate = {}
        ) { innerPadding ->
            RaceScreenContent(
                serviceState = ServiceState(
                    isRecording = true,
                    isSensorsEnabled = true,
                    elapsedSeconds = 125,
                    currentSpeedMs = 15.5f,
                    currentAccelMs2 = 1.2f,
                    currentG = 1.15f,
                    currentAccuracyM = 2.1f,
                    satellites = SatelliteInfo(usedInFix = 8, visible = 12)
                ),
                onStart = {},
                onStop = {},
                onToggleSensors = {},
                showPastSessions = false,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun PreviewSettingsScreen() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentDestination = mockDestination(MainDestination.SETTINGS.route),
            onNavigate = {}
        ) { innerPadding ->
            SettingsScreenContent(
                autoStart = true,
                onAutoStartChange = {},
                batteryWattage = -0.52f,
                batteryCapacityMah = 4500,
                batteryTimeRemainingMs = 12600000L,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, name = "Sessions Screen")
@Composable
fun PreviewSessionsScreen() {
    val mockSessions = listOf(
        SessionSummary("1", startTimeMs = 1715000000000L, endTimeMs = 1715003600000L, pointCount = 1200, maxSpeedMs = 25.5f),
        SessionSummary("2", startTimeMs = 1715100000000L, endTimeMs = 1715103600000L, pointCount = 1500, maxSpeedMs = 30.2f),
        SessionSummary("3", startTimeMs = 1715200000000L, endTimeMs = 1715203600000L, pointCount = 800, maxSpeedMs = 18.9f)
    )
    RaceLoggerTheme {
        SpeedAppShell(
            currentDestination = mockDestination(MainDestination.SESSIONS.route),
            onNavigate = {}
        ) { innerPadding ->
            SessionsScreenContent(
                sessions = mockSessions,
                onSessionClick = {},
                onDelete = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, name = "Session Detail Screen")
@Composable
fun PreviewSessionDetailScreen() {
    val mockPoints = List(20) { i ->
        DataPoint(
            id = i.toLong(),
            sessionId = "1",
            elapsedRealtimeNs = i * 1_000_000_000L,
            wallClockMs = 1715000000000L + (i * 1000),
            latitude = 48.8566,
            longitude = 2.3522,
            altitude = 35.0,
            gpsSpeedMs = (i * 2f).coerceAtMost(25f),
            gpsAccuracyM = 3.0f,
            satellitesUsed = 8,
            satellitesVisible = 12,
            accelX = 0f,
            accelY = 0f,
            accelZ = 9.8f,
            accelMagnitude = 9.8f,
            derivedSpeedMs = null,
            derivedAccelMs2 = null
        )
    }
    RaceLoggerTheme {
        // We do not wrap Detail screen in SpeedAppShell because the detail screen 
        // handles its own Scaffold and TopAppBar, hiding the navigation bar in reality.
        SessionDetailContent(
            points = mockPoints,
            onBack = {},
            onExport = {},
            onDelete = {}
        )
    }
}
