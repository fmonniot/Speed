package eu.monniot.speed.domain

import eu.monniot.speed.data.SessionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideAggregatesTest {

    private fun summary(
        id: String,
        startMs: Long,
        distanceM: Float? = null,
        maxSpeedMs: Float? = null,
        maxLeanDeg: Float? = null,
        maxLateralG: Float? = null,
    ) = SessionSummary(
        sessionId = id,
        startTimeMs = startMs,
        endTimeMs = startMs + 1000,
        pointCount = 10,
        maxSpeedMs = maxSpeedMs,
        distanceM = distanceM,
        avgSpeedMs = null,
        maxLateralG = maxLateralG,
        maxLeanDeg = maxLeanDeg,
    )

    private val sessions = listOf(
        summary("a", 1_000, distanceM = 10_000f, maxSpeedMs = 30f, maxLeanDeg = 40f, maxLateralG = 0.8f),
        summary("b", 5_000, distanceM = 20_000f, maxSpeedMs = 50f, maxLeanDeg = 52f, maxLateralG = 1.2f),
        summary("c", 9_000, distanceM = 5_000f, maxSpeedMs = 45f, maxLeanDeg = 30f, maxLateralG = 0.5f),
    )

    @Test
    fun lifetimeDistanceSumsAll() {
        assertEquals(35_000f, RideAggregates.lifetimeDistanceM(sessions), 0.01f)
    }

    @Test
    fun countAndDistanceInRangeAreHalfOpen() {
        // [1_000, 9_000) includes a and b, excludes c (start == 9_000).
        assertEquals(2, RideAggregates.countInRange(sessions, 1_000, 9_000))
        assertEquals(30_000f, RideAggregates.distanceInRange(sessions, 1_000, 9_000), 0.01f)
    }

    @Test
    fun rangeStatsPicksRecordsWithHoldingSession() {
        val r = RideAggregates.rangeStats(sessions, 0, Long.MAX_VALUE)
        assertEquals(35_000f, r.totalDistanceM, 0.01f)
        assertEquals(3, r.rideCount)
        assertEquals(50f, r.topSpeed.value, 0.01f); assertEquals("b", r.topSpeed.sessionId)
        assertEquals(52f, r.maxLean.value, 0.01f); assertEquals("b", r.maxLean.sessionId)
        assertEquals(1.2f, r.maxLateralG.value, 0.01f); assertEquals("b", r.maxLateralG.sessionId)
        assertEquals(20_000f, r.longestRide.value, 0.01f); assertEquals("b", r.longestRide.sessionId)
    }

    @Test
    fun rangeStatsEmptyGivesZeroRecords() {
        val r = RideAggregates.rangeStats(sessions, 100_000, 200_000)
        assertEquals(0, r.rideCount)
        assertEquals(0f, r.topSpeed.value, 0.01f)
        assertNull(r.topSpeed.sessionId)
    }

    @Test
    fun trendPercentHandlesZeroBaseline() {
        assertEquals(50, RideAggregates.trendPercent(15f, 10f))
        assertEquals(-20, RideAggregates.trendPercent(8f, 10f))
        assertNull(RideAggregates.trendPercent(10f, 0f))
    }

    @Test
    fun monthlyDistanceBucketsByStart() {
        val buckets = listOf(
            MonthBucket(0, 5_000, "Jan", isCurrent = false),
            MonthBucket(5_000, 10_000, "Feb", isCurrent = true),
        )
        val bars = RideAggregates.monthlyDistance(sessions, buckets)
        assertEquals(2, bars.size)
        assertEquals(10_000f, bars[0].distanceM, 0.01f)   // a
        assertEquals(25_000f, bars[1].distanceM, 0.01f)   // b + c
        assertEquals(true, bars[1].isCurrent)
    }
}
