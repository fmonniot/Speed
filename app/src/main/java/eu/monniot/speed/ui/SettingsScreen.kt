package eu.monniot.speed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.monniot.speed.viewmodel.RaceViewModel

@Composable
fun SettingsScreen(
    viewModel: RaceViewModel,
    modifier: Modifier = Modifier
) {
    val autoStart by viewModel.autoStartSensors.collectAsState()
    
    SettingsScreenContent(
        autoStart = autoStart,
        onAutoStartChange = { viewModel.setAutoStartSensors(it) },
        modifier = modifier
    )
}

@Composable
fun SettingsScreenContent(
    autoStart: Boolean,
    onAutoStartChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            text = "Debug",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // TODO Add items for debug (things like battery wattage consumption, time remaining, etc…)
        // Mostly useful to understand how the app behave and −potentially− improve it
    }
}
