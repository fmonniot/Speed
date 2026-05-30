package eu.monniot.speed.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "segment_attempts")
data class SegmentAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: String,
    val sessionId: String,
    val elapsedTimeMs: Long,
    val dateMs: Long,
    val maxSpeedMs: Float,
    val maxLateralG: Float,
    val maxLeanDeg: Float
)

/** Flat projection used to render a row in the segments list screen.
 *
 * - [runCount]: total number of attempts on this segment.
 * - [bestTimeMs]: minimum elapsedTimeMs across all attempts; null if no attempts.
 * - [lastTimeMs]: elapsedTimeMs of the most-recent attempt (by dateMs); null if no attempts.
 * - [previousTimeMs]: elapsedTimeMs of the second-most-recent attempt; null if < 2 attempts.
 *   Compare with [lastTimeMs] to show a trend indicator (faster / slower / equal).
 */
data class SegmentListItem(
    val segmentId: String,
    val name: String,
    val distanceM: Float,
    val isFavourite: Boolean,
    val isGoal: Boolean,
    val runCount: Int,
    val bestTimeMs: Long?,
    val lastTimeMs: Long?,
    val previousTimeMs: Long?
)
