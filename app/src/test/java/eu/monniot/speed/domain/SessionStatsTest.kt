package eu.monniot.speed.domain

import eu.monniot.speed.data.DataPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStatsTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Constructs a [DataPoint] with sensible defaults for fields not under test.
     * Only override the fields relevant to each test case.
     */
    private fun dp(
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null,
        gpsSpeedMs: Float? = null,
        derivedSpeedMs: Float? = null,
        derivedAccelMs2: Float? = null,
        leanAngleDeg: Float? = null,
        lateralGz: Float? = null,
        wallClockMs: Long = 0L,
        elapsedRealtimeNs: Long = 0L,
    ) = DataPoint(
        id = 0,
        sessionId = "test",
        elapsedRealtimeNs = elapsedRealtimeNs,
        wallClockMs = wallClockMs,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        gpsSpeedMs = gpsSpeedMs,
        gpsAccuracyM = null,
        satellitesUsed = null,
        satellitesVisible = null,
        accelX = 0f,
        accelY = 0f,
        accelZ = 0f,
        accelMagnitude = 0f,
        derivedSpeedMs = derivedSpeedMs,
        derivedAccelMs2 = derivedAccelMs2,
        leanAngleDeg = leanAngleDeg,
        lateralGz = lateralGz,
    )

    // -------------------------------------------------------------------------
    // Empty list
    // -------------------------------------------------------------------------

    @Test
    fun emptyList_allZeros() {
        val stats = SessionStatsComputer.compute(emptyList())
        assertEquals(0f, stats.distanceM, 0f)
        assertEquals(0f, stats.avgSpeedMs, 0f)
        assertEquals(0f, stats.maxSpeedMs, 0f)
        assertEquals(0f, stats.maxLateralG, 0f)
        assertEquals(0f, stats.maxLeanDeg, 0f)
        assertEquals(0f, stats.hardBrakeG, 0f)
        assertEquals(0, stats.movingPercent)
    }

    // -------------------------------------------------------------------------
    // Distance
    // -------------------------------------------------------------------------

    @Test
    fun distance_twoPts_oneDegreeLatApart_approx111km() {
        // One degree of latitude ≈ 111 195 m at the equator.
        val points = listOf(
            dp(latitude = 0.0, longitude = 0.0),
            dp(latitude = 1.0, longitude = 0.0),
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(111_195f, stats.distanceM, 500f)
    }

    @Test
    fun distance_threePts_sumsSegments() {
        // Two consecutive 1-degree steps → ~222 390 m total.
        val points = listOf(
            dp(latitude = 0.0, longitude = 0.0),
            dp(latitude = 1.0, longitude = 0.0),
            dp(latitude = 2.0, longitude = 0.0),
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(222_390f, stats.distanceM, 1_000f)
    }

    @Test
    fun distance_pointsWithNullLatLon_skipped() {
        // Middle point has no coordinates; distance should still cover 1° lat.
        val points = listOf(
            dp(latitude = 0.0, longitude = 0.0),
            dp(latitude = null, longitude = null),   // no GPS fix
            dp(latitude = 1.0, longitude = 0.0),
        )
        val stats = SessionStatsComputer.compute(points)
        // Only the segment (0→2) has both endpoints with coords; it spans 1 degree.
        // The pair (0,null) and (null,1) are skipped, but the pair (null,1) is also skipped.
        // Actually pairs: (pt0,pt1): pt1 has null → skip; (pt1,pt2): pt1 has null → skip.
        // So distanceM == 0 when the middle point breaks the chain.
        assertEquals(0f, stats.distanceM, 0f)
    }

    @Test
    fun distance_allNullLatLon_zero() {
        val points = listOf(dp(), dp(), dp())
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0f, stats.distanceM, 0f)
    }

    // -------------------------------------------------------------------------
    // Speed
    // -------------------------------------------------------------------------

    @Test
    fun maxSpeed_picksHighestSpeedSource() {
        val points = listOf(
            dp(derivedSpeedMs = 10f),
            dp(derivedSpeedMs = 25f),
            dp(gpsSpeedMs = 5f),        // derivedSpeedMs null → falls back to gpsSpeedMs
            dp(),                        // both null → 0
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(25f, stats.maxSpeedMs, 0.01f)
    }

    @Test
    fun avgSpeed_excludesStationaryPoints() {
        // Moving points: 10 and 20 m/s → avg = 15 m/s
        // Stationary point: 0.3 m/s (below 0.5 threshold) → excluded
        val points = listOf(
            dp(derivedSpeedMs = 10f),
            dp(derivedSpeedMs = 0.3f),   // stationary
            dp(derivedSpeedMs = 20f),
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(15f, stats.avgSpeedMs, 0.01f)
    }

    @Test
    fun avgSpeed_allStationary_zero() {
        val points = listOf(dp(derivedSpeedMs = 0f), dp(derivedSpeedMs = 0.4f))
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0f, stats.avgSpeedMs, 0.01f)
    }

    @Test
    fun speedSource_derivedPreferredOverGps() {
        val points = listOf(
            dp(derivedSpeedMs = 30f, gpsSpeedMs = 10f),  // derived wins
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(30f, stats.maxSpeedMs, 0.01f)
    }

    @Test
    fun speedSource_fallbackToGpsWhenDerivedNull() {
        val points = listOf(
            dp(derivedSpeedMs = null, gpsSpeedMs = 12f),
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(12f, stats.maxSpeedMs, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Lateral G
    // -------------------------------------------------------------------------

    @Test
    fun maxLateralG_picksMaxAbsValue() {
        val points = listOf(
            dp(lateralGz = 0.3f),
            dp(lateralGz = -0.8f),   // magnitude 0.8 → should win
            dp(lateralGz = 0.5f),
            dp(),                     // null → ignored
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0.8f, stats.maxLateralG, 0.001f)
    }

    @Test
    fun maxLateralG_noReadings_zero() {
        val points = listOf(dp(), dp())
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0f, stats.maxLateralG, 0f)
    }

    // -------------------------------------------------------------------------
    // Lean angle
    // -------------------------------------------------------------------------

    @Test
    fun maxLeanDeg_picksMaxAbsValue() {
        val points = listOf(
            dp(leanAngleDeg = 15f),
            dp(leanAngleDeg = -40f),   // magnitude 40° → should win
            dp(leanAngleDeg = 20f),
            dp(),                       // null → ignored
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(40f, stats.maxLeanDeg, 0.001f)
    }

    @Test
    fun maxLeanDeg_noReadings_zero() {
        val points = listOf(dp(), dp())
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0f, stats.maxLeanDeg, 0f)
    }

    // -------------------------------------------------------------------------
    // Hard brake
    // -------------------------------------------------------------------------

    @Test
    fun hardBrakeG_mostNegativeAccel_convertedToG() {
        // -19.62 m/s² / 9.81 = -2.0 G
        val points = listOf(
            dp(derivedAccelMs2 = -9.81f),    // -1.0 G
            dp(derivedAccelMs2 = -19.62f),   // -2.0 G  ← hardest
            dp(derivedAccelMs2 = 5f),         // positive → not hardest brake
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(-2.0f, stats.hardBrakeG, 0.01f)
        assertTrue("hardBrakeG must be <= 0", stats.hardBrakeG <= 0f)
    }

    @Test
    fun hardBrakeG_onlyPositiveAccel_returnsZero() {
        val points = listOf(
            dp(derivedAccelMs2 = 5f),
            dp(derivedAccelMs2 = 10f),
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0f, stats.hardBrakeG, 0.001f)
    }

    @Test
    fun hardBrakeG_noAccelReadings_zero() {
        val points = listOf(dp(), dp())
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0f, stats.hardBrakeG, 0f)
    }

    // -------------------------------------------------------------------------
    // Moving percent
    // -------------------------------------------------------------------------

    @Test
    fun movingPercent_mixedPoints() {
        // 2 moving (10, 20 m/s) + 2 stationary (0, 0.4 m/s) → 2/4 = 50 %
        val points = listOf(
            dp(derivedSpeedMs = 10f),
            dp(derivedSpeedMs = 0f),
            dp(derivedSpeedMs = 20f),
            dp(derivedSpeedMs = 0.4f),
        )
        val stats = SessionStatsComputer.compute(points)
        assertEquals(50, stats.movingPercent)
    }

    @Test
    fun movingPercent_allMoving_100() {
        val points = listOf(dp(derivedSpeedMs = 5f), dp(derivedSpeedMs = 10f))
        val stats = SessionStatsComputer.compute(points)
        assertEquals(100, stats.movingPercent)
    }

    @Test
    fun movingPercent_allStationary_0() {
        val points = listOf(dp(derivedSpeedMs = 0f), dp(derivedSpeedMs = 0.1f))
        val stats = SessionStatsComputer.compute(points)
        assertEquals(0, stats.movingPercent)
    }

    // -------------------------------------------------------------------------
    // filterStationary = true
    // -------------------------------------------------------------------------

    @Test
    fun filterStationary_distanceSkipsStationaryGpsNoise() {
        // A stationary stop with GPS drift to a far-off position should not inflate distance.
        // Without filtering the big detour through lon=10 would dominate; with filtering
        // only the moving-to-moving haversine is computed.
        val points = listOf(
            dp(latitude = 0.0, longitude = 0.0, derivedSpeedMs = 10f),   // moving
            dp(latitude = 0.0, longitude = 10.0, derivedSpeedMs = 0f),   // stationary, drifted far
            dp(latitude = 1.0, longitude = 0.0, derivedSpeedMs = 10f),   // moving
        )
        val filtered = SessionStatsComputer.compute(points, filterStationary = true)
        val unfiltered = SessionStatsComputer.compute(points, filterStationary = false)

        // Filtered distance = pt0 → pt2 ≈ 111 195 m (1 degree lat at equator).
        assertEquals(111_195f, filtered.distanceM, 500f)
        // Unfiltered distance is much larger because of the lon=10 detour.
        assertTrue(
            "unfiltered distance should exceed filtered by >100 km",
            unfiltered.distanceM > filtered.distanceM + 100_000f,
        )
    }

    @Test
    fun filterStationary_movingPercentUsesTotalPointCount() {
        // 2 moving + 2 stationary; movingPercent must be 50 even when filterStationary is on,
        // because the denominator is always the full stored-point count.
        val points = listOf(
            dp(derivedSpeedMs = 10f),
            dp(derivedSpeedMs = 0f),
            dp(derivedSpeedMs = 20f),
            dp(derivedSpeedMs = 0.4f),
        )
        val stats = SessionStatsComputer.compute(points, filterStationary = true)
        assertEquals(50, stats.movingPercent)
    }

    @Test
    fun filterStationary_allStationaryPoints_returnsZeros() {
        // If every point is stationary and filtering is on, workingPoints is empty.
        // Should return all-zero stats without throwing.
        val points = listOf(
            dp(latitude = 0.0, longitude = 0.0, derivedSpeedMs = 0f),
            dp(latitude = 0.1, longitude = 0.0, derivedSpeedMs = 0.2f),
        )
        val stats = SessionStatsComputer.compute(points, filterStationary = true)
        assertEquals(0f, stats.distanceM, 0f)
        assertEquals(0f, stats.avgSpeedMs, 0f)
        assertEquals(0f, stats.maxSpeedMs, 0f)
        assertEquals(0, stats.movingPercent)
    }

    @Test
    fun filterStationary_false_behaviourUnchanged() {
        // Explicit false must produce the same result as the no-arg overload.
        val points = listOf(
            dp(latitude = 0.0, longitude = 0.0, derivedSpeedMs = 10f),
            dp(latitude = 1.0, longitude = 0.0, derivedSpeedMs = 20f),
        )
        val explicit = SessionStatsComputer.compute(points, filterStationary = false)
        val default  = SessionStatsComputer.compute(points)
        assertEquals(explicit.distanceM,    default.distanceM,    0f)
        assertEquals(explicit.maxSpeedMs,   default.maxSpeedMs,   0f)
        assertEquals(explicit.movingPercent, default.movingPercent)
    }
}
