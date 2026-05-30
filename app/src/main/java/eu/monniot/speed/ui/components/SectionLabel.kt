package eu.monniot.speed.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.monniot.speed.ui.theme.RaceLoggerTheme

// Section header label per spec §2.5 and component-reference §SectionLabel.
// 13 sp/600, uppercase, letter-spacing, primary color; left padding 4 dp.
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.04.em,
        modifier = modifier.padding(start = 4.dp),
    )
}

@PreviewLightDark
@Composable
private fun SectionLabelPreview() {
    RaceLoggerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionLabel("Sampling")
            SectionLabel("Display")
        }
    }
}
