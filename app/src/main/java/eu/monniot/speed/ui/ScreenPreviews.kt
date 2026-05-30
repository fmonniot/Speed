package eu.monniot.speed.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import eu.monniot.speed.Routes
import eu.monniot.speed.SpeedAppShell
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.sensor.SatelliteInfo
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.ui.theme.RaceLoggerTheme

@Preview(showBackground = true, showSystemUi = true, name = "Race Screen - Ready")
@Composable
fun PreviewRaceScreenReady() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentRoute = Routes.RIDE,
            onTabSelected = {}
        ) {
            RaceScreenContent(
                serviceState = ServiceState(
                    isRecording = false,
                    isSensorsEnabled = true,
                    currentSpeedMs = 0f,
                    currentAccelMs2 = 0f,
                    currentG = 1.0f,
                    currentAccuracyM = 3.5f,
                    satellites = SatelliteInfo(usedInFix = 6, visible = 10)
                ),
                onStart = {},
                onStop = {},
                onToggleSensors = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Race Screen - Sensors Disabled")
@Composable
fun PreviewRaceScreenSensorsDisabled() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentRoute = Routes.RIDE,
            onTabSelected = {}
        ) {
            RaceScreenContent(
                serviceState = ServiceState(
                    isRecording = false,
                    isSensorsEnabled = false,
                    currentSpeedMs = 0f,
                    currentAccelMs2 = 0f,
                    currentG = 0f,
                    currentAccuracyM = null,
                    satellites = SatelliteInfo(usedInFix = 0, visible = 0)
                ),
                onStart = {},
                onStop = {},
                onToggleSensors = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Race Screen - Recording")
@Composable
fun PreviewRaceScreenRecording() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentRoute = Routes.RIDE,
            onTabSelected = {}
        ) {
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
                onToggleSensors = {}
            )
        }
    }
}

// The legacy Settings preview was removed when the old SettingsScreenContent was replaced by the
// D9 SettingsScreen (which carries its own @PreviewLightDark). Remaining legacy previews below are
// cleaned up in G1.

@Preview(showBackground = true, showSystemUi = true, name = "Sessions Screen - Populated")
@Composable
fun PreviewSessionsScreenPopulated() {
    val mockSessions = listOf(
        SessionSummary("1", startTimeMs = 1715000000000L, endTimeMs = 1715003600000L, pointCount = 1200, maxSpeedMs = 25.5f),
        SessionSummary("2", startTimeMs = 1715100000000L, endTimeMs = 1715103600000L, pointCount = 1500, maxSpeedMs = 30.2f),
        SessionSummary("3", startTimeMs = 1715200000000L, endTimeMs = 1715203600000L, pointCount = 800, maxSpeedMs = 18.9f)
    )
    RaceLoggerTheme {
        SpeedAppShell(
            currentRoute = Routes.TRIPS,
            onTabSelected = {}
        ) {
            SessionsScreenContent(
                sessions = mockSessions,
                onSessionClick = {},
                onDelete = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Sessions Screen - Empty")
@Composable
fun PreviewSessionsScreenEmpty() {
    RaceLoggerTheme {
        SpeedAppShell(
            currentRoute = Routes.TRIPS,
            onTabSelected = {}
        ) {
            SessionsScreenContent(
                sessions = emptyList(),
                onSessionClick = {},
                onDelete = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Session Detail Screen")
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
    val mockSession = Session(
        sessionId = "1",
        startTimeMs = 1715000000000L,
        endTimeMs = 1715003600000L,
        pointCount = 20,
        maxSpeedMs = 25f,
        notes = "This was a great testing session on the track."
    )
    RaceLoggerTheme {
        SessionDetailContent(
            session = mockSession,
            points = mockPoints,
            onBack = {},
            onExport = {},
            onDelete = {},
            onNotesChange = {}
        )
    }
}
