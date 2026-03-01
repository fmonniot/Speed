package eu.monniot.speed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.viewmodel.RaceViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SessionsScreen(
    viewModel: RaceViewModel,
    onSessionClick: (String) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())

    SessionsScreenContent(
        sessions = sessions,
        onSessionClick = onSessionClick,
        onDelete = { viewModel.deleteSession(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreenContent(
    sessions: List<SessionSummary>,
    onSessionClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past Sessions") }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            PastSessionsList(
                sessions = sessions,
                onSessionClick = onSessionClick,
                onDelete = onDelete
            )
        }
    }
}

@Composable
fun PastSessionsList(
    sessions: List<SessionSummary>,
    onSessionClick: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        items(sessions) { session ->
            SessionItem(session, onSessionClick, onDelete)
        }
    }
}


@Composable
fun SessionItem(
    session: SessionSummary,
    onSessionClick: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val timeInfo = remember(session.startTimeMs, session.endTimeMs) {
        formatSessionTime(session.startTimeMs, session.endTimeMs)
    }
    
    val dateDisplay = if (session.endTimeMs == null) {
        "${timeInfo.primary} • ${timeInfo.secondary}"
    } else {
        "${timeInfo.primary} • ${timeInfo.secondary}"
    }

    val durationDisplay = remember(session.startTimeMs, session.endTimeMs) {
        session.endTimeMs?.let { endTime ->
            val duration = (endTime - session.startTimeMs).milliseconds
            duration.toComponents { hours, minutes, seconds, _ ->
                if (hours > 0) {
                    "%dh %02dm %02ds".format(hours, minutes, seconds)
                } else {
                    "%02dm %02ds".format(minutes, seconds)
                }
            }
        } ?: "Ongoing"
    }

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
                    text = dateDisplay,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Max: %.1f km/h".format((session.maxSpeedMs ?: 0f) * 3.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Duration: $durationDisplay",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "ID: ${session.sessionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = { onDelete(session.sessionId) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}
