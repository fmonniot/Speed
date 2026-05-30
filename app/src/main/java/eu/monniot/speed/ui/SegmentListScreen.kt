package eu.monniot.speed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.SegmentListItem
import eu.monniot.speed.data.Units
import eu.monniot.speed.ui.components.SpeedSelectableChip
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat

enum class SegmentSort { BEST_TIME, MOST_RUNS, NEARBY }

@Composable
fun SegmentListScreen(
    segments: List<SegmentListItem>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onOpenSegment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current
    var sort by remember { mutableStateOf(SegmentSort.BEST_TIME) }

    val sortedSegments = when (sort) {
        SegmentSort.BEST_TIME, SegmentSort.NEARBY ->
            segments.sortedWith(compareBy(nullsLast()) { it.bestTimeMs })
        SegmentSort.MOST_RUNS ->
            segments.sortedByDescending { it.runCount }
    }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Segments",
                onBack = onBack,
                trailingIcon = Icons.Rounded.Add,
                onTrailingAction = onAdd,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SpeedDimens.screenPadding),
        ) {
            // Search bar pill
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
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Search segments…",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Sort chips row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    SpeedSelectableChip(
                        selected = sort == SegmentSort.BEST_TIME,
                        onClick = { sort = SegmentSort.BEST_TIME },
                        label = "Best time",
                    )
                    SpeedSelectableChip(
                        selected = sort == SegmentSort.MOST_RUNS,
                        onClick = { sort = SegmentSort.MOST_RUNS },
                        label = "Most runs",
                    )
                    SpeedSelectableChip(
                        selected = sort == SegmentSort.NEARBY,
                        onClick = { sort = SegmentSort.NEARBY },
                        label = "Nearby",
                    )
                }
            }

            // Count label
            item {
                Text(
                    text = "${segments.size} SEGMENTS",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, start = 2.dp),
                )
            }

            if (sortedSegments.isEmpty()) {
                // Empty state
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = "No segments yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Tap + to create one from a ride",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Segment rows with dividers between them
                itemsIndexed(sortedSegments, key = { _, item -> item.segmentId }) { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    SegmentRow(
                        item = item,
                        units = units,
                        onClick = { onOpenSegment(item.segmentId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentRow(
    item: SegmentListItem,
    units: Units,
    onClick: () -> Unit,
) {
    val isHighlighted = item.isFavourite || item.isGoal
    val leadingContainerColor = if (isHighlighted)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    val leadingContentColor = if (isHighlighted)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val bestTimeColor = if (item.isFavourite)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface

    val delta = if (item.lastTimeMs != null && item.previousTimeMs != null)
        item.lastTimeMs - item.previousTimeMs
    else
        null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        // Leading timer chip
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SpeedDimens.listRowIconSize)
                .clip(CircleShape)
                .background(leadingContainerColor),
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = leadingContentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        // Middle column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight(500),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${UnitFormat.distance(item.distanceM, units)} · ${item.runCount} runs",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Trailing column
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (item.bestTimeMs != null) formatSegmentTime(item.bestTimeMs) else "—",
                fontSize = 16.sp,
                fontWeight = FontWeight(600),
                color = bestTimeColor,
            )
            // Trend row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                when {
                    delta != null && delta < 0 -> {
                        val absSecs = "%.1f".format(kotlin.math.abs(delta) / 1000.0)
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "${absSecs}s",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    delta != null && delta > 0 -> {
                        val absSecs = "%.1f".format(delta / 1000.0)
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "${absSecs}s",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "—",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Format a time in milliseconds as "m:ss.t" (e.g. 134_800 ms → "2:14.8"). */
private fun formatSegmentTime(ms: Long): String {
    val totalTenths = ms / 100
    val tenths = totalTenths % 10
    val totalSeconds = totalTenths / 10
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return "$minutes:${seconds.toString().padStart(2, '0')}.$tenths"
}

// ---- Previews ----

private val previewSegments = listOf(
    SegmentListItem(
        segmentId = "seg-1",
        name = "Col de Turini — North approach",
        distanceM = 8_400f,
        isFavourite = true,
        isGoal = false,
        runCount = 12,
        bestTimeMs = 134_800L,   // 2:14.8
        lastTimeMs = 136_200L,
        previousTimeMs = 139_400L, // faster: delta = -3200 ms → 3.2s faster
    ),
    SegmentListItem(
        segmentId = "seg-2",
        name = "Hairpin section",
        distanceM = 2_150f,
        isFavourite = false,
        isGoal = true,
        runCount = 5,
        bestTimeMs = 47_300L,   // 0:47.3
        lastTimeMs = 49_100L,
        previousTimeMs = 47_500L, // slower: delta = +1600 ms → 1.6s
    ),
    SegmentListItem(
        segmentId = "seg-3",
        name = "Sprint to summit",
        distanceM = 1_050f,
        isFavourite = false,
        isGoal = false,
        runCount = 3,
        bestTimeMs = 22_600L,   // 0:22.6
        lastTimeMs = null,
        previousTimeMs = null,  // no trend
    ),
)

@PreviewLightDark
@Composable
private fun SegmentListScreenPopulatedPreview() {
    RaceLoggerTheme {
        SegmentListScreen(
            segments = previewSegments,
            onBack = {},
            onAdd = {},
            onSearch = {},
            onOpenSegment = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SegmentListScreenEmptyPreview() {
    RaceLoggerTheme {
        SegmentListScreen(
            segments = emptyList(),
            onBack = {},
            onAdd = {},
            onSearch = {},
            onOpenSegment = {},
        )
    }
}
