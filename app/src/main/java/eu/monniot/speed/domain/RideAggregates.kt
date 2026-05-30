package eu.monniot.speed.domain

import eu.monniot.speed.data.SessionSummary
import kotlin.math.roundToInt

// E3: range-scoped aggregates for the Ride home (D1) and Stats overview (D6) screens.
//
// These are pure functions over the persisted per-session stats carried by [SessionSummary]
// (distance / max lateral G / max lean are written at finalize by SessionStatsComputer, E2).
// Computing in-memory over the summaries list — rather than SQL aggregates — keeps the logic pure
// and JVM-testable, and per-session distance isn't a raw column anyway. All values are SI; screens
// convert at the display boundary (C2).

/** A single record value and the session that holds it (null/0 when there is no data). */
data class RideRecord(val value: Float, val sessionId: String?)

/** One month's bucket for the Stats 6-month bar chart. */
data class MonthBucket(
    val startMs: Long,
    val endMsExclusive: Long,
    val label: String,
    val isCurrent: Boolean,
)

/** Per-month distance for the bar chart. */
data class MonthBar(val label: String, val distanceM: Float, val isCurrent: Boolean)

/** The four record cards + totals for a scoped Stats view. */
data class RangeStats(
    val totalDistanceM: Float,
    val rideCount: Int,
    val topSpeed: RideRecord,    // m/s
    val maxLean: RideRecord,     // degrees
    val maxLateralG: RideRecord, // G
    val longestRide: RideRecord, // meters
)

object RideAggregates {

    /** Lifetime cumulative distance across all sessions (meters). */
    fun lifetimeDistanceM(summaries: List<SessionSummary>): Float =
        summaries.sumOf { (it.distanceM ?: 0f).toDouble() }.toFloat()

    /** Number of rides whose start falls in [sinceMs, untilMsExclusive). */
    fun countInRange(summaries: List<SessionSummary>, sinceMs: Long, untilMsExclusive: Long): Int =
        summaries.count { it.startTimeMs in sinceMs until untilMsExclusive }

    /** Total distance (meters) of rides whose start falls in [sinceMs, untilMsExclusive). */
    fun distanceInRange(summaries: List<SessionSummary>, sinceMs: Long, untilMsExclusive: Long): Float =
        summaries.filter { it.startTimeMs in sinceMs until untilMsExclusive }
            .sumOf { (it.distanceM ?: 0f).toDouble() }.toFloat()

    /** Scoped totals + records over [sinceMs, untilMsExclusive). Records carry the holding sessionId. */
    fun rangeStats(summaries: List<SessionSummary>, sinceMs: Long, untilMsExclusive: Long): RangeStats {
        val inRange = summaries.filter { it.startTimeMs in sinceMs until untilMsExclusive }
        fun recordBy(selector: (SessionSummary) -> Float?): RideRecord {
            val best = inRange.maxByOrNull { selector(it) ?: Float.NEGATIVE_INFINITY }
            val v = best?.let(selector)
            return if (best != null && v != null) RideRecord(v, best.sessionId) else RideRecord(0f, null)
        }
        return RangeStats(
            totalDistanceM = inRange.sumOf { (it.distanceM ?: 0f).toDouble() }.toFloat(),
            rideCount = inRange.size,
            topSpeed = recordBy { it.maxSpeedMs },
            maxLean = recordBy { it.maxLeanDeg },
            maxLateralG = recordBy { it.maxLateralG },
            longestRide = recordBy { it.distanceM },
        )
    }

    /**
     * Percent change of [current] vs [previous] comparable period, rounded.
     * Null when [previous] is 0 (no baseline → no meaningful trend).
     */
    fun trendPercent(current: Float, previous: Float): Int? =
        if (previous == 0f) null else (100f * (current - previous) / previous).roundToInt()

    /** Per-month distance over the provided buckets (typically the trailing 6 months). */
    fun monthlyDistance(summaries: List<SessionSummary>, buckets: List<MonthBucket>): List<MonthBar> =
        buckets.map { b ->
            MonthBar(
                label = b.label,
                distanceM = distanceInRange(summaries, b.startMs, b.endMsExclusive),
                isCurrent = b.isCurrent,
            )
        }
}
