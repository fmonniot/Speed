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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.Session
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens
import eu.monniot.speed.ui.theme.SpeedTextStyles
import eu.monniot.speed.util.FormatUtils
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.util.UnitFormat
import java.util.Locale

@Composable
fun SummaryScreen(
    session: Session?,
    isNewPb: Boolean,
    maxLateralG: Float?,     // TODO(E2): populated by sensor fusion
    maxLeanDeg: Float?,      // TODO(E1/E2): populated by IMU lean estimation
    distanceM: Float?,       // TODO(E2): accumulated from GPS
    avgSpeedMs: Float?,      // TODO(E2): computed from moving segments
    hardBrakeG: Float?,      // TODO(E2): most-negative longitudinal G
    movingPercent: Float?,   // TODO(E2): ratio of moving time to total, 0..100
    segmentPbCount: Int?,    // TODO(E5): segment PB detection
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRename: (String) -> Unit,
    onOpenTrace: () -> Unit,
    onOpenSegments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val units = LocalUnits.current
    var showRename by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Trip summary",
                onBack = onBack,
                trailingIcon = Icons.Rounded.Share,
                onTrailingAction = onShare,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (showRename) {
            var draft by remember { mutableStateOf(session.name.orEmpty()) }
            AlertDialog(
                onDismissRequest = { showRename = false },
                title = { Text("Rename ride") },
                text = {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Ride name") },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpeedDimens.screenPadding)
                .padding(bottom = SpeedDimens.screenPadding),
        ) {
            // User-given name, or a weekday-derived label. Tap to rename.
            val sessionName = sessionDisplayName(session.name, session.startTimeMs)
            Text(
                text = sessionName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { showRename = true },
            )

            // Meta line: date · start→end times · duration
            val timeInfo = formatSessionTime(session.startTimeMs, session.endTimeMs)
            val metaText = if (session.endTimeMs != null) {
                val durationSeconds = ((session.endTimeMs - session.startTimeMs) / 1000L).toInt()
                val duration = FormatUtils.formatDuration(durationSeconds)
                "${timeInfo.primary} · ${timeInfo.secondary} · $duration"
            } else {
                "${timeInfo.primary} · ${timeInfo.secondary}"
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metaText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Top-speed hero card
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(SpeedDimens.radiusHeroCard),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SpeedDimens.radiusHeroCard))
                    .clickable(onClick = onOpenTrace),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    // Header row: label + optional NEW PB badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "TOP SPEED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp,
                                letterSpacing = 0.08.em,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                        if (isNewPb) {
                            Surface(
                                shape = RoundedCornerShape(SpeedDimens.radiusChip),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Text(
                                        text = "NEW PB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight(600),
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }

                    // Numeral row: value + unit aligned to baseline
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = UnitFormat.speedValue(session.maxSpeedMs ?: 0f, units),
                            style = SpeedTextStyles.heroSummary,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = UnitFormat.speedUnit(units),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight(500),
                                fontSize = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }

                    // U2: explicit tap affordance — the whole card navigates to the Trace at the
                    // peak-speed moment (spec §4.3 #3), but had no visual cue. A trailing
                    // "View trace" hint + chevron_right signals it is interactive.
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "View trace",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            // 6-cell stat grid (3 rows × 2 columns) — DECORATIVE, no click
            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: Max G lat + Max lean
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCell(
                    label = "Max G lat",
                    // TODO(E2): maxLateralG is null until sensor fusion provides lateral acceleration
                    value = maxLateralG?.let { UnitFormat.lateralGValue(it) } ?: "—",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = "Max lean",
                    // TODO(E1/E2): maxLeanDeg is null until IMU lean estimation is implemented
                    value = maxLeanDeg?.let { UnitFormat.leanValue(it) } ?: "—",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Distance + Avg speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCell(
                    label = "Distance",
                    // TODO(E2): distanceM is null until GPS distance accumulation is implemented
                    value = distanceM?.let { UnitFormat.distance(it, units) } ?: "—",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = "Avg speed",
                    // TODO(E2): avgSpeedMs is null until moving-segment average is computed
                    value = avgSpeedMs?.let { UnitFormat.speed(it, units) } ?: "—",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Hard brake + Moving
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCell(
                    label = "Hard brake",
                    // TODO(E2): hardBrakeG is null until longitudinal G tracking is implemented
                    value = hardBrakeG?.let { String.format(Locale.US, "%+.2f g", it) } ?: "—",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatCell(
                    label = "Moving",
                    // TODO(E2): movingPercent is null until moving-time ratio is computed
                    value = movingPercent?.let { "${it.toInt()}%" } ?: "—",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            // Segment PB link row — CLICKABLE
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(SpeedDimens.radiusBorderedRow),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(SpeedDimens.radiusBorderedRow))
                    .clickable(onClick = onOpenSegments),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // TODO(E5): segmentPbCount is null until segment PB detection is implemented
                    Text(
                        text = "${segmentPbCount?.toString() ?: "—"} personal bests by segment",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight(500),
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * DECORATIVE stat cell — no onClick/clickable.
 * Used in the 6-cell stat grid on the Summary screen (§4.3).
 */
@Composable
private fun StatCell(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(SpeedDimens.radiusSmallStat),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = contentColor.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = SpeedTextStyles.cardStatSmall,
                color = contentColor,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SummaryScreenPreview() {
    RaceLoggerTheme {
        SummaryScreen(
            session = Session(
                sessionId = "s1",
                startTimeMs = 1716731520000L,
                endTimeMs = 1716734352000L,
                pointCount = 28000,
                maxSpeedMs = 52f,
                notes = "",
            ),
            isNewPb = true,
            maxLateralG = null,
            maxLeanDeg = null,
            distanceM = 54800f,
            avgSpeedMs = 19.4f,
            hardBrakeG = null,
            movingPercent = null,
            segmentPbCount = 3,
            onBack = {},
            onShare = {},
            onRename = {},
            onOpenTrace = {},
            onOpenSegments = {},
        )
    }
}
