package eu.monniot.speed.domain

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Segment
import eu.monniot.speed.data.decodePath
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Result of matching a single [Segment] against a finished ride's GPS track.
 *
 * @param segmentId   Identifier of the matched segment.
 * @param elapsedTimeMs Time in milliseconds from the point nearest the segment start to the point
 *                    nearest the segment end.
 * @param maxSpeedMs  Maximum speed (m/s) observed in the matched sub-track, using
 *                    `derivedSpeedMs` when available and falling back to `gpsSpeedMs`. Null values
 *                    are treated as 0.
 * @param maxLateralG Maximum absolute lateral G force (`|lateralGz|`) in the matched sub-track.
 *                    Null values are skipped (treated as 0 when no non-null value exists).
 * @param maxLeanDeg  Maximum absolute lean angle (`|leanAngleDeg|`) in degrees in the matched
 *                    sub-track. Null values are skipped.
 */
data class SegmentMatchResult(
    val segmentId: String,
    val elapsedTimeMs: Long,
    val maxSpeedMs: Float,
    val maxLateralG: Float,
    val maxLeanDeg: Float,
)

/**
 * Pure, deterministic heuristic matcher that detects which segments from a known set were crossed
 * during a finished ride, and extracts timing and performance data for each crossing.
 *
 * ## Detection heuristic
 * For each segment the matcher:
 * 1. Decodes the segment's polyline to obtain its **start** (first waypoint) and **end** (last
 *    waypoint).
 * 2. Scans the ride track (GPS-valid points only) to find `iStart` — the index of the point whose
 *    haversine distance to the segment start is **minimum** and **≤ thresholdMeters**.
 * 3. Scans the portion of the track **at or after** `iStart` to find `iEnd` — the index of the
 *    point nearest the segment end that is also **≤ thresholdMeters** away.
 * 4. If either endpoint is never approached within threshold, or `iEnd ≤ iStart` (rider went the
 *    wrong way or start/end are the same point), the segment is **not** counted.
 *
 * ### Limitations (first-pass detector)
 * This approach only checks the segment's two endpoints, not its intermediate waypoints. A future
 * version could compute the minimum distance from each track point to the full polyline and require
 * the rider to stay within threshold along the entire route — at the cost of O(|track| × |poly|)
 * per segment. For the common case of point-to-point road segments on a fixed circuit this
 * endpoint-only heuristic is sufficient.
 */
object SegmentMatcher {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * Matches [segments] against the GPS track contained in [points].
     *
     * @param points          All [DataPoint]s from a finished ride (order preserved).
     * @param segments        Candidate segments to match.
     * @param thresholdMeters Maximum haversine distance (metres) from a track point to a segment
     *                        endpoint for the point to be considered an "approach". Default 25 m.
     * @return One [SegmentMatchResult] per segment that was crossed, in the same order as
     *         [segments]. Segments that were not crossed are omitted.
     */
    fun match(
        points: List<DataPoint>,
        segments: List<Segment>,
        thresholdMeters: Double = 25.0,
    ): List<SegmentMatchResult> {
        if (points.isEmpty() || segments.isEmpty()) return emptyList()

        // Keep only points with valid GPS coordinates, preserving order.
        val gpsPoints = points.filter { it.latitude != null && it.longitude != null }
        if (gpsPoints.isEmpty()) return emptyList()

        val results = mutableListOf<SegmentMatchResult>()

        for (segment in segments) {
            val waypoints = decodePath(segment.pathPolyline)
            if (waypoints.size < 2) continue

            val (startLat, startLon) = waypoints.first()
            val (endLat, endLon) = waypoints.last()

            // Find the track point closest to the segment start.
            var iStart = -1
            var minStartDist = Double.MAX_VALUE
            for (i in gpsPoints.indices) {
                val p = gpsPoints[i]
                val d = haversineMeters(p.latitude!!, p.longitude!!, startLat, startLon)
                if (d < minStartDist) {
                    minStartDist = d
                    iStart = i
                }
            }
            if (iStart == -1 || minStartDist > thresholdMeters) continue

            // Find the track point closest to the segment end, searching only after iStart.
            var iEnd = -1
            var minEndDist = Double.MAX_VALUE
            for (i in (iStart + 1) until gpsPoints.size) {
                val p = gpsPoints[i]
                val d = haversineMeters(p.latitude!!, p.longitude!!, endLat, endLon)
                if (d < minEndDist) {
                    minEndDist = d
                    iEnd = i
                }
            }
            if (iEnd == -1 || minEndDist > thresholdMeters) continue
            // iEnd > iStart is guaranteed by the search range above (iStart + 1 .. size-1),
            // but we double-check for safety.
            if (iEnd <= iStart) continue

            val startPoint = gpsPoints[iStart]
            val endPoint = gpsPoints[iEnd]
            val elapsedMs = endPoint.wallClockMs - startPoint.wallClockMs

            // Aggregate stats over the inclusive sub-track [iStart, iEnd].
            var maxSpeed = 0f
            var maxLateralG = 0f
            var maxLeanDeg = 0f
            for (i in iStart..iEnd) {
                val p = gpsPoints[i]
                val speed = p.derivedSpeedMs ?: p.gpsSpeedMs ?: 0f
                if (speed > maxSpeed) maxSpeed = speed
                val latG = abs(p.lateralGz ?: 0f)
                if (latG > maxLateralG) maxLateralG = latG
                val lean = abs(p.leanAngleDeg ?: 0f)
                if (lean > maxLeanDeg) maxLeanDeg = lean
            }

            results += SegmentMatchResult(
                segmentId = segment.segmentId,
                elapsedTimeMs = elapsedMs,
                maxSpeedMs = maxSpeed,
                maxLateralG = maxLateralG,
                maxLeanDeg = maxLeanDeg,
            )
        }

        return results
    }

    /**
     * Computes the haversine (great-circle) distance in metres between two geographic coordinates.
     *
     * Uses Earth radius = [EARTH_RADIUS_M] (6 371 000 m).
     */
    private fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }
}
