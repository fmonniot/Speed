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
            .heightIn(max = 300.dp)
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
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}