package eu.monniot.speed.domain

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Segment
import eu.monniot.speed.data.encodePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentMatcherTest {

    // -------------------------------------------------------------------------
    // Helper: DataPoint factory with sensible defaults
    // -------------------------------------------------------------------------

    /**
     * Constructs a [DataPoint] with all nullable sensor fields defaulting to null and required
     * float fields defaulting to 0f. Callers only supply the fields relevant to each test case.
     */
    private fun dp(
        latitude: Double? = null,
        longitude: Double? = null,
        wallClockMs: Long = 0L,
        derivedSpeedMs: Float? = null,
        gpsSpeedMs: Float? = null,
        leanAngleDeg: Float? = null,
        lateralGz: Float? = null,
    ) = DataPoint(
        id = 0,
        sessionId = "test",
        elapsedRealtimeNs = 0L,
        wallClockMs = wallClockMs,
        latitude = latitude,
        longitude = longitude,
        altitude = null,
        gpsSpeedMs = gpsSpeedMs,
        gpsAccuracyM = null,
        satellitesUsed = null,
        satellitesVisible = null,
        accelX = 0f,
        accelY = 0f,
        accelZ = 0f,
        accelMagnitude = 0f,
        derivedSpeedMs = derivedSpeedMs,
        derivedAccelMs2 = null,
        leanAngleDeg = leanAngleDeg,
        lateralGz = lateralGz,
    )

    // -------------------------------------------------------------------------
    // Track fixture
    //
    // A straight north-marching track: latitude increases 0.001° per step (≈ 111 m each step),
    // longitude stays fixed at 10.0°. Timestamps are 1 000 ms apart.
    //
    //   index  lat        lon    wallClockMs
    //     0    48.0000    10.0    0
    //     1    48.0010    10.0    1 000
    //     2    48.0020    10.0    2 000
    //     3    48.0030    10.0    3 000
    //     4    48.0040    10.0    4 000
    //     5    48.0050    10.0    5 000
    //     6    48.0060    10.0    6 000
    //     7    48.0070    10.0    7 000
    //     8    48.0080    10.0    8 000
    //     9    48.0090    10.0    9 000
    // -------------------------------------------------------------------------

    private val BASE_LAT = 48.0
    private val BASE_LON = 10.0
    private val STEP_DEG = 0.001   // ≈ 111 m per step
    private val STEP_MS = 1_000L

    /** Build a straight north-marching track with [n] points. */
    private fun buildTrack(n: Int, speedMs: Float = 20f): List<DataPoint> =
        (0 until n).map { i ->
            dp(
                latitude = BASE_LAT + i * STEP_DEG,
                longitude = BASE_LON,
                wallClockMs = i * STEP_MS,
                derivedSpeedMs = speedMs,
            )
        }

    /** Build a [Segment] whose start is at track index [startIdx] and end is at [endIdx]. */
    private fun buildSegment(
        startIdx: Int,
        endIdx: Int,
        id: String = "seg-$startIdx-$endIdx",
    ): Segment {
        val startLat = BASE_LAT + startIdx * STEP_DEG
        val endLat = BASE_LAT + endIdx * STEP_DEG
        val polyline = encodePath(
            listOf(
                startLat to BASE_LON,
                endLat to BASE_LON,
            )
        )
        val distanceM = ((endIdx - startIdx) * STEP_DEG * 111_195).toFloat()
        return Segment(
            segmentId = id,
            name = "Test Segment $id",
            distanceM = distanceM,
            pathPolyline = polyline,
            createdAtMs = 0L,
        )
    }

    // -------------------------------------------------------------------------
    // Empty input
    // -------------------------------------------------------------------------

    @Test
    fun emptyPoints_returnsEmpty() {
        val segment = buildSegment(startIdx = 2, endIdx = 7)
        val result = SegmentMatcher.match(emptyList(), listOf(segment))
        assertTrue("Expected no results for empty track", result.isEmpty())
    }

    @Test
    fun emptySegments_returnsEmpty() {
        val track = buildTrack(10)
        val result = SegmentMatcher.match(track, emptyList())
        assertTrue("Expected no results for empty segment list", result.isEmpty())
    }

    @Test
    fun bothEmpty_returnsEmpty() {
        val result = SegmentMatcher.match(emptyList(), emptyList())
        assertTrue("Expected no results when both inputs are empty", result.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Happy path: segment on track
    // -------------------------------------------------------------------------

    @Test
    fun segmentOnTrack_returnsOneResult_withCorrectElapsedTime() {
        // The segment spans track indices 2 → 7, so the elapsed time should be
        // (7 - 2) * 1 000 ms = 5 000 ms.
        val track = buildTrack(10, speedMs = 30f)
        val segment = buildSegment(startIdx = 2, endIdx = 7)

        val results = SegmentMatcher.match(track, listOf(segment))

        assertEquals("Expected exactly one matched segment", 1, results.size)
        val r = results[0]
        assertEquals("segmentId mismatch", segment.segmentId, r.segmentId)
        assertEquals("elapsedTimeMs should be 5 000", 5_000L, r.elapsedTimeMs)
    }

    @Test
    fun segmentOnTrack_maxSpeedReflectsSubTrack() {
        // Build a track where the sub-track [2..7] has a faster point in the middle.
        val base = buildTrack(10, speedMs = 10f).toMutableList()
        // Replace index 4 with a faster point (still within the segment).
        base[4] = base[4].copy(derivedSpeedMs = 55f)
        val segment = buildSegment(startIdx = 2, endIdx = 7)

        val results = SegmentMatcher.match(base, listOf(segment))

        assertEquals(1, results.size)
        assertEquals("maxSpeedMs should pick the fastest point in sub-track", 55f, results[0].maxSpeedMs, 0.01f)
    }

    @Test
    fun segmentOnTrack_maxSpeedFallsBackToGpsSpeed() {
        // Build track with null derivedSpeedMs but populated gpsSpeedMs.
        val track = (0 until 10).map { i ->
            dp(
                latitude = BASE_LAT + i * STEP_DEG,
                longitude = BASE_LON,
                wallClockMs = i * STEP_MS,
                derivedSpeedMs = null,
                gpsSpeedMs = if (i == 5) 42f else 10f,
            )
        }
        val segment = buildSegment(startIdx = 2, endIdx = 7)
        val results = SegmentMatcher.match(track, listOf(segment))

        assertEquals(1, results.size)
        assertEquals("Should fall back to gpsSpeedMs", 42f, results[0].maxSpeedMs, 0.01f)
    }

    @Test
    fun segmentOnTrack_maxLateralG_andLeanDeg_fromSubTrack() {
        val track = (0 until 10).map { i ->
            dp(
                latitude = BASE_LAT + i * STEP_DEG,
                longitude = BASE_LON,
                wallClockMs = i * STEP_MS,
                derivedSpeedMs = 20f,
                lateralGz = if (i == 4) -0.9f else 0.1f,
                leanAngleDeg = if (i == 5) 35f else 5f,
            )
        }
        val segment = buildSegment(startIdx = 2, endIdx = 7)
        val results = SegmentMatcher.match(track, listOf(segment))

        assertEquals(1, results.size)
        assertEquals("maxLateralG should use |lateralGz|", 0.9f, results[0].maxLateralG, 0.001f)
        assertEquals("maxLeanDeg should pick highest |leanAngleDeg|", 35f, results[0].maxLeanDeg, 0.001f)
    }

    // -------------------------------------------------------------------------
    // Far-away segment: no match
    // -------------------------------------------------------------------------

    @Test
    fun segmentFarFromTrack_noResult() {
        // Place the segment endpoints 1° away in longitude — well beyond 25 m threshold.
        val track = buildTrack(10)
        val polyline = encodePath(
            listOf(
                (BASE_LAT + 2 * STEP_DEG) to (BASE_LON + 1.0),   // ~80 km away in longitude
                (BASE_LAT + 7 * STEP_DEG) to (BASE_LON + 1.0),
            )
        )
        val segment = Segment(
            segmentId = "far-segment",
            name = "Far Away",
            distanceM = 1000f,
            pathPolyline = polyline,
            createdAtMs = 0L,
        )

        val results = SegmentMatcher.match(track, listOf(segment))
        assertTrue("Expected no results for segment far from track", results.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Reversed segment: rider went end → start, not start → end
    // -------------------------------------------------------------------------

    @Test
    fun segmentReversed_noResult() {
        // The segment's "start" is at track index 8 and "end" is at index 2.
        // Since the rider travels northward (index 0 → 9), the start is encountered AFTER the end
        // in riding order, which should be rejected (iEnd ≤ iStart after search from iStart+1).
        val track = buildTrack(10)
        val segment = buildSegment(startIdx = 8, endIdx = 2, id = "reversed")

        val results = SegmentMatcher.match(track, listOf(segment))
        assertTrue("Expected no result for reversed segment", results.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Multiple segments: only crossing ones returned
    // -------------------------------------------------------------------------

    @Test
    fun multipleSegments_onlyMatchingOnesReturned() {
        val track = buildTrack(10)

        val onTrack = buildSegment(startIdx = 1, endIdx = 6, id = "on-track")
        // Offset 2° in latitude — both endpoints are far from the track.
        val offTrackPolyline = encodePath(
            listOf(
                (BASE_LAT + 2.0) to BASE_LON,
                (BASE_LAT + 2.005) to BASE_LON,
            )
        )
        val offTrack = Segment(
            segmentId = "off-track",
            name = "Off Track",
            distanceM = 500f,
            pathPolyline = offTrackPolyline,
            createdAtMs = 0L,
        )

        val results = SegmentMatcher.match(track, listOf(onTrack, offTrack))

        assertEquals("Only the on-track segment should be matched", 1, results.size)
        assertEquals("on-track", results[0].segmentId)
    }

    // -------------------------------------------------------------------------
    // Track points with null GPS coordinates are filtered out
    // -------------------------------------------------------------------------

    @Test
    fun nullGpsPoints_filtered_segmentStillMatched() {
        // Insert null-GPS points between the genuine track points; matcher should ignore them.
        val track = buildTrack(10).flatMap { point ->
            listOf(point, dp(latitude = null, longitude = null, wallClockMs = point.wallClockMs + 50L))
        }
        val segment = buildSegment(startIdx = 2, endIdx = 7)
        val results = SegmentMatcher.match(track, listOf(segment))

        assertEquals(1, results.size)
        assertEquals(5_000L, results[0].elapsedTimeMs)
    }

    @Test
    fun allNullGpsPoints_returnsEmpty() {
        val track = listOf(
            dp(latitude = null, longitude = null, wallClockMs = 0L),
            dp(latitude = null, longitude = null, wallClockMs = 1_000L),
        )
        val segment = buildSegment(startIdx = 0, endIdx = 1)
        val results = SegmentMatcher.match(track, listOf(segment))
        assertTrue("Expected no results when all GPS points are null", results.isEmpty())
    }
}
