package eu.monniot.speed.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.ui.components.MapTrackCard
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures

// §4.5 Trace / detail screen — shows a ride's map placeholder and two drag-scrub charts.
// No bottom nav; top bar has back + download.
@Composable
fun TraceScreen(
    points: List<DataPoint>,    // ordered by time asc; may be empty
    onBack: () -> Unit,
    onDownload: () -> Unit,     // exports just this ride (CSV today)
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current

    // Shared playhead: both charts drive and read this same index.
    var playheadIndex by remember { mutableStateOf<Int?>(null) }

    // Derived series — computed once and shared between charts + header readouts.
    val speedSeries: List<Float> = remember(points) {
        points.map { it.derivedSpeedMs ?: it.gpsSpeedMs ?: 0f }
    }
    // E1 landed: plot the real signed lateral G (+ = rider's right). Points before E1 (older
    // recordings) have null lateralGz and contribute 0.
    val gSeries: List<Float> = remember(points) {
        points.map { it.lateralGz ?: 0f }
    }
    // Top speed for the map chip label.
    val topSpeedMs: Float = remember(points) { speedSeries.maxOrNull() ?: 0f }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trace",
                onBack = onBack,
                trailingIcon = Icons.Rounded.Download,
                onTrailingAction = onDownload,
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
                .padding(bottom = SpeedDimens.screenPadding),
        ) {
            // ---- Map card (F1) ----
            // TODO(F2): tap opens a full-screen interactive map.
            Spacer(modifier = Modifier.height(12.dp))
            MapTrackCard(
                points = points,
                topSpeedLabel = UnitFormat.speed(topSpeedMs, units),
                playheadIndex = playheadIndex,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- Empty state (skip charts, keep map placeholder above) ----
            if (points.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No trace data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Scaffold
            }

            // ---- Speed chart card ----
            val speedMin = speedSeries.minOrNull() ?: 0f
            val speedMax = speedSeries.maxOrNull() ?: 0f

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(SpeedDimens.radiusMediumTonal),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    // Header row: label + scrubbed value or min–max range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Speed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val idx = playheadIndex
                        if (idx != null && idx in speedSeries.indices) {
                            Text(
                                text = UnitFormat.speed(speedSeries[idx], units),
                                fontSize = 12.sp,
                                fontWeight = FontWeight(600),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Text(
                                text = "${UnitFormat.speedValue(speedMin, units)} – " +
                                    "${UnitFormat.speedValue(speedMax, units)} " +
                                    UnitFormat.speedUnit(units),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TraceChart(
                        values = speedSeries,
                        lineColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primaryContainer,
                        drawZeroBaseline = false,
                        heightDp = 64.dp,
                        playheadIndex = playheadIndex,
                        onScrub = { playheadIndex = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ---- Lateral-G chart card ----
            val gMin = gSeries.minOrNull() ?: 0f
            val gMax = gSeries.maxOrNull() ?: 0f

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(SpeedDimens.radiusMediumTonal),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "G lateral",
                            fontSize = 14.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val idx = playheadIndex
                        if (idx != null && idx in gSeries.indices) {
                            Text(
                                text = String.format(Locale.US, "%+.2f g", gSeries[idx]),
                                fontSize = 12.sp,
                                fontWeight = FontWeight(600),
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        } else {
                            Text(
                                text = String.format(Locale.US, "%+.2f / %+.2f", gMin, gMax),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TraceChart(
                        values = gSeries,
                        lineColor = MaterialTheme.colorScheme.tertiary,
                        fillColor = null,
                        drawZeroBaseline = true,
                        heightDp = 52.dp,
                        playheadIndex = playheadIndex,
                        onScrub = { playheadIndex = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ---- Private chart composable ----

@Composable
private fun TraceChart(
    values: List<Float>,
    lineColor: Color,
    fillColor: Color?,          // null → no filled area (used for G chart)
    drawZeroBaseline: Boolean,  // true for the G chart
    heightDp: Dp,
    playheadIndex: Int?,
    onScrub: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Capture colors from MaterialTheme before entering the Canvas lambda.
    val playheadLineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .height(heightDp)
            .pointerInput(values.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val n = values.size
                        if (n > 0) {
                            val idx = ((offset.x / size.width.toFloat()) * (n - 1))
                                .roundToInt()
                                .coerceIn(0, n - 1)
                            onScrub(idx)
                        }
                    },
                    onDrag = { change, _ ->
                        val n = values.size
                        if (n > 0) {
                            val idx = ((change.position.x / size.width.toFloat()) * (n - 1))
                                .roundToInt()
                                .coerceIn(0, n - 1)
                            onScrub(idx)
                        }
                    },
                    onDragEnd = {
                        // Keep the last scrubbed index so headers stay visible after the drag.
                    },
                )
            },
    ) {
        val n = values.size
        val w = size.width
        val h = size.height

        // Guard: flat series or too few points — draw a simple baseline.
        if (n < 2) {
            drawLine(
                color = baselineColor,
                start = androidx.compose.ui.geometry.Offset(0f, h / 2f),
                end = androidx.compose.ui.geometry.Offset(w, h / 2f),
                strokeWidth = 1.dp.toPx(),
            )
            return@Canvas
        }

        val minVal = values.minOrNull() ?: 0f
        val maxVal = values.maxOrNull() ?: 0f
        val range = maxVal - minVal

        // Normalize y: top of canvas = maxVal, bottom = minVal.
        // If range == 0 every value maps to the midpoint.
        fun yFor(v: Float): Float =
            if (range == 0f) h / 2f
            else h - ((v - minVal) / range) * h

        // Optional zero-baseline (G chart).
        if (drawZeroBaseline) {
            val zeroY = yFor(0f)
            drawLine(
                color = baselineColor,
                start = androidx.compose.ui.geometry.Offset(0f, zeroY),
                end = androidx.compose.ui.geometry.Offset(w, zeroY),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Build the line path.
        val linePath = Path().apply {
            val xStep = w / (n - 1).toFloat()
            moveTo(0f, yFor(values[0]))
            for (i in 1 until n) {
                lineTo(i * xStep, yFor(values[i]))
            }
        }

        // Filled area — close the path to the bottom edge.
        if (fillColor != null) {
            val fillPath = Path().apply {
                val xStep = w / (n - 1).toFloat()
                moveTo(0f, h)
                lineTo(0f, yFor(values[0]))
                for (i in 1 until n) {
                    lineTo(i * xStep, yFor(values[i]))
                }
                lineTo(w, h)
                close()
            }
            drawPath(
                path = fillPath,
                color = fillColor,
                alpha = 0.4f,
            )
        }

        // Data line.
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )

        // Playhead — vertical line + accent dot.
        if (playheadIndex != null && playheadIndex in values.indices) {
            val xStep = w / (n - 1).toFloat()
            val px = playheadIndex * xStep
            val py = yFor(values[playheadIndex])

            drawLine(
                color = playheadLineColor,
                start = androidx.compose.ui.geometry.Offset(px, 0f),
                end = androidx.compose.ui.geometry.Offset(px, h),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(px, py),
            )
        }
    }
}

// ---- Previews ----

private fun buildPreviewPoints(count: Int = 60): List<DataPoint> {
    // Ramp up to ~33 m/s (~120 km/h) then back down; varied longitudinal accel.
    return (0 until count).map { i ->
        val progress = i / (count - 1).toFloat()
        // Speed curve: sine-like ramp up then down
        val speedMs = 33f * kotlin.math.sin(progress * Math.PI.toFloat()).coerceIn(0f, 33f)
        // Accel: positive on the way up, negative on the way down
        val accelMs2 = if (progress < 0.5f) 2.5f * (1f - progress * 2f) else -3f * (progress - 0.5f) * 2f
        DataPoint(
            id = i.toLong(),
            sessionId = "preview",
            elapsedRealtimeNs = (i * 1e8).toLong(),
            wallClockMs = 1716731520000L + i * 100L,
            latitude = null,
            longitude = null,
            altitude = null,
            gpsSpeedMs = speedMs,
            gpsAccuracyM = null,
            satellitesUsed = null,
            satellitesVisible = null,
            accelX = 0f,
            accelY = 0f,
            accelZ = 0f,
            accelMagnitude = 0f,
            derivedSpeedMs = speedMs,
            derivedAccelMs2 = accelMs2,
        )
    }
}

@PreviewLightDark
@Composable
private fun TraceScreenPreview() {
    RaceLoggerTheme {
        TraceScreen(
            points = buildPreviewPoints(),
            onBack = {},
            onDownload = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun TraceScreenEmptyPreview() {
    RaceLoggerTheme {
        TraceScreen(
            points = emptyList(),
            onBack = {},
            onDownload = {},
        )
    }
}
