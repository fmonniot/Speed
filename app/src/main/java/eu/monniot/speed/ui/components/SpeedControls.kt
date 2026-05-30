package eu.monniot.speed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import eu.monniot.speed.ui.theme.SpeedDimens

// Switch per spec §2.5 and component-reference §Switch (52×32).
// On: primary track + onPrimary thumb. Off: surfaceContainerHighest + outline border + outline thumb.
@Composable
fun SpeedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier,
    )
}

// Extended FAB per spec §2.5 and component-reference §Buttons & FAB.
// Height 56, radius 18, primary bg. Icon 24 + label 16 sp/600, onPrimary.
@Composable
fun SpeedExtendedFab(
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(SpeedDimens.radiusFab),
        modifier = modifier.height(SpeedDimens.fabHeight),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
        text = {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

// Full-width filled button per spec §2.5 and component-reference §Buttons & FAB.
// Height 56, radius 28. Defaults to primary bg; set containerColor = error for Stop.
@Composable
fun SpeedFullWidthButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(SpeedDimens.radiusFullWidthButton),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(SpeedDimens.fullWidthButtonHeight),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@PreviewLightDark
@Composable
private fun SpeedControlsPreview() {
    RaceLoggerTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var checked by remember { mutableStateOf(true) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark theme", modifier = Modifier.weight(1f))
                SpeedSwitch(checked = checked, onCheckedChange = { checked = it })
            }
            SpeedExtendedFab(
                onClick = {},
                label = "Record",
                icon = Icons.Rounded.FiberManualRecord,
            )
            SpeedFullWidthButton(
                onClick = {},
                label = "Stop",
                icon = Icons.Rounded.Stop,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
            SpeedFullWidthButton(
                onClick = {},
                label = "Export 142 trips · 2.6 GB",
            )
        }
    }
}
