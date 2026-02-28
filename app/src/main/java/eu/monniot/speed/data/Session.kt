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
    val notes: String = ""
)

data class SessionSummary(
    val sessionId: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val pointCount: Int,
    val maxSpeedMs: Float?
)
