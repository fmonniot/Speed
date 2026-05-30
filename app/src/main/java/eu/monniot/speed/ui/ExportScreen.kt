package eu.monniot.speed.ui

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.TwoWheeler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.ui.components.SectionLabel
import eu.monniot.speed.ui.components.SpeedFullWidthButton
import eu.monniot.speed.ui.components.SpeedSwitch
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens

// ---- Public API ----

enum class ExportFormat { CSV, GPX, FIT }

// Scope pickers (D10). Trips = which rides; Date = time window. Applied together (AND).
enum class TripsScope(val label: String) { ALL("All trips"), THIS_WEEK("This week") }
enum class DateScope(val label: String) { ALL_TIME("All time"), THIS_YEAR("This year"), LAST_90("Last 90 days") }

@Composable
fun ExportScreen(
    sessions: List<eu.monniot.speed.data.SessionSummary>,  // all sessions (newest-first)
    isExporting: Boolean,                                   // true while an export runs
    onBack: () -> Unit,
    onExport: (sessionIds: List<String>, format: ExportFormat, includeGps: Boolean, includeImu: Boolean, includeLean: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ---- Selection state ----
    var format by remember { mutableStateOf(ExportFormat.CSV) }
    var includeGps by remember { mutableStateOf(true) }
    var includeImu by remember { mutableStateOf(true) }
    var includeLean by remember { mutableStateOf(false) }
    var tripsScope by remember { mutableStateOf(TripsScope.ALL) }
    var dateScope by remember { mutableStateOf(DateScope.ALL_TIME) }
    var showTripsPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // ---- Scope filtering (D10/F5) ----
    val now = System.currentTimeMillis()
    val dateCutoffMs = remember(dateScope) {
        when (dateScope) {
            DateScope.ALL_TIME -> 0L
            DateScope.THIS_YEAR -> {
                java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            DateScope.LAST_90 -> now - 90L * 24 * 3600 * 1000
        }
    }
    val scoped = sessions
        .filter { if (tripsScope == TripsScope.THIS_WEEK) isThisWeek(it.startTimeMs) else true }
        .filter { it.startTimeMs >= dateCutoffMs }
    val tripCount = scoped.size

    // ---- Live export size estimate over the SCOPED set ----
    //   Per point ≈ 120 B at full CSV resolution; streams: GPS ~25%, IMU ~60%, Lean ~15%.
    //   GPX/FIT scale to ~0.7× of CSV. Floor keeps the label from reading "0 B".
    val scopedPoints = scoped.sumOf { it.pointCount.toLong() }
    val fullScopedBytes = scopedPoints * 120L
    val streamWeightSum = (if (includeGps) 0.25f else 0f) +
            (if (includeImu) 0.60f else 0f) +
            (if (includeLean) 0.15f else 0f)
    val floor = fullScopedBytes * 0.01f
    val formatScale = if (format == ExportFormat.CSV) 1.0f else 0.7f
    val estimatedBytes = maxOf(floor, fullScopedBytes * streamWeightSum * formatScale).toLong()

    val exportLabel = if (isExporting) "Exporting…" else "Export $tripCount trips · ${formatBytes(estimatedBytes)}"

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Export data",
                onBack = onBack,
                trailingIcon = Icons.Rounded.HelpOutline,
                onTrailingAction = { showHelp = true },
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ---- Summary line ----
            Text(
                text = "$tripCount trips · ${formatBytes(fullScopedBytes)} at full 100 ms resolution",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---- FORMAT section ----
            SectionLabel("Format")

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FormatCard(
                    label = "CSV",
                    icon = Icons.Rounded.Description,
                    selected = format == ExportFormat.CSV,
                    onClick = { format = ExportFormat.CSV },
                    modifier = Modifier.weight(1f),
                )
                FormatCard(
                    label = "GPX",
                    icon = Icons.Rounded.Map,
                    selected = format == ExportFormat.GPX,
                    onClick = { format = ExportFormat.GPX },
                    modifier = Modifier.weight(1f),
                )
                FormatCard(
                    label = "FIT",
                    icon = Icons.Rounded.Memory,
                    selected = format == ExportFormat.FIT,
                    onClick = { format = ExportFormat.FIT },
                    modifier = Modifier.weight(1f),
                )
            }

            // ---- SCOPE section ----
            SectionLabel("Scope")

            ExportGroupContainer {
                ExportScopeRow(
                    icon = Icons.Rounded.Route,
                    label = "Trips",
                    trailingValue = if (tripsScope == TripsScope.ALL) "All $tripCount" else tripsScope.label,
                    onClick = { showTripsPicker = true },
                )
                ExportGroupDivider()
                ExportScopeRow(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "Date range",
                    trailingValue = dateScope.label,
                    onClick = { showDatePicker = true },
                )
            }

            // ---- INCLUDE section ----
            SectionLabel("Include")

            ExportGroupContainer {
                ExportSwitchRow(
                    icon = Icons.Rounded.SatelliteAlt,
                    label = "GPS track",
                    checked = includeGps,
                    onCheckedChange = { includeGps = it },
                )
                ExportGroupDivider()
                ExportSwitchRow(
                    icon = Icons.Rounded.Sensors,
                    label = "IMU · accel & gyro",
                    checked = includeImu,
                    onCheckedChange = { includeImu = it },
                )
                ExportGroupDivider()
                ExportSwitchRow(
                    icon = Icons.Rounded.TwoWheeler,
                    label = "Lean angle",
                    checked = includeLean,
                    onCheckedChange = { includeLean = it },
                )
            }

            // ---- Export button (shows progress + disables while exporting) ----
            Box(modifier = Modifier.fillMaxWidth()) {
                SpeedFullWidthButton(
                    onClick = {
                        if (!isExporting && tripCount > 0) {
                            onExport(scoped.map { it.sessionId }, format, includeGps, includeImu, includeLean)
                        }
                    },
                    label = exportLabel,
                    icon = Icons.Rounded.Download,
                )
                if (isExporting) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp)
                            .size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ---- Scope picker dialogs ----
    if (showTripsPicker) {
        ScopePickerDialog(
            title = "Trips",
            options = TripsScope.entries.map { it to it.label },
            selected = tripsScope,
            onSelect = { tripsScope = it },
            onDismiss = { showTripsPicker = false },
        )
    }
    if (showDatePicker) {
        ScopePickerDialog(
            title = "Date range",
            options = DateScope.entries.map { it to it.label },
            selected = dateScope,
            onSelect = { dateScope = it },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showHelp) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Export formats") },
            text = {
                Text(
                    "CSV — every captured sample as columns; best for spreadsheets and analysis.\n\n" +
                        "GPX — the GPS track (lat/lon/elevation/time); opens in any map or GPX viewer.\n\n" +
                        "FIT — a compact binary track for Garmin and other fitness tools.\n\n" +
                        "SCOPE limits which trips and dates are included; INCLUDE toggles which data " +
                        "streams go in the file. The button shows the resulting count and size.",
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showHelp = false }) { Text("Got it") }
            },
        )
    }
}

@Composable
private fun <T> ScopePickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 12.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value); onDismiss() },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
    )
}

// ---- Private helpers ----

/**
 * Formats a byte count into a human-readable string (B / KB / MB / GB).
 * Uses base-10 (SI) units for consistency with storage-sizing conventions.
 */
private fun formatBytes(bytes: Long): String = when {
    bytes < 1_000L -> "$bytes B"
    bytes < 1_000_000L -> "%.1f KB".format(bytes / 1_000.0)
    bytes < 1_000_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.1f GB".format(bytes / 1_000_000_000.0)
}

@Composable
private fun FormatCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) 2.dp else 1.dp
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick),
    ) {
        Surface(
            color = bgColor,
            shape = shape,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(600),
                    color = contentColor,
                )
            }
        }

        // Check circle badge at top-end corner, visible only when selected
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun ExportGroupContainer(
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ExportGroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 52.dp),
    )
}

@Composable
private fun ExportScopeRow(
    icon: ImageVector,
    label: String,
    trailingValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trailingValue,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ExportSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight(500),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        SpeedSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ---- Previews ----

@PreviewLightDark
@Composable
private fun ExportScreenPreview() {
    RaceLoggerTheme {
        ExportScreen(
            sessions = listOf(
                eu.monniot.speed.data.SessionSummary("a", System.currentTimeMillis(), null, 12000, 52f),
                eu.monniot.speed.data.SessionSummary("b", System.currentTimeMillis() - 5L * 24 * 3600 * 1000, null, 9000, 44f),
            ),
            isExporting = false,
            onBack = {},
            onExport = { _, _, _, _, _ -> },
        )
    }
}
