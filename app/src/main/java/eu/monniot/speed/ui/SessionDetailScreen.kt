package eu.monniot.speed.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import eu.monniot.speed.data.Session
import eu.monniot.speed.util.FormatUtils
import eu.monniot.speed.viewmodel.RaceViewModel

@Composable
fun SessionDetailScreen(
    sessionId: String,
    viewModel: RaceViewModel,
    onBack: () -> Unit
) {
    var points by remember { mutableStateOf<List<DataPoint>>(emptyList()) }
    var session by remember { mutableStateOf<Session?>(null) }
    
    LaunchedEffect(sessionId) {
        points = viewModel.getPointsForSession(sessionId)
        session = viewModel.getSession(sessionId)
    }

    SessionDetailContent(
        session = session,
        points = points,
        onBack = onBack,
        onExport = { viewModel.exportSession(sessionId) },
        onDelete = {
            viewModel.deleteSession(sessionId)
            onBack()
        },
        onNotesChange = { newNotes ->
            session?.let {
                viewModel.updateSessionNotes(it, newNotes)
                session = it.copy(notes = newNotes)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailContent(
    session: Session?,
    points: List<DataPoint>,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onNotesChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (session == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val maxSpeed = points.maxByOrNull { it.gpsSpeedMs ?: 0f }?.gpsSpeedMs ?: 0f
                val avgSpeed = if (points.isNotEmpty()) points.map { it.gpsSpeedMs ?: 0f }.average().toFloat() else 0f
                
                // 1. Summary Card
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
                
                // 2. Speed Profile (Visualization)
                Text("Speed Profile", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SpeedChart(
                    points = points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // 3. Notes (Commentary)
                Text("Notes", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = session.notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add notes about this session...") },
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                // 4. Session Info (Metadata)
                Text("Session Info", style = MaterialTheme.typography.titleMedium)

                val timeInfo = remember(session.startTimeMs, session.endTimeMs) {
                    formatSessionTime(session.startTimeMs, session.endTimeMs)
                }

                ListItem(
                    headlineContent = { Text("Time") },
                    trailingContent = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(timeInfo.primary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = timeInfo.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                ListItem(headlineContent = { Text("Data Points") }, trailingContent = { Text("${points.size}") })
                ListItem(headlineContent = { Text("Duration") }, trailingContent = { 
                    val duration = if (points.size >= 2) (points.last().elapsedRealtimeNs - points.first().elapsedRealtimeNs) / 1_000_000_000 else 0
                    Text(FormatUtils.formatDuration(duration.toInt()))
                })
            }
        }
    }
}

@Composable
fun SpeedChart(points: List<DataPoint>, modifier: Modifier = Modifier) {
    val speedData = points.map { it.gpsSpeedMs ?: 0f }
    if (speedData.size < 2) {
        Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Not enough data to display chart", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxSpeed = speedData.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (speedData.size - 1)
        
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
