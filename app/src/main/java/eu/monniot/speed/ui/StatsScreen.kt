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
import androidx.compose.material.icons.filled.Moving
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.ui.components.SpeedSelectableChip
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.ui.theme.SpeedTextStyles
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ── Range enum ──────────────────────────────────────────────────────────────

enum class StatsRange { THIS_YEAR, NINETY_DAYS, ALL_TIME }

// ── Public screen ────────────────────────────────────────────────────────────

@Composable
fun StatsScreen(
    sessions: List<SessionSummary>,  // all sessions, newest-first
    segmentCount: Int,                // number of tracked segments (E4)
    onDateRange: () -> Unit,          // opens a date-range picker (no-op for now)
    onOpenSummary: (String) -> Unit,  // open the ride holding a record
    onOpenTrips: () -> Unit,          // month-bar tap → that month's trips (month filter is TODO)
    onOpenSegments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current
    var range by remember { mutableStateOf(StatsRange.THIS_YEAR) }

    // Compute scope boundaries once per composition
    val now = System.currentTimeMillis()
    val currentYear = remember {
        Calendar.getInstance().get(Calendar.YEAR)
    }
    val yearStartMs = remember(currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val ninetyDaysCutoffMs = now - 90L * 24 * 3600 * 1000
    val untilMs = now + 1

    // Scope window [since, until) for E3 aggregates.
    val sinceMs = when (range) {
        StatsRange.THIS_YEAR -> yearStartMs
        StatsRange.NINETY_DAYS -> ninetyDaysCutoffMs
        StatsRange.ALL_TIME -> 0L
    }
    val stats = remember(sessions, range) {
        eu.monniot.speed.domain.RideAggregates.rangeStats(sessions, sinceMs, untilMs)
    }
    // Trend = scoped distance vs the previous comparable period (null for All time / no baseline).
    val trendPercent = remember(sessions, range) {
        when (range) {
            StatsRange.THIS_YEAR -> {
                val prevYearStart = Calendar.getInstance().apply {
                    timeInMillis = yearStartMs; add(Calendar.YEAR, -1)
                }.timeInMillis
                eu.monniot.speed.domain.RideAggregates.trendPercent(
                    stats.totalDistanceM,
                    eu.monniot.speed.domain.RideAggregates.distanceInRange(sessions, prevYearStart, yearStartMs),
                )
            }
            StatsRange.NINETY_DAYS -> eu.monniot.speed.domain.RideAggregates.trendPercent(
                stats.totalDistanceM,
                eu.monniot.speed.domain.RideAggregates.distanceInRange(sessions, now - 180L * 24 * 3600 * 1000, ninetyDaysCutoffMs),
            )
            StatsRange.ALL_TIME -> null
        }
    }
    // Trailing-6-month buckets for the bar chart.
    val monthBars = remember(sessions) {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("MMM", Locale.getDefault())
        val buckets = (5 downTo 0).map { monthsBack ->
            val start = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -monthsBack)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            eu.monniot.speed.domain.MonthBucket(
                startMs = start.timeInMillis,
                endMsExclusive = end.timeInMillis,
                label = fmt.format(start.time).uppercase(Locale.getDefault()),
                isCurrent = monthsBack == 0,
            )
        }
        eu.monniot.speed.domain.RideAggregates.monthlyDistance(sessions, buckets)
    }

    val scopeLabel = when (range) {
        StatsRange.THIS_YEAR -> currentYear.toString()
        StatsRange.NINETY_DAYS -> "90 DAYS"
        StatsRange.ALL_TIME -> "ALL TIME"
    }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Statistics",
                trailingIcon = Icons.Rounded.DateRange,
                onTrailingAction = onDateRange,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpeedDimens.screenPadding),
        ) {

            // ── Range chips ──────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                SpeedSelectableChip(
                    selected = range == StatsRange.THIS_YEAR,
                    onClick = { range = StatsRange.THIS_YEAR },
                    label = "This year",
                )
                SpeedSelectableChip(
                    selected = range == StatsRange.NINETY_DAYS,
                    onClick = { range = StatsRange.NINETY_DAYS },
                    label = "90 days",
                )
                SpeedSelectableChip(
                    selected = range == StatsRange.ALL_TIME,
                    onClick = { range = StatsRange.ALL_TIME },
                    label = "All time",
                )
            }

            // ── Distance hero card ───────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(SpeedDimens.radiusHeroCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Header row: label + trend pill
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "DISTANCE · $scopeLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600),
                            letterSpacing = 0.08.em,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        // Trend % vs the previous comparable period (hidden when no baseline).
                        if (trendPercent != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = (if (trendPercent >= 0) "+" else "") + "$trendPercent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight(600),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }

                    // Numeral row: scoped total distance + unit
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = UnitFormat.distanceValue(stats.totalDistanceM, units),
                            style = SpeedTextStyles.heroStats,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = UnitFormat.distanceUnit(units),
                            fontSize = 16.sp,
                            fontWeight = FontWeight(500),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    // 6-month bar chart (real per-month distance heights).
                    MonthBars(
                        bars = monthBars,
                        onOpenTrips = onOpenTrips,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                    )
                }
            }

            // ── Records grid 2×2 ────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // 1. Top speed
                    RecordCard(
                        label = "Top speed",
                        value = if (stats.topSpeed.sessionId != null) UnitFormat.speedValue(stats.topSpeed.value, units) else "—",
                        unit = UnitFormat.speedUnit(units),
                        icon = Icons.Rounded.Speed,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = stats.topSpeed.sessionId?.let { id -> { onOpenSummary(id) } },
                        modifier = Modifier.weight(1f),
                    )
                    // 2. Max lean
                    RecordCard(
                        label = "Max lean",
                        value = if (stats.maxLean.sessionId != null) UnitFormat.leanValue(stats.maxLean.value) else "—",
                        unit = "",
                        icon = Icons.Filled.Moving,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = stats.maxLean.sessionId?.let { id -> { onOpenSummary(id) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // 3. Max lateral G
                    RecordCard(
                        label = "Max g lat",
                        value = if (stats.maxLateralG.sessionId != null) UnitFormat.lateralGValue(stats.maxLateralG.value) else "—",
                        unit = "g",
                        icon = Icons.Rounded.Speed,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = stats.maxLateralG.sessionId?.let { id -> { onOpenSummary(id) } },
                        modifier = Modifier.weight(1f),
                    )
                    // 4. Longest ride
                    RecordCard(
                        label = "Longest",
                        value = if (stats.longestRide.sessionId != null) UnitFormat.distanceValue(stats.longestRide.value, units) else "—",
                        unit = UnitFormat.distanceUnit(units),
                        icon = Icons.Filled.Route,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = stats.longestRide.sessionId?.let { id -> { onOpenSummary(id) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Segments link row ────────────────────────────────────────
            Surface(
                onClick = onOpenSegments,
                shape = RoundedCornerShape(SpeedDimens.radiusBorderedRow),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "$segmentCount tracked segments",
                            fontSize = 15.sp,
                            fontWeight = FontWeight(500),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Bottom breathing room above nav bar
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Private: 6-month bar chart ───────────────────────────────────────────────

@Composable
private fun MonthBars(
    bars: List<eu.monniot.speed.domain.MonthBar>,
    onOpenTrips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxDistance = bars.maxOfOrNull { it.distanceM } ?: 0f
    val maxBarHeight = 48.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(64.dp),
    ) {
        bars.forEach { bar ->
            val isCurrentMonth = bar.isCurrent
            val barColor = if (isCurrentMonth) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
            }
            val labelWeight = if (isCurrentMonth) FontWeight(700) else FontWeight(500)
            val labelAlpha = if (isCurrentMonth) 0.95f else 0.6f
            // Height ∝ distance, with a 4 dp minimum so empty months still show a baseline.
            val frac = if (maxDistance > 0f) bar.distanceM / maxDistance else 0f
            val barHeight = (maxBarHeight * frac).coerceAtLeast(4.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { onOpenTrips() },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight),
                ) {
                    Surface(
                        color = barColor,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxSize(),
                        content = {},
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bar.label,
                    fontSize = 10.sp,
                    fontWeight = labelWeight,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = labelAlpha),
                )
            }
        }
    }
}

// ── Private: record card ─────────────────────────────────────────────────────

@Composable
private fun RecordCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(SpeedDimens.radiusMediumTonal),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            // Top row: label + icon
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
            }
            // Value row: numeral + unit (Bottom-aligned)
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = value,
                    style = SpeedTextStyles.cardStatMedium,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val previewStatsSessions = listOf(
    SessionSummary(
        sessionId = "s1",
        startTimeMs = System.currentTimeMillis() - 2L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 2L * 24 * 3600 * 1000 + 3_600_000L,
        pointCount = 1400,
        maxSpeedMs = 52.8f,  // ~190 km/h
    ),
    SessionSummary(
        sessionId = "s2",
        startTimeMs = System.currentTimeMillis() - 10L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 10L * 24 * 3600 * 1000 + 5_400_000L,
        pointCount = 2200,
        maxSpeedMs = 44.4f,  // ~160 km/h
    ),
    SessionSummary(
        sessionId = "s3",
        startTimeMs = System.currentTimeMillis() - 35L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 35L * 24 * 3600 * 1000 + 4_200_000L,
        pointCount = 1800,
        maxSpeedMs = 38.9f,  // ~140 km/h
    ),
    SessionSummary(
        sessionId = "s4",
        startTimeMs = System.currentTimeMillis() - 60L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 60L * 24 * 3600 * 1000 + 7_200_000L,
        pointCount = 3000,
        maxSpeedMs = 47.2f,  // ~170 km/h
    ),
    SessionSummary(
        sessionId = "s5",
        startTimeMs = System.currentTimeMillis() - 120L * 24 * 3600 * 1000,
        endTimeMs = System.currentTimeMillis() - 120L * 24 * 3600 * 1000 + 2_700_000L,
        pointCount = 900,
        maxSpeedMs = 30.6f,  // ~110 km/h
    ),
)

@PreviewLightDark
@Composable
private fun StatsScreenPreview() {
    RaceLoggerTheme {
        StatsScreen(
            sessions = previewStatsSessions,
            segmentCount = 14,
            onDateRange = {},
            onOpenSummary = {},
            onOpenTrips = {},
            onOpenSegments = {},
        )
    }
}
