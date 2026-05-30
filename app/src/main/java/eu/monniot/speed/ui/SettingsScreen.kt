package eu.monniot.speed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.Units
import eu.monniot.speed.ui.components.SectionLabel
import eu.monniot.speed.ui.components.SpeedSwitch
import eu.monniot.speed.ui.components.SpeedTopBar
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens

// ---- Public screen composable ----

@Composable
fun SettingsScreen(
    gpsRateHz: Int,
    imuRateHz: Int,
    autoPause: Boolean,
    units: Units,
    darkTheme: Boolean,
    tripCount: Int,
    storageSummary: String,
    onSetGpsRate: (Int) -> Unit,
    onSetImuRate: (Int) -> Unit,
    onSetAutoPause: (Boolean) -> Unit,
    onSetUnits: (Units) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onHelp: () -> Unit,
    onExportAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dialog state
    var showGpsPicker by remember { mutableStateOf(false) }
    var showImuPicker by remember { mutableStateOf(false) }
    var showUnitsPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SpeedTopBar(
                title = "Settings",
                trailingIcon = Icons.Rounded.HelpOutline,
                onTrailingAction = onHelp,
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

            // ---- SAMPLING section ----
            SectionLabel("Sampling")

            SettingsGroupContainer {
                SettingRow(
                    icon = Icons.Rounded.GpsFixed,
                    label = "GPS rate",
                    trailingValue = "$gpsRateHz Hz",
                    showChevron = true,
                    onClick = { showGpsPicker = true },
                )
                SettingsGroupDivider()
                SettingRow(
                    icon = Icons.Rounded.Sensors,
                    label = "IMU rate",
                    trailingValue = "$imuRateHz Hz",
                    showChevron = true,
                    onClick = { showImuPicker = true },
                )
                SettingsGroupDivider()
                SettingRow(
                    icon = Icons.Rounded.PauseCircle,
                    label = "Auto-pause",
                    showChevron = false,
                    trailing = {
                        SpeedSwitch(
                            checked = autoPause,
                            onCheckedChange = onSetAutoPause,
                        )
                    },
                )
            }

            // ---- DISPLAY section ----
            SectionLabel("Display")

            SettingsGroupContainer {
                SettingRow(
                    icon = Icons.Rounded.Straighten,
                    label = "Units",
                    trailingValue = if (units == Units.IMPERIAL) "Imperial" else "Metric",
                    showChevron = true,
                    onClick = { showUnitsPicker = true },
                )
                SettingsGroupDivider()
                SettingRow(
                    icon = Icons.Rounded.DarkMode,
                    label = "Dark theme",
                    showChevron = false,
                    trailing = {
                        SpeedSwitch(
                            checked = darkTheme,
                            onCheckedChange = onSetDarkTheme,
                        )
                    },
                )
            }

            // ---- Raw data export card ----
            RawDataExportCard(
                storageSummary = storageSummary,
                onExportAll = onExportAll,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ---- Pickers ----

    if (showGpsPicker) {
        val gpsOptions = listOf(1, 5, 10, 20)
        SingleSelectDialog(
            title = "GPS rate",
            options = gpsOptions,
            selected = gpsRateHz,
            labelFor = { "$it Hz" },
            onSelect = { onSetGpsRate(it); showGpsPicker = false },
            onDismiss = { showGpsPicker = false },
        )
    }

    if (showImuPicker) {
        val imuOptions = listOf(50, 100, 200)
        SingleSelectDialog(
            title = "IMU rate",
            options = imuOptions,
            selected = imuRateHz,
            labelFor = { "$it Hz" },
            onSelect = { onSetImuRate(it); showImuPicker = false },
            onDismiss = { showImuPicker = false },
        )
    }

    if (showUnitsPicker) {
        SingleSelectDialog(
            title = "Units",
            options = Units.entries,
            selected = units,
            labelFor = { if (it == Units.IMPERIAL) "Imperial" else "Metric" },
            onSelect = { onSetUnits(it); showUnitsPicker = false },
            onDismiss = { showUnitsPicker = false },
        )
    }
}

// ---- Private helpers ----

@Composable
private fun SettingsGroupContainer(
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
private fun SettingsGroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 56.dp), // indent past icon area
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    showChevron: Boolean,
    modifier: Modifier = Modifier,
    trailingValue: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier,
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
        if (trailing != null) {
            trailing()
        } else {
            if (trailingValue != null) {
                Text(
                    text = trailingValue,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RawDataExportCard(
    storageSummary: String,
    onExportAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Raw data export",
                    fontSize = 16.sp,
                    fontWeight = FontWeight(600),
                )
            }

            // Summary line
            Text(
                text = storageSummary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
            )

            // Export all button
            Button(
                onClick = onExportAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export all",
                    fontSize = 15.sp,
                    fontWeight = FontWeight(600),
                )
            }
        }
    }
}

@Composable
private fun <T> SingleSelectDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelFor(option),
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ---- Previews ----

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    RaceLoggerTheme {
        SettingsScreen(
            gpsRateHz = 10,
            imuRateHz = 100,
            autoPause = false,
            units = Units.METRIC,
            darkTheme = false,
            tripCount = 142,
            storageSummary = "142 trips · 2.8 GB at full 100 ms resolution. CSV, GPX or FIT.",
            onSetGpsRate = {},
            onSetImuRate = {},
            onSetAutoPause = {},
            onSetUnits = {},
            onSetDarkTheme = {},
            onHelp = {},
            onExportAll = {},
        )
    }
}
