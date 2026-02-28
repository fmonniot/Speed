package eu.monniot.speed.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.service.ServiceState
import eu.monniot.speed.viewmodel.RaceViewModel
import kotlin.math.abs

@Composable
fun RaceScreen(
    viewModel: RaceViewModel,
    onSessionClick: (String) -> Unit = {},
    showPastSessions: Boolean = true
) {
    val serviceState by viewModel.serviceState.collectAsState()
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())

    RaceScreenContent(
        serviceState = serviceState,
        sessions = sessions,
        onStart = { viewModel.startRecording() },
        onStop = { viewModel.stopRecording() },
        onSessionClick = onSessionClick,
        onDeleteSession = { viewModel.deleteSession(it) },
        showPastSessions = showPastSessions
    )
}

@Composable
fun RaceScreenContent(
    serviceState: ServiceState,
    sessions: List<SessionSummary> = emptyList(),
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSessionClick: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    showPastSessions: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RecordingStatusHeader(serviceState)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        SpeedDisplay(serviceState)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        MetricsDisplay(serviceState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GpsQualityIndicator(serviceState)
        
        Spacer(modifier = Modifier.weight(1f))
        
        StartStopButton(
            isRecording = serviceState.isRecording,
            onStart = onStart,
            onStop = onStop
        )
        
        if (showPastSessions) {
            Spacer(modifier = Modifier.height(32.dp))
            
            PastSessionsList(
                sessions = sessions,
                onSessionClick = onSessionClick,
                onDelete = onDeleteSession
            )
        }
    }
}

@Composable
fun RecordingStatusHeader(state: ServiceState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.isRecording) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ), label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "RECORDING",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                Text(
                    "READY",
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        
        Text(
            text = formatDuration(state.elapsedSeconds),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SpeedDisplay(state: ServiceState) {
    val speedKmh = state.currentSpeedMs * 3.6f
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%.1f".format(speedKmh),
            fontSize = 80.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "km/h",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun MetricsDisplay(state: ServiceState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Fix: Render values that round to 0.0 as positive 0.0 to avoid "-0.0" flickering
        val displayAccel = if (abs(state.currentAccelMs2) < 0.05f) 0.0f else state.currentAccelMs2
        MetricItem(label = "Accel", value = "%.1f m/s²".format(displayAccel))
        
        VerticalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))

        MetricItem(label = "3D G", value = "%.2f".format(state.currentG))
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
    }
}

@Composable
fun GpsQualityIndicator(state: ServiceState) {
    val accuracy = state.currentAccuracyM
    val sats = state.satellites
    val color = when {
        accuracy == null -> Color.Red
        accuracy < 5f -> Color.Green
        accuracy < 15f -> Color.Yellow
        else -> Color.Red
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        val gpsText = if (accuracy == null) {
            if (sats.visible > 0) {
                "GPS: Searching... (${sats.visible} visible)"
            } else {
                "GPS: Searching..."
            }
        } else {
            "GPS: ±%.0fm (%d/%d sats)".format(accuracy, sats.usedInFix, sats.visible)
        }
        Text(
            text = gpsText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun StartStopButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Button(
        onClick = { if (isRecording) onStop() else onStart() },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) Color.DarkGray else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (isRecording) "STOP RACE" else "START RACE",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PastSessionsList(
    sessions: List<SessionSummary>,
    onSessionClick: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Past Sessions",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(sessions) { session ->
                SessionItem(session, onSessionClick, onDelete)
            }
        }
    }
}

@Composable
fun SessionItem(
    session: SessionSummary,
    onSessionClick: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSessionClick(session.sessionId) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Session ${session.sessionId.take(6)}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Max: %.1f km/h".format((session.maxSpeedMs ?: 0f) * 3.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { onDelete(session.sessionId) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Details", tint = Color.Gray)
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
