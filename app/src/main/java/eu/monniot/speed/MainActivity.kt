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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.monniot.speed.ui.ExportPlaceholder
import eu.monniot.speed.ui.isThisWeek
import eu.monniot.speed.ui.LiveHudScreen
import eu.monniot.speed.ui.RideHomeScreen
import eu.monniot.speed.ui.SegmentDetailPlaceholder
import eu.monniot.speed.ui.SegmentsPlaceholder
import eu.monniot.speed.ui.SettingsPlaceholder
import eu.monniot.speed.ui.StatsPlaceholder
import eu.monniot.speed.ui.SummaryScreen
import eu.monniot.speed.ui.TraceScreen
import eu.monniot.speed.ui.TripsFilter
import eu.monniot.speed.ui.TripsScreen
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import eu.monniot.speed.ui.components.SpeedBottomNav
import eu.monniot.speed.ui.components.SpeedNavItem
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.util.LocalUnits
import eu.monniot.speed.viewmodel.RaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: RaceViewModel = viewModel()
            // C3: theme follows the dark_theme preference, not the system setting.
            val darkTheme by viewModel.darkTheme.collectAsState()

            RaceLoggerTheme(darkTheme = darkTheme) {
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
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Race Session Data"))
    }
}

// Route names for the whole nav graph (B2).
object Routes {
    const val RIDE = "ride"
    const val LIVE = "live"
    const val SUMMARY = "summary/{sessionId}"
    const val TRIPS = "trips"
    // Trips accepts an optional ?filter= so the Ride "This week" card can deep-link pre-filtered.
    // Navigating to bare "trips" (bottom nav) resolves to this pattern with the default "all".
    const val TRIPS_PATTERN = "trips?filter={filter}"
    const val TRACE = "trace/{sessionId}"
    const val STATS = "stats"
    const val SEGMENTS = "segments"
    const val SEGMENT = "segment/{segmentId}"
    const val SETTINGS = "settings"
    const val EXPORT = "export"

    fun summary(id: String) = "summary/$id"
    fun trace(id: String) = "trace/$id"
    fun segment(id: String) = "segment/$id"
    fun tripsThisWeek() = "trips?filter=week"

    // Routes that show the bottom nav. Sub-screens / full-bleed routes hide it
    // (Live, Summary, Trace, Segment detail, Export) per §4 / B1.
    val bottomBar = setOf(RIDE, TRIPS, STATS, SEGMENTS, SETTINGS)
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
    val currentRoute = navBackStackEntry?.destination?.route

    val units by viewModel.units.collectAsState()

    CompositionLocalProvider(LocalUnits provides units) {
        SpeedAppShell(
            currentRoute = currentRoute,
            onTabSelected = { destination ->
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        ) {
            SpeedNavHost(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
private fun SpeedNavHost(navController: NavHostController, viewModel: RaceViewModel) {
    NavHost(
        navController = navController,
        startDestination = Routes.RIDE,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.RIDE) {
            val serviceState by viewModel.serviceState.collectAsState()
            val gpsRateHz by viewModel.gpsRateHz.collectAsState()
            val sessions by viewModel.sessions.collectAsState(initial = emptyList())
            RideHomeScreen(
                serviceState = serviceState,
                gpsRateHz = gpsRateHz,
                lastRide = sessions.firstOrNull(),
                thisWeekCount = sessions.count { isThisWeek(it.startTimeMs) },
                onRecord = {
                    viewModel.startRecording()
                    navController.navigate(Routes.LIVE)
                },
                onOpenSummary = { id -> navController.navigate(Routes.summary(id)) },
                onOpenTrips = { navController.navigate(Routes.tripsThisWeek()) },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
        composable(Routes.LIVE) {
            val serviceState by viewModel.serviceState.collectAsState()
            val gpsRateHz by viewModel.gpsRateHz.collectAsState()
            LiveHudScreen(
                serviceState = serviceState,
                gpsRateHz = gpsRateHz,
                onStop = {
                    // Capture the session id before stopRecording clears it from state.
                    val id = viewModel.serviceState.value.sessionId ?: ""
                    viewModel.stopRecording()
                    // Replace Live with the Summary so Back doesn't return to the HUD.
                    navController.navigate(Routes.summary(id)) {
                        popUpTo(Routes.LIVE) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SUMMARY) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val sessions by viewModel.sessions.collectAsState(initial = emptyList())
            var session by remember(sessionId) { mutableStateOf<Session?>(null) }
            LaunchedEffect(sessionId) { session = viewModel.getSession(sessionId) }
            // NEW PB when this session's top speed beats every other recorded session (E5 will
            // add the per-segment PB count; this is the all-time top-speed PB for the badge).
            val topSpeed = session?.maxSpeedMs
            val isNewPb = topSpeed != null &&
                sessions.filter { it.sessionId != sessionId }.all { (it.maxSpeedMs ?: 0f) < topSpeed }
            SummaryScreen(
                session = session,
                isNewPb = isNewPb,
                maxLateralG = null,
                maxLeanDeg = null,
                distanceM = null,
                avgSpeedMs = null,
                hardBrakeG = null,
                movingPercent = null,
                segmentPbCount = null,
                onBack = { navController.popBackStack() },
                onShare = { viewModel.exportSession(sessionId) },
                onOpenTrace = { navController.navigate(Routes.trace(sessionId)) },
                onOpenSegments = { navController.navigate(Routes.SEGMENTS) },
            )
        }
        composable(
            route = Routes.TRIPS_PATTERN,
            arguments = listOf(navArgument("filter") {
                type = NavType.StringType
                defaultValue = "all"
            }),
        ) { backStackEntry ->
            val sessions by viewModel.sessions.collectAsState(initial = emptyList())
            val filter = if (backStackEntry.arguments?.getString("filter") == "week") {
                TripsFilter.THIS_WEEK
            } else {
                TripsFilter.ALL
            }
            TripsScreen(
                sessions = sessions,
                initialFilter = filter,
                onSearch = {},
                onOpenSummary = { id -> navController.navigate(Routes.summary(id)) },
            )
        }
        composable(Routes.TRACE) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            var points by remember(sessionId) { mutableStateOf<List<DataPoint>>(emptyList()) }
            LaunchedEffect(sessionId) { points = viewModel.getPointsForSession(sessionId) }
            TraceScreen(
                points = points,
                onBack = { navController.popBackStack() },
                onDownload = { viewModel.exportSession(sessionId) },
            )
        }
        composable(Routes.STATS) {
            StatsPlaceholder(
                onDateRange = {},
                onOpenSummary = { id -> navController.navigate(Routes.summary(id)) },
                onOpenTrips = { navController.navigate(Routes.TRIPS) },
                onOpenSegments = { navController.navigate(Routes.SEGMENTS) },
            )
        }
        composable(Routes.SEGMENTS) {
            SegmentsPlaceholder(
                onBack = { navController.popBackStack() },
                onAdd = {},
                onOpenSegment = { id -> navController.navigate(Routes.segment(id)) },
            )
        }
        composable(Routes.SEGMENT) { backStackEntry ->
            val segmentId = backStackEntry.arguments?.getString("segmentId") ?: ""
            SegmentDetailPlaceholder(
                segmentId = segmentId,
                onBack = { navController.popBackStack() },
                onMore = {},
                onOpenTrace = { id -> navController.navigate(Routes.trace(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsPlaceholder(
                onHelp = {},
                onExportAll = { navController.navigate(Routes.EXPORT) },
            )
        }
        composable(Routes.EXPORT) {
            ExportPlaceholder(
                onBack = { navController.popBackStack() },
                onHelp = {},
            )
        }
    }
}

@Composable
fun SpeedAppShell(
    currentRoute: String?,
    onTabSelected: (MainDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    // Strip any optional query (e.g. "trips?filter=week") before matching nav membership.
    val baseRoute = currentRoute?.substringBefore('?')
    val showNav = baseRoute in Routes.bottomBar
    // Segment list keeps Stats highlighted (§4.7).
    val selectedRoute = if (baseRoute == Routes.SEGMENTS) Routes.STATS else baseRoute

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        if (showNav) {
            SpeedBottomNav(
                items = MainDestination.entries.map {
                    SpeedNavItem(it.route, it.label, it.icon, it.selectedIcon)
                },
                currentRoute = selectedRoute,
                onNavigate = { route ->
                    MainDestination.entries.firstOrNull { it.route == route }?.let(onTabSelected)
                },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    RIDE(Routes.RIDE, "Ride", Icons.Outlined.Home, Icons.Filled.Home),
    TRIPS(Routes.TRIPS, "Trips", Icons.Outlined.Route, Icons.Filled.Route),
    STATS(Routes.STATS, "Stats", Icons.Outlined.Leaderboard, Icons.Filled.Leaderboard),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}
