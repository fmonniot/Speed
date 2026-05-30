package eu.monniot.speed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens

// Destination descriptor for SpeedBottomNav; wired to actual routes in B1.
data class SpeedNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

// 72 dp bottom navigation bar per spec §2.5 and component-reference §M3Nav.
// Active item: 56×32 secondaryContainer pill behind a filled icon; surfaceContainer bg.
@Composable
fun SpeedBottomNav(
    items: List<SpeedNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(SpeedDimens.bottomNavHeight),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            items.forEach { item ->
                NavItemColumn(
                    item = item,
                    selected = currentRoute == item.route,
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun NavItemColumn(
    item: SpeedNavItem,
    selected: Boolean,
    onNavigate: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .width(64.dp)
            .clickable { onNavigate(item.route) },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = SpeedDimens.navPillWidth, height = SpeedDimens.navPillHeight)
                .clip(RoundedCornerShape(SpeedDimens.radiusNavPill))
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent,
                ),
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = if (selected)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight(500),
            color = if (selected)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.02.em,
        )
    }
}

private val previewItems = listOf(
    SpeedNavItem("ride", "Ride", Icons.Rounded.Home),
    SpeedNavItem("trips", "Trips", Icons.Rounded.Route),
    SpeedNavItem("stats", "Stats", Icons.Rounded.Leaderboard),
    SpeedNavItem("settings", "Settings", Icons.Rounded.Settings),
)

@PreviewLightDark
@Composable
private fun SpeedBottomNavPreview() {
    RaceLoggerTheme {
        SpeedBottomNav(
            items = previewItems,
            currentRoute = "trips",
            onNavigate = {},
        )
    }
}
