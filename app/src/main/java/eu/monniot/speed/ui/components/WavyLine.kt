package eu.monniot.speed.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.monniot.speed.ui.theme.RaceLoggerTheme
import kotlin.math.PI
import kotlin.math.sin

// M3 Expressive wavy progress indicator (component-reference.md §WavyLine).
// Algorithm: y(x) = 8 + amp·sin((x / period)·π), amp=3, period=14 (wavelength 28 dp).
// Canvas height 16 dp, centerline at y=8 dp.
@Composable
fun WavyLine(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
    ) {
        val w = size.width
        val centerY = size.height / 2f
        val amp = 3.dp.toPx()
        val period = 14.dp.toPx()
        val strokeWidth = 3.dp.toPx()
        val dotRadius = 4.dp.toPx()

        val wavePath = buildWavePath(w, centerY, amp, period)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        // 1 — full-width wave in track color
        drawPath(wavePath, trackColor, style = stroke)

        // 2 — progress-clipped wave in accent color
        val progressX = clamped * w
        clipRect(right = progressX) {
            drawPath(wavePath, color, style = stroke)
        }

        // 3 — leading dot at the progress head
        drawCircle(color = color, radius = dotRadius, center = Offset(progressX, centerY))
    }
}

private fun buildWavePath(width: Float, centerY: Float, amp: Float, period: Float): Path {
    val path = Path()
    var x = 0f
    var first = true
    while (x <= width + 1f) {
        val y = centerY + amp * sin((x / period) * PI).toFloat()
        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }
        x += 1f
    }
    return path
}

@PreviewLightDark
@Composable
private fun WavyLinePreview() {
    RaceLoggerTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WavyLine(progress = 0.0f)
            WavyLine(progress = 0.35f)
            WavyLine(progress = 0.64f)
            WavyLine(progress = 1.0f)
        }
    }
}
