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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.monniot.speed.ui.RaceScreen
import eu.monniot.speed.ui.SessionDetailScreen
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.viewmodel.RaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                NavHost(navController = navController, startDestination = "race") {
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

    private fun shareFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Race Session CSV"))
    }
}
