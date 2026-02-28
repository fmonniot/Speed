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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.monniot.speed.ui.RaceScreen
import eu.monniot.speed.ui.SessionDetailScreen
import eu.monniot.speed.ui.SessionsScreen
import eu.monniot.speed.ui.SettingsScreen
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.viewmodel.RaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RaceLoggerTheme {
                val viewModel: RaceViewModel = viewModel()

                LaunchedEffect(viewModel.exportUri) {
                    viewModel.exportUri.collect { uri ->
                        shareFile(uri)
                    }
                }

                SpeedApp(viewModel)
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

@Composable
fun SpeedApp(viewModel: RaceViewModel) {
    val navController = rememberNavController()

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    SpeedAppShell(
        currentDestination = currentDestination,
        onNavigate = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = MainDestination.RACE.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(MainDestination.RACE.route) {
                RaceScreen(
                    viewModel = viewModel,
                )
            }
            composable(MainDestination.SESSIONS.route) {
                SessionsScreen(
                    viewModel = viewModel,
                    onSessionClick = { sessionId ->
                        navController.navigate("session_detail/$sessionId")
                    }
                )
            }
            composable(MainDestination.SETTINGS.route) {
                SettingsScreen(viewModel = viewModel)
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

@Composable
fun SpeedAppShell(
    currentDestination: NavDestination?,
    onNavigate: (MainDestination) -> Unit,
    content: @Composable () -> Unit
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            MainDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                    onClick = { onNavigate(destination) }
                )
            }
        }
    ) {
        content()
    }
}

enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    RACE("race", "Race", Icons.Default.Speed),
    SESSIONS("sessions", "Sessions", Icons.Default.History),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}
