package eu.monniot.speed.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.viewmodel.RaceViewModel

@Composable
fun SessionDetailScreen(
    sessionId: String,
    viewModel: RaceViewModel,
    onBack: () -> Unit
) {
    var points by remember { mutableStateOf<List<DataPoint>>(emptyList()) }
    
    LaunchedEffect(sessionId) {
        points = viewModel.getPointsForSession(sessionId)
    }

    SessionDetailContent(
        points = points,
        onBack = onBack,
        onExport = { viewModel.exportSession(sessionId) },
        onDelete = {
            viewModel.deleteSession(sessionId)
            onBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailContent(
    points: List<DataPoint>,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (points.isEmpty()) {
                CircularProgressIndicator()
            } else {
                val maxSpeed = points.maxByOrNull { it.gpsSpeedMs ?: 0f }?.gpsSpeedMs ?: 0f
                val avgSpeed = points.map { it.gpsSpeedMs ?: 0f }.average().toFloat()
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Max Speed", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f km/h".format(maxSpeed * 3.6f), style = MaterialTheme.typography.headlineSmall)
                        }
                        Column {
                            Text("Avg Speed", style = MaterialTheme.typography.labelMedium)
                            Text("%.1f km/h".format(avgSpeed * 3.6f), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Speed Profile", style = MaterialTheme.typography.titleMedium)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SpeedChart(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Session Info", style = MaterialTheme.typography.titleMedium)
                ListItem(headlineContent = { Text("Data Points") }, trailingContent = { Text("${points.size}") })
                ListItem(headlineContent = { Text("Duration") }, trailingContent = { 
                    val duration = if (points.isNotEmpty()) (points.last().elapsedRealtimeNs - points.first().elapsedRealtimeNs) / 1_000_000_000 else 0
                    Text(formatDuration(duration.toInt()))
                })
            }
        }
    }
}

@Composable
fun SpeedChart(points: List<DataPoint>, modifier: Modifier = Modifier) {
    val speedData = points.map { it.gpsSpeedMs ?: 0f }
    if (speedData.isEmpty()) return

    val maxSpeed = speedData.maxOrNull() ?: 1f
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (speedData.size - 1).coerceAtLeast(1)
        
        val path = Path().apply {
            moveTo(0f, height - (speedData[0] / maxSpeed * height))
            for (i in 1 until speedData.size) {
                lineTo(i * spacing, height - (speedData[i] / maxSpeed * height))
            }
        }
        
        drawPath(
            path = path,
            color = Color(0xFFFF5252),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
