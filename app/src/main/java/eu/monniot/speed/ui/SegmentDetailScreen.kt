package eu.monniot.speed.ui

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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.Segment
import eu.monniot.speed.data.SegmentAttempt
import eu.monniot.speed.ui.components.SectionLabel
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.ui.theme.SpeedTextStyles
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

// §4.8 Stats · Segment detail screen — personal-best card + attempt history.
// Stateless; orchestrator wires navigation and provides data.

// ── Private time formatter ────────────────────────────────────────────────────

/**
 * Formats an elapsed time in milliseconds as "m:ss.t"
 * (minutes, two-digit seconds, one-digit tenths).
 * Example: 75_300 ms → "1:15.3"
 */
private fun formatSegmentTime(ms: Long): String {
    val totalTenths = (ms / 100L)  // 1/10 s precision
    val tenths = totalTenths % 10
    val totalSeconds = totalTenths / 10
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

// ── Public screen ─────────────────────────────────────────────────────────────

@Composable
fun SegmentDetailScreen(
    segment: Segment?,                  // null while loading
    attempts: List<SegmentAttempt>,     // newest-first; may be empty
    onBack: () -> Unit,
    onRename: (newName: String) -> Unit,
    onSetGoal: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onOpenTrace: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    // Compute the personal best (smallest elapsedTimeMs).
    val bestAttempt: SegmentAttempt? = remember(attempts) {
        attempts.minByOrNull { it.elapsedTimeMs }
    }

    // Compute trend: scan chronologically to find when the current PB was first set,
    // and what the previous best was at that moment.
    data class PbTrend(val previousBestMs: Long, val deltaMs: Long)

    val pbTrend: PbTrend? = remember(attempts) {
        if (bestAttempt == null) return@remember null
        // Sort ascending by date to replay history
        val chronological = attempts.sortedBy { it.dateMs }
        var runningBest: Long? = null
        var prevBest: Long? = null
        for (a in chronological) {
            if (runningBest == null || a.elapsedTimeMs < runningBest) {
                // This attempt improved the running best
                if (runningBest != null) {
                    // There was a prior best — record it as prevBest at the moment we hit the final PB
                    prevBest = runningBest
                }
                runningBest = a.elapsedTimeMs
            }
        }
        // prevBest is the best time just before the current overall PB was set
        if (prevBest != null && bestAttempt.elapsedTimeMs < prevBest) {
            val delta = prevBest - bestAttempt.elapsedTimeMs
            PbTrend(previousBestMs = prevBest, deltaMs = delta)
        } else {
            null
        }
    }

    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    if (showRename) {
        var draft by remember { mutableStateOf(segment?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename segment") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Segment name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = { onRename(draft.trim()); showRename = false },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            Box {
                SpeedTopBar(
                    title = "Segment",
                    onBack = onBack,
                    trailingIcon = Icons.Rounded.MoreVert,
                    onTrailingAction = { menuExpanded = true },
                )
                // Anchor the DropdownMenu at the top-right of the Box (near the top bar icon)
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuExpanded = false; showRename = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Set as goal") },
                        onClick = { menuExpanded = false; onSetGoal() },
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { menuExpanded = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpeedDimens.screenPadding)
                .padding(bottom = SpeedDimens.screenPadding),
        ) {
            Spacer(modifier = Modifier.height(0.dp))  // top breathing room after topBar

            // ── Title + meta ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = segment?.name ?: "—",
                    fontSize = 24.sp,
                    fontWeight = FontWeight(500),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val distanceStr = segment?.let { UnitFormat.distance(it.distanceM, units) } ?: "—"
                Text(
                    text = "$distanceStr · ${attempts.size} attempt${if (attempts.size != 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Personal-best card ────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(SpeedDimens.radiusHeroCard),
                onClick = { bestAttempt?.let { onOpenTrace(it.sessionId) } },
                enabled = bestAttempt != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header row: "PERSONAL BEST" label + optional trend
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "PERSONAL BEST",
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600),
                            letterSpacing = 0.06.em,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        // Trend: only shown when there was a prior PB and we improved on it
                        if (pbTrend != null) {
                            val deltaSeconds = pbTrend.deltaMs / 1000.0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "%.1fs faster".format(deltaSeconds),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight(600),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    // Time numeral
                    Text(
                        text = if (bestAttempt != null) formatSegmentTime(bestAttempt.elapsedTimeMs) else "—",
                        style = SpeedTextStyles.heroSegmentTime,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    // Three inline stats from the PB run
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        PbInlineStat(
                            value = bestAttempt?.let { UnitFormat.speed(it.maxSpeedMs, units) } ?: "—",
                            caption = "v.max",
                        )
                        PbInlineStat(
                            value = bestAttempt?.let { UnitFormat.lateralG(it.maxLateralG) } ?: "—",
                            caption = "g lat",
                        )
                        PbInlineStat(
                            value = bestAttempt?.let { UnitFormat.lean(it.maxLeanDeg) } ?: "—",
                            caption = "lean",
                        )
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (attempts.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = "No attempts yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Ride through this segment to set a time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // ── History list ──────────────────────────────────────────
                Column {
                    SectionLabel(
                        text = "History",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    val bestTimeMs: Long = bestAttempt!!.elapsedTimeMs

                    attempts.forEachIndexed { index, attempt ->
                        val isBest = attempt.elapsedTimeMs == bestTimeMs

                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTrace(attempt.sessionId) }
                                .padding(vertical = 10.dp),
                        ) {
                            // Date column (~60 dp)
                            Text(
                                text = remember(attempt.dateMs) { dateFormat.format(attempt.dateMs) },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(60.dp),
                            )

                            // Time
                            Text(
                                text = formatSegmentTime(attempt.elapsedTimeMs),
                                fontSize = 16.sp,
                                fontWeight = FontWeight(600),
                                color = if (isBest) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Relative bar — fills remaining width
                            val fillFraction = bestTimeMs.toFloat() / attempt.elapsedTimeMs.toFloat()
                            val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            val fillColor = if (isBest) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.outline

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp),
                            ) {
                                // Track
                                Surface(
                                    color = trackColor,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    content = {},
                                )
                                // Fill
                                Surface(
                                    color = fillColor,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = fillFraction.coerceIn(0f, 1f))
                                        .fillMaxSize(),
                                    content = {},
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Delta vs best
                            val deltaText = if (isBest) {
                                "PB"
                            } else {
                                val deltaMs = attempt.elapsedTimeMs - bestTimeMs
                                val deltaSec = deltaMs / 1000.0
                                "+%.1fs".format(deltaSec)
                            }
                            Text(
                                text = deltaText,
                                fontSize = 13.sp,
                                color = if (isBest) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Private: inline PB stat (value + caption stacked) ────────────────────────

@Composable
private fun PbInlineStat(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight(600),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = caption,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewSegment = Segment(
    segmentId = "seg-col-1",
    name = "Col de la Croix de Fer",
    distanceM = 12_400f,
    pathPolyline = "45.22,6.19;45.23,6.20",
    isFavourite = true,
    isGoal = false,
    createdAtMs = 1_700_000_000_000L,
)

// Four attempts: newest-first as the screen expects.
// attempt1 is the slowest, attempt4 is the PB.
private val previewAttempts = listOf(
    SegmentAttempt(
        id = 4,
        segmentId = "seg-col-1",
        sessionId = "sess-d",
        elapsedTimeMs = 75_300L,   // 1:15.3 — PB
        dateMs = System.currentTimeMillis() - 2L * 24 * 3600 * 1000,
        maxSpeedMs = 38.9f,
        maxLateralG = 0.84f,
        maxLeanDeg = 42f,
    ),
    SegmentAttempt(
        id = 3,
        segmentId = "seg-col-1",
        sessionId = "sess-c",
        elapsedTimeMs = 77_800L,   // 1:17.8
        dateMs = System.currentTimeMillis() - 10L * 24 * 3600 * 1000,
        maxSpeedMs = 36.1f,
        maxLateralG = 0.78f,
        maxLeanDeg = 39f,
    ),
    SegmentAttempt(
        id = 2,
        segmentId = "seg-col-1",
        sessionId = "sess-b",
        elapsedTimeMs = 81_000L,   // 1:21.0
        dateMs = System.currentTimeMillis() - 20L * 24 * 3600 * 1000,
        maxSpeedMs = 34.5f,
        maxLateralG = 0.71f,
        maxLeanDeg = 36f,
    ),
    SegmentAttempt(
        id = 1,
        segmentId = "seg-col-1",
        sessionId = "sess-a",
        elapsedTimeMs = 88_500L,   // 1:28.5
        dateMs = System.currentTimeMillis() - 35L * 24 * 3600 * 1000,
        maxSpeedMs = 31.2f,
        maxLateralG = 0.62f,
        maxLeanDeg = 31f,
    ),
)

@PreviewLightDark
@Composable
private fun SegmentDetailScreenPopulatedPreview() {
    RaceLoggerTheme {
        SegmentDetailScreen(
            segment = previewSegment,
            attempts = previewAttempts,
            onBack = {},
            onRename = {},
            onSetGoal = {},
            onDelete = {},
            onShare = {},
            onOpenTrace = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SegmentDetailScreenEmptyPreview() {
    RaceLoggerTheme {
        SegmentDetailScreen(
            segment = previewSegment,
            attempts = emptyList(),
            onBack = {},
            onRename = {},
            onSetGoal = {},
            onDelete = {},
            onShare = {},
            onOpenTrace = {},
        )
    }
}
