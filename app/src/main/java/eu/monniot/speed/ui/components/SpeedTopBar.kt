package eu.monniot.speed.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.monniot.speed.ui.theme.RaceLoggerTheme

// 56 dp top app bar per spec §2.5 and component-reference §M3TopBar.
// Background = surface; trailing icon tinted primary; optional leading arrow_back.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingAction: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (trailingIcon != null && onTrailingAction != null) {
                IconButton(onClick = onTrailingAction) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun SpeedTopBarPreview() {
    RaceLoggerTheme {
        androidx.compose.foundation.layout.Column {
            SpeedTopBar(title = "Speed")
            SpeedTopBar(
                title = "Trip summary",
                onBack = {},
                trailingIcon = Icons.Rounded.Share,
                onTrailingAction = {},
            )
        }
    }
}
