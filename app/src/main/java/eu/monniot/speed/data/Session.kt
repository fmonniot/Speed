package eu.monniot.speed.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val sessionId: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val pointCount: Int,
    val maxSpeedMs: Float?,
    val notes: String = "",
    // E3: per-session aggregates computed by SessionStatsComputer at finalize (null until finalized).
    val distanceM: Float? = null,
    val avgSpeedMs: Float? = null,
    val maxLateralG: Float? = null,
    val maxLeanDeg: Float? = null,
    val hardBrakeG: Float? = null,
    val movingPercent: Int? = null
)

// Projection for lists/aggregates. Carries the persisted per-session stats so screens (Trips/Stats/
// Home) and the in-memory aggregates can read distance / max-lateral-G / lean without loading points.
data class SessionSummary(
    val sessionId: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val pointCount: Int,
    val maxSpeedMs: Float?,
    val distanceM: Float? = null,
    val avgSpeedMs: Float? = null,
    val maxLateralG: Float? = null,
    val maxLeanDeg: Float? = null
)
