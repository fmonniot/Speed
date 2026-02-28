package eu.monniot.speed

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import eu.monniot.speed.ui.RaceScreen
import eu.monniot.speed.ui.SessionDetailScreen
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.viewmodel.RaceViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure the app content is laid out behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            RaceLoggerTheme {
                val navController = rememberNavController()
                val viewModel: RaceViewModel = viewModel()

                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Handle permissions result
                }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionsLauncher.launch(permissions.toTypedArray())
                }

                LaunchedEffect(viewModel.exportUri) {
                    viewModel.exportUri.collect { uri ->
                        shareFile(uri)
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Race Logger") },
                            actions = {
                                IconButton(onClick = {
                                    viewModel.stopRecording()
                                    finishAndRemoveTask()
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close and stop background service"
                                    )
                                }
                            },
                            // Optional: make the top bar slightly transparent or match background
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "race"
                        ) {
                            composable("race") {
                                RaceScreen(
                                    viewModel = viewModel,
                                    onSessionClick = { sessionId ->
                                        navController.navigate("session_detail/$sessionId")
                                    }
                                )
                            }
                            composable("session_detail/{sessionId}") { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                                SessionDetailScreen(
                                    sessionId = sessionId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shareFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Race Session CSV"))
    }
}
