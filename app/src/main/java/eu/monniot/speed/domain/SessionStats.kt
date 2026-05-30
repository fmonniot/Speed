package eu.monniot.speed.domain

import eu.monniot.speed.data.DataPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computed per-session ride metrics, all in SI units.
 *
 * @property distanceM      Total GPS track length in metres, computed via haversine on consecutive
 *                          points that both have non-null lat/lon.
 * @property avgSpeedMs     Mean speed in m/s over *moving* points only (speed > 0.5 m/s).
 *                          0 if no moving points exist.
 * @property maxSpeedMs     Peak speed in m/s across all points. 0 if the session is empty.
 * @property maxLateralG    Maximum absolute lateral acceleration in G (>= 0).
 *                          0 if no lateralGz readings are present.
 * @property maxLeanDeg     Maximum absolute lean angle in degrees (>= 0).
 *                          0 if no leanAngleDeg readings are present.
 * @property hardBrakeG     Most-negative longitudinal deceleration in G (<= 0).
 *                          0 if the session is empty or the bike never decelerated.
 * @property movingPercent  Percentage (0..100) of data points whose speed source exceeds
 *                          the stationary threshold (0.5 m/s). 0 if session is empty.
 */
data class SessionStats(
    val distanceM: Float,
    val avgSpeedMs: Float,
    val maxSpeedMs: Float,
    val maxLateralG: Float,
    val maxLeanDeg: Float,
    val hardBrakeG: Float,
    val movingPercent: Int,
)

/** Threshold below which a point is considered stationary, in m/s. */
private const val STATIONARY_THRESHOLD_MS = 0.5f

/** Earth radius used for haversine distance calculations, in metres. */
private const val EARTH_RADIUS_M = 6_371_000.0

/** Standard gravity constant used to convert m/s² to G. */
private const val GRAVITY = 9.81f

/**
 * Computes [SessionStats] from an ordered (oldest-first) list of [DataPoint]s.
 *
 * Speed source per point: `derivedSpeedMs ?: gpsSpeedMs ?: 0f`.
 * All output values are in SI units (metres, m/s, G, degrees).
 */
object SessionStatsComputer {

    fun compute(points: List<DataPoint>): SessionStats {
        if (points.isEmpty()) {
            return SessionStats(
                distanceM = 0f,
                avgSpeedMs = 0f,
                maxSpeedMs = 0f,
                maxLateralG = 0f,
                maxLeanDeg = 0f,
                hardBrakeG = 0f,
                movingPercent = 0,
            )
        }

        // --- distance ---
        var distanceM = 0f
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val lat1 = prev.latitude ?: continue
            val lon1 = prev.longitude ?: continue
            val lat2 = curr.latitude ?: continue
            val lon2 = curr.longitude ?: continue
            distanceM += haversineMeters(lat1, lon1, lat2, lon2)
        }

        // --- per-point aggregation ---
        var maxSpeedMs = 0f
        var movingSpeedSum = 0.0
        var movingCount = 0
        var maxLateralG = 0f
        var maxLeanDeg = 0f
        var hardestBrakeMs2: Float? = null   // most negative derivedAccelMs2

        for (point in points) {
            val speed = point.derivedSpeedMs ?: point.gpsSpeedMs ?: 0f

            if (speed > maxSpeedMs) maxSpeedMs = speed

            if (speed > STATIONARY_THRESHOLD_MS) {
                movingSpeedSum += speed
                movingCount++
            }

            point.lateralGz?.let { lg ->
                val absLg = abs(lg)
                if (absLg > maxLateralG) maxLateralG = absLg
            }

            point.leanAngleDeg?.let { la ->
                val absLa = abs(la)
                if (absLa > maxLeanDeg) maxLeanDeg = absLa
            }

            point.derivedAccelMs2?.let { a ->
                if (hardestBrakeMs2 == null || a < hardestBrakeMs2!!) {
                    hardestBrakeMs2 = a
                }
            }
        }

        val avgSpeedMs = if (movingCount > 0) (movingSpeedSum / movingCount).toFloat() else 0f

        // Convert most-negative accel to G; clamp so it is never positive
        val hardBrakeG = if (hardestBrakeMs2 != null) {
            minOf(0f, hardestBrakeMs2!! / GRAVITY)
        } else {
            0f
        }

        val movingPercent = (100.0 * movingCount / points.size).roundToInt()

        return SessionStats(
            distanceM = distanceM,
            avgSpeedMs = avgSpeedMs,
            maxSpeedMs = maxSpeedMs,
            maxLateralG = maxLateralG,
            maxLeanDeg = maxLeanDeg,
            hardBrakeG = hardBrakeG,
            movingPercent = movingPercent,
        )
    }

    /**
     * Computes the great-circle distance between two WGS-84 coordinates using the haversine
     * formula.
     *
     * @param lat1 Latitude of the first point, in decimal degrees.
     * @param lon1 Longitude of the first point, in decimal degrees.
     * @param lat2 Latitude of the second point, in decimal degrees.
     * @param lon2 Longitude of the second point, in decimal degrees.
     * @return Distance in metres.
     */
    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_M * c).toFloat()
    }
}
