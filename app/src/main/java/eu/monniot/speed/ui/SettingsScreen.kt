package eu.monniot.speed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.viewmodel.RaceViewModel

@Composable
fun SettingsScreen(
    viewModel: RaceViewModel,
    modifier: Modifier = Modifier
) {
    val autoStart by viewModel.autoStartSensors.collectAsState()
    val serviceState by viewModel.serviceState.collectAsState()
    
    SettingsScreenContent(
        autoStart = autoStart,
        onAutoStartChange = { viewModel.setAutoStartSensors(it) },
        batteryWattage = serviceState.batteryWattage,
        batteryCapacityMah = serviceState.batteryCapacityMah,
        batteryTimeRemainingMs = serviceState.batteryTimeRemainingMs,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    autoStart: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
    batteryWattage: Float?,
    batteryCapacityMah: Int?,
    batteryTimeRemainingMs: Long?,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "General",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Auto-start sensors") },
                supportingContent = { Text("Automatically enable High Precision Mode when the app opens") },
                leadingContent = {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = autoStart,
                        onCheckedChange = onAutoStartChange
                    )
                },
                modifier = Modifier.clickable { onAutoStartChange(!autoStart) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Battery (debug)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
            )
            Text(
                text = "Battery metrics are updated every 5 seconds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Battery Usage") },
                supportingContent = {
                    val wattageText = if (batteryWattage != null) "%+.2f W".format(batteryWattage) else "Unknown"
                    val capacityText = if (batteryCapacityMah != null) "$batteryCapacityMah mAh" else "Unknown"
                    Text("Wattage: $wattageText\nCapacity: $capacityText")
                },
                leadingContent = {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text("Battery Time Remaining") },
                supportingContent = {
                    val timeText = if (batteryTimeRemainingMs != null && batteryTimeRemainingMs > 0) {
                        val hours = batteryTimeRemainingMs / 3_600_000
                        val minutes = (batteryTimeRemainingMs % 3_600_000) / 60_000
                        "${hours}h ${minutes}m"
                    } else {
                        "Unknown (Calculating...)"
                    }
                    Text(timeText)
                },
                leadingContent = {
                    Icon(Icons.Default.Timer, contentDescription = null)
                }
            )
        }
    }
}
