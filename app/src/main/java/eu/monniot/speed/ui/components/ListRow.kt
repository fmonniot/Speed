package eu.monniot.speed.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens

// List row per spec §2.5 and component-reference §ListRow.
// Leading 44 dp round icon chip; title + optional meta; trailing value or chevron.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    leadingIcon: ImageVector? = null,
    leadingContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    leadingContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingValue: String? = null,
    trailingUnit: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val rowModifier = when {
        // F1: long-press opens the per-trip delete confirmation in the Trips list.
        onClick != null && onLongClick != null ->
            modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        onClick != null ->
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        else -> modifier.fillMaxWidth()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = rowModifier.padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        if (leadingIcon != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(SpeedDimens.listRowIconSize)
                    .clip(CircleShape)
                    .background(leadingContainerColor),
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = leadingContentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (trailingValue != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = trailingValue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (trailingUnit != null) {
                    Text(
                        text = trailingUnit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ListRowPreview() {
    RaceLoggerTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
        ) {
            ListRow(
                title = "GPS rate",
                meta = "Capture frequency",
                leadingIcon = Icons.Rounded.Settings,
                trailingValue = "10 Hz",
                showChevron = true,
                onClick = {},
            )
            ListRow(
                title = "Col de Turini",
                meta = "26 May · 54.8 km",
                trailingValue = "187",
                trailingUnit = "km/h",
                onClick = {},
            )
        }
    }
}
