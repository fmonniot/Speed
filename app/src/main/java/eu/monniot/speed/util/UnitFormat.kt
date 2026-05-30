package eu.monniot.speed.util

import androidx.compose.runtime.compositionLocalOf
import eu.monniot.speed.data.Units
import kotlin.math.abs
import kotlin.math.roundToInt

// Display-boundary formatting keyed off the active Units setting (§4.9, §5).
// All inputs are SI (m/s, m, m/s² → G already, degrees); outputs are display strings.
// `Value`/`label` variants are provided so callers can lay out the numeral and unit
// separately (e.g. hero numerals) or get a combined string.
object UnitFormat {

    // ---- Speed (input m/s) ----

    /** Speed numeral only, in the active unit (km/h or mph), rounded to an integer. */
    fun speedValue(ms: Float, units: Units): String {
        val v = if (units == Units.IMPERIAL) Conversions.msToMph(ms) else Conversions.msToKmh(ms)
        return v.roundToInt().toString()
    }

    fun speedUnit(units: Units): String = if (units == Units.IMPERIAL) "mph" else "km/h"

    /** Combined "142 km/h". */
    fun speed(ms: Float, units: Units): String = "${speedValue(ms, units)} ${speedUnit(units)}"

    // ---- Distance (input meters) ----

    /** Distance numeral only, in the active unit (km or mi), one decimal. */
    fun distanceValue(meters: Float, units: Units): String {
        val km = meters / 1000f
        val v = if (units == Units.IMPERIAL) Conversions.kmToMi(km) else km
        return "%.1f".format(v)
    }

    fun distanceUnit(units: Units): String = if (units == Units.IMPERIAL) "mi" else "km"

    /** Combined "54.8 km". */
    fun distance(meters: Float, units: Units): String =
        "${distanceValue(meters, units)} ${distanceUnit(units)}"

    // ---- Altitude (input meters) ----

    /** Altitude numeral only, in the active unit (m or ft), rounded to an integer. */
    fun altitudeValue(meters: Float, units: Units): String {
        val v = if (units == Units.IMPERIAL) Conversions.mToFt(meters) else meters
        return v.roundToInt().toString()
    }

    fun altitudeUnit(units: Units): String = if (units == Units.IMPERIAL) "ft" else "m"

    /** Combined "1247 m". */
    fun altitude(meters: Float, units: Units): String =
        "${altitudeValue(meters, units)} ${altitudeUnit(units)}"

    // ---- Lateral G (input already in G, unit-independent) ----

    /** Lateral G value, two decimals, e.g. "0.84". */
    fun lateralGValue(g: Float): String = "%.2f".format(g)

    fun lateralGUnit(): String = "g lat"

    fun lateralG(g: Float): String = "${lateralGValue(g)} ${lateralGUnit()}"

    // ---- Lean angle (input signed degrees, + = right) ----

    /** Lean magnitude, e.g. "38°". */
    fun leanValue(deg: Float): String = "${abs(deg).roundToInt()}°"

    /** Direction word for a signed lean ("right"/"left"), empty when near zero. */
    fun leanDirection(deg: Float): String = when {
        deg > 0.5f -> "right"
        deg < -0.5f -> "left"
        else -> ""
    }

    fun lean(deg: Float): String {
        val dir = leanDirection(deg)
        return if (dir.isEmpty()) leanValue(deg) else "${leanValue(deg)} $dir"
    }
}

// Provides the active Units to the composable tree so screens read it from one place
// without threading it manually (C2). Defaults to METRIC; overridden in the app shell.
val LocalUnits = compositionLocalOf { Units.METRIC }
