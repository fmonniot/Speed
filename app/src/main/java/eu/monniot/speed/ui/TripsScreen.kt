package eu.monniot.speed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.ui.components.ListRow
import eu.monniot.speed.ui.components.SpeedSearchBar
import eu.monniot.speed.ui.components.SpeedSelectableChip
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.text.SimpleDateFormat
import java.util.Locale

enum class TripsFilter { ALL, THIS_WEEK }

@Composable
fun TripsScreen(
    sessions: List<SessionSummary>,          // newest-first already (parent provides DESC by start time)
    initialFilter: TripsFilter,              // pre-select a chip (e.g. arriving from Ride "This week")
    onOpenSummary: (String) -> Unit,         // pass the row's sessionId
    modifier: Modifier = Modifier,
    dateWindow: Pair<Long, Long>? = null,    // optional [from, to) ms window (e.g. a Stats month bar)
    onDeleteSession: (String) -> Unit = {},  // F1: permanently delete a trip (incl. raw trace file)
) {
    val units = LocalUnits.current
    var filter by remember { mutableStateOf(initialFilter) }
    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    // F1: trip pending delete confirmation (long-press a row). Pair of (sessionId, displayName).
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    pendingDelete?.let { (id, name) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete trip?") },
            text = { Text("\"$name\" and its recorded data will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDeleteSession(id); pendingDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // U4: include start time so multiple rides on the same day are distinguishable.
    val dateFormat = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }

    // Restrict to the date window first (when arriving month-scoped), then chip-filter + search.
    val windowed = if (dateWindow != null) {
        sessions.filter { it.startTimeMs >= dateWindow.first && it.startTimeMs < dateWindow.second }
    } else {
        sessions
    }
    val filteredSessions = when (filter) {
        TripsFilter.ALL -> windowed
        TripsFilter.THIS_WEEK -> windowed.filter { isThisWeek(it.startTimeMs) }
    }.filter { s ->
        query.isBlank() ||
            "${sessionDisplayName(s.name, s.startTimeMs)} ${dateFormat.format(s.startTimeMs)}"
                .contains(query.trim(), ignoreCase = true)
    }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trips",
                trailingIcon = Icons.Rounded.Search,
                onTrailingAction = { searchFocus.requestFocus() },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SpeedDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Search bar pill — live in-place filter over the trip list.
            item {
                SpeedSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search ${windowed.size} trips",
                    focusRequester = searchFocus,
                )
            }

            // Filter chips row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    SpeedSelectableChip(
                        selected = filter == TripsFilter.ALL,
                        onClick = { filter = TripsFilter.ALL },
                        label = "All",
                    )
                    SpeedSelectableChip(
                        selected = filter == TripsFilter.THIS_WEEK,
                        onClick = { filter = TripsFilter.THIS_WEEK },
                        label = "This week",
                    )
                }
            }

            if (filteredSessions.isEmpty()) {
                // Empty state
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                    ) {
                        Text(
                            text = when {
                                query.isNotBlank() -> "No matching trips"
                                filter == TripsFilter.ALL -> "No trips yet"
                                else -> "No trips this week"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Trip rows, newest-first (order maintained from parent)
                items(filteredSessions, key = { it.sessionId }) { session ->
                    val name = sessionDisplayName(session.name, session.startTimeMs)
                    val dateStr = remember(session.startTimeMs) {
                        dateFormat.format(session.startTimeMs)
                    }
                    val distanceStr = session.distanceM?.let { UnitFormat.distance(it, units) } ?: "—"
                    ListRow(
                        title = name,
                        meta = "$dateStr · $distanceStr",
                        leadingIcon = Icons.Filled.TwoWheeler,
                        leadingContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        leadingContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        trailingValue = UnitFormat.speedValue(session.maxSpeedMs ?: 0f, units),
                        trailingUnit = UnitFormat.speedUnit(units),
                        onClick = { onOpenSummary(session.sessionId) },
                        onLongClick = { pendingDelete = session.sessionId to name },
                    )
                }
            }
        }
    }
}

// ---- Previews ----

private val previewSessions = listOf(
    SessionSummary(
        sessionId = "a1b2c3",
        startTimeMs = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L,  // yesterday
        endTimeMs = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L + 3_600_000L,
        pointCount = 1200,
        maxSpeedMs = 28.5f,  // ~102 km/h
        distanceM = 38_400f,
    ),
    SessionSummary(
        sessionId = "d4e5f6",
        startTimeMs = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,  // 3 days ago
        endTimeMs = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L + 2_700_000L,
        pointCount = 890,
        maxSpeedMs = 44.4f,  // ~160 km/h
        distanceM = 54_800f,
    ),
    SessionSummary(
        sessionId = "g7h8i9",
        startTimeMs = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L, // 10 days ago
        endTimeMs = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L + 5_400_000L,
        pointCount = 2100,
        maxSpeedMs = 52.1f,  // ~187 km/h
        distanceM = 71_200f,
    ),
    SessionSummary(
        sessionId = "j0k1l2",
        startTimeMs = System.currentTimeMillis() - 20 * 24 * 60 * 60 * 1000L, // 20 days ago
        endTimeMs = null,
        pointCount = 0,
        maxSpeedMs = null,
    ),
)

@PreviewLightDark
@Composable
private fun TripsScreenPopulatedPreview() {
    RaceLoggerTheme {
        TripsScreen(
            sessions = previewSessions,
            initialFilter = TripsFilter.ALL,
            onOpenSummary = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun TripsScreenEmptyPreview() {
    RaceLoggerTheme {
        TripsScreen(
            sessions = emptyList(),
            initialFilter = TripsFilter.ALL,
            onOpenSummary = {},
        )
    }
}
