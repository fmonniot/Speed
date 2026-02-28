package eu.monniot.speed.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable
fun SessionsScreenContent(
    sessions: List<SessionSummary>,
    onSessionClick: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Past Sessions",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        PastSessionsList(
            sessions = sessions,
            onSessionClick = onSessionClick,
            onDelete = onDelete
        )
    }
}