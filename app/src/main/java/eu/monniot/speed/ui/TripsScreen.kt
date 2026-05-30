package eu.monniot.speed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.ui.components.ListRow
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
    onSearch: () -> Unit,
    onOpenSummary: (String) -> Unit,         // pass the row's sessionId
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current
    var filter by remember { mutableStateOf(initialFilter) }

    val filteredSessions = when (filter) {
        TripsFilter.ALL -> sessions
        TripsFilter.THIS_WEEK -> sessions.filter { isThisWeek(it.startTimeMs) }
    }

    val nameFormat = remember { SimpleDateFormat("EEEE 'ride'", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trips",
                trailingIcon = Icons.Rounded.Search,
                onTrailingAction = onSearch,
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
            // Search bar pill — tap-to-open, NOT a real text field
            item {
                Surface(
                    shape = RoundedCornerShape(SpeedDimens.radiusSearchBar),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SpeedDimens.searchBarHeight)
                        .clickable(onClick = onSearch),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "Search ${sessions.size} trips",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                            text = if (filter == TripsFilter.ALL) "No trips yet" else "No trips this week",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Trip rows, newest-first (order maintained from parent)
                items(filteredSessions, key = { it.sessionId }) { session ->
                    val name = remember(session.startTimeMs) {
                        nameFormat.format(session.startTimeMs)
                    }
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
            onSearch = {},
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
            onSearch = {},
            onOpenSummary = {},
        )
    }
}
