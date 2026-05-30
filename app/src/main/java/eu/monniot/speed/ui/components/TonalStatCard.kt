package eu.monniot.speed.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens

// Tonal stat card per spec §2.5 and component-reference §TonalStatCard.
// Container color + matching on* text; optional top-row label + trailing icon;
// large value numeral; small caption beneath. Non-interactive (decorative contract).
@Composable
fun TonalStatCard(
    value: String,
    caption: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(SpeedDimens.radiusMediumTonal),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (label != null || icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (label != null) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02f).em,
                lineHeight = 20.sp,
                color = contentColor,
            )
            Text(
                text = caption,
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.8f),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TonalStatCardPreview() {
    RaceLoggerTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            TonalStatCard(
                value = "4 rides",
                caption = "This week",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                label = "THIS WEEK",
                icon = Icons.Rounded.Speed,
                modifier = Modifier.weight(1f),
            )
            TonalStatCard(
                value = "8 412 km",
                caption = "Total",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                label = "TOTAL",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
