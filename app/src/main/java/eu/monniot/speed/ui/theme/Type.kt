package eu.monniot.speed.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Design note: spec calls for Roboto Flex (variable font). Using system sans-serif as a
// practical fallback; swap in a downloaded Roboto Flex font resource in the G1 cleanup.
val SpeedTypography = Typography(
    // Hero numerals — live speed (104 sp/700, tight line-height + letter-spacing)
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 104.sp,
        lineHeight = 93.6.sp,
        letterSpacing = (-0.04f).em,
    ),
    // Hero numerals — summary / segment (80 sp/700)
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 80.sp,
        lineHeight = 72.sp,
        letterSpacing = (-0.04f).em,
    ),
    // Hero numerals — stats distance (56 sp/700)
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 50.4.sp,
        letterSpacing = (-0.03f).em,
    ),
    // Screen headings — larger (28 sp/500)
    headlineMedium = TextStyle(
        fontWeight = FontWeight(500),
        fontSize = 28.sp,
    ),
    // Screen headings — smaller (24 sp/500)
    headlineSmall = TextStyle(
        fontWeight = FontWeight(500),
        fontSize = 24.sp,
    ),
    // Top-bar title (22 sp/400)
    titleLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
    ),
    // Card stat values — standard (20 sp/600)
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.02f).em,
    ),
    // Body / list primary (16 sp/500)
    bodyLarge = TextStyle(
        fontWeight = FontWeight(500),
        fontSize = 16.sp,
    ),
    // Body / list smaller (15 sp/500)
    bodyMedium = TextStyle(
        fontWeight = FontWeight(500),
        fontSize = 15.sp,
    ),
    // Caption / meta (13 sp/500)
    labelLarge = TextStyle(
        fontWeight = FontWeight(500),
        fontSize = 13.sp,
    ),
    // Section label / caption (12 sp/600, apply uppercase + letterSpacing at call site)
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    ),
    // Small caption (11 sp/400)
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    ),
)

// Named hero-numeral styles for components that reference them directly.
// These mirror the displayLarge/Medium/Small roles but are named by use case.
object SpeedTextStyles {
    // 104 sp — live-speed hero numeral (Live HUD)
    val heroLiveSpeed = SpeedTypography.displayLarge
    // 80 sp — top-speed / PB hero numeral (Summary, Segment detail)
    val heroSummary = SpeedTypography.displayMedium
    // 64 sp — segment PB time (uses displayMedium sized down slightly)
    val heroSegmentTime = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 57.6.sp,
        letterSpacing = (-0.03f).em,
    )
    // 56 sp — stats distance hero
    val heroStats = SpeedTypography.displaySmall
    // 30 sp/600 — large card stat values (e.g. last-ride card inline stats)
    val cardStatLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.02f).em,
    )
    // 26 sp/600 — records-grid card values
    val cardStatMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
    )
    // 22 sp/600 — stat-grid values in Summary
    val cardStatSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    )
}
