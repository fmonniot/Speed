package eu.monniot.speed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.monniot.speed.ui.components.SpeedExtendedFab
import eu.monniot.speed.ui.components.SpeedFullWidthButton
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.SpeedDimens

// B2 placeholder screens. Each wires the correct top bar (via SpeedTopBar) and the
// §4 navigation interactions so the whole app is tappable before the real D-phase
// screens land. Bottom-nav visibility is driven by MainActivity (Routes.bottomBar).
// Ride (D1) and Live (D2) get real screens in the same phase; the rest stay placeholders
// until later D-tasks. A literal sample id is used where a real session id isn't known yet.

private const val SAMPLE_ID = "sample"

@Composable
private fun PlaceholderColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SpeedDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

// --- Ride · Home (§4.1) — bottom nav active = Ride. Replaced by D1 RideHomeScreen. ---
@Composable
fun RidePlaceholder(
    onRecord: () -> Unit,
    onOpenSummary: (String) -> Unit,
    onOpenTrips: () -> Unit,
    onOpenStats: () -> Unit,
) {
    Scaffold(
        topBar = { SpeedTopBar(title = "Speed") },
        floatingActionButton = {
            SpeedExtendedFab(
                onClick = onRecord,
                label = "Record",
                icon = Icons.Rounded.FiberManualRecord,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Ready to ride", style = MaterialTheme.typography.headlineMedium)
            SpeedFullWidthButton(onClick = { onOpenSummary(SAMPLE_ID) }, label = "Last ride summary")
            SpeedFullWidthButton(onClick = onOpenTrips, label = "This week")
            SpeedFullWidthButton(onClick = onOpenStats, label = "Total")
        }
    }
}

// --- Ride · Live HUD (§4.2) — no top bar / no nav. Replaced by D2 LiveHudScreen. ---
@Composable
fun LivePlaceholder(
    onStop: (String) -> Unit,
) {
    Scaffold { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Live HUD", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(
                onClick = { onStop(SAMPLE_ID) },
                label = "Stop",
            )
        }
    }
}

// --- Trips · List (§4.4) ---
@Composable
fun TripsPlaceholder(
    onSearch: () -> Unit,
    onOpenSummary: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trips",
                trailingIcon = Icons.Rounded.Search,
                onTrailingAction = onSearch,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Trips list", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(onClick = { onOpenSummary(SAMPLE_ID) }, label = "Open a trip summary")
        }
    }
}

// --- Ride · Summary (§4.3) ---
@Composable
fun SummaryPlaceholder(
    sessionId: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenTrace: (String) -> Unit,
    onOpenSegments: () -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trip summary",
                onBack = onBack,
                trailingIcon = Icons.Rounded.Share,
                onTrailingAction = onShare,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Summary · $sessionId", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(onClick = { onOpenTrace(sessionId) }, label = "Open trace (top-speed moment)")
            SpeedFullWidthButton(onClick = onOpenSegments, label = "Segments with a PB this ride")
        }
    }
}

// --- Trips · Trace (§4.5) ---
@Composable
fun TracePlaceholder(
    sessionId: String,
    onBack: () -> Unit,
    onDownload: () -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trace",
                onBack = onBack,
                trailingIcon = Icons.Rounded.Download,
                onTrailingAction = onDownload,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Trace · $sessionId", style = MaterialTheme.typography.headlineSmall)
            Text("Map + speed/G charts pending (D5/F1).", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// --- Stats · Overview (§4.6) ---
@Composable
fun StatsPlaceholder(
    onDateRange: () -> Unit,
    onOpenSummary: (String) -> Unit,
    onOpenTrips: () -> Unit,
    onOpenSegments: () -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Statistics",
                trailingIcon = Icons.Rounded.DateRange,
                onTrailingAction = onDateRange,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Statistics", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(onClick = onOpenTrips, label = "A month's trips")
            SpeedFullWidthButton(onClick = { onOpenSummary(SAMPLE_ID) }, label = "Ride holding a record")
            SpeedFullWidthButton(onClick = onOpenSegments, label = "Tracked segments")
        }
    }
}

// --- Stats · Segment list (§4.7) — bottom nav active = Stats ---
@Composable
fun SegmentsPlaceholder(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenSegment: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Segments",
                onBack = onBack,
                trailingIcon = Icons.Rounded.Add,
                onTrailingAction = onAdd,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Segments", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(onClick = { onOpenSegment(SAMPLE_ID) }, label = "Open a segment")
        }
    }
}

// --- Stats · Segment detail (§4.8) ---
@Composable
fun SegmentDetailPlaceholder(
    segmentId: String,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onOpenTrace: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Segment",
                onBack = onBack,
                trailingIcon = Icons.Rounded.MoreVert,
                onTrailingAction = onMore,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Segment · $segmentId", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(onClick = { onOpenTrace(SAMPLE_ID) }, label = "Trace of the PB attempt")
        }
    }
}

// --- Settings (§4.9) — bottom nav active = Settings ---
@Composable
fun SettingsPlaceholder(
    onHelp: () -> Unit,
    onExportAll: () -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Settings",
                trailingIcon = Icons.Rounded.HelpOutline,
                onTrailingAction = onHelp,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            SpeedFullWidthButton(onClick = onExportAll, label = "Export all")
        }
    }
}

// --- Settings · Export (§4.10) ---
@Composable
fun ExportPlaceholder(
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Export data",
                onBack = onBack,
                trailingIcon = Icons.Rounded.HelpOutline,
                onTrailingAction = onHelp,
            )
        },
    ) { padding ->
        PlaceholderColumn(Modifier.padding(padding)) {
            Text("Export data", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
