package eu.monniot.speed.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// M3 Shapes wired to the spec's corner-radius scale (§2.4):
//   extraSmall → filter/sort/range chips (8 dp)
//   small      → recording banner, small chip-buttons (12 dp)
//   medium     → bordered link rows (16 dp)
//   large      → medium tonal cards (20 dp)
//   extraLarge → hero / large cards, full-width buttons (28 dp)
val SpeedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
