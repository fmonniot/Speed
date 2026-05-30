package eu.monniot.speed.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@Dao
interface SegmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSegment(segment: Segment)

    @Update
    suspend fun updateSegment(segment: Segment)

    @Query("DELETE FROM segments WHERE segmentId = :id")
    suspend fun deleteSegment(id: String)

    @Query("DELETE FROM segment_attempts WHERE segmentId = :id")
    suspend fun deleteAttemptsForSegment(id: String)

    @Insert
    suspend fun insertAttempt(attempt: SegmentAttempt)

    @Query("SELECT * FROM segments ORDER BY name ASC")
    fun getSegments(): Flow<List<Segment>>

    @Query("SELECT * FROM segments WHERE segmentId = :id")
    suspend fun getSegment(id: String): Segment?

    @Query("SELECT * FROM segment_attempts WHERE segmentId = :id ORDER BY dateMs DESC")
    fun getAttemptsForSegment(id: String): Flow<List<SegmentAttempt>>

    // ---------------------------------------------------------------------------
    // Helpers used by getSegmentListItems()
    // ---------------------------------------------------------------------------

    /** All segments, name-sorted. */
    @Query("SELECT * FROM segments ORDER BY name ASC")
    suspend fun _getSegmentsOnce(): List<Segment>

    /** For each segment: total run count and best (minimum) elapsed time. */
    @Query(
        """
        SELECT segmentId,
               COUNT(*)        AS runCount,
               MIN(elapsedTimeMs) AS bestTimeMs
        FROM segment_attempts
        GROUP BY segmentId
        """
    )
    suspend fun _getAttemptAggregates(): List<AttemptAggregate>

    /** All attempts ordered newest-first so we can pick rank-1 and rank-2. */
    @Query("SELECT segmentId, elapsedTimeMs, dateMs FROM segment_attempts ORDER BY dateMs DESC")
    suspend fun _getAttemptsChronological(): List<AttemptThin>

    // ---------------------------------------------------------------------------
    // Composite list-item query
    // ---------------------------------------------------------------------------

    /**
     * Returns one [SegmentListItem] per segment, with aggregated stats.
     *
     * lastTimeMs / previousTimeMs are derived by taking the two most-recent
     * attempts for each segment (by dateMs DESC) from a flat list sorted that
     * way.  The first occurrence for a segmentId is "last", the second is
     * "previous".  This avoids complex window-function SQL that older SQLite
     * versions (bundled with Android) may not support.
     */
    @Transaction
    suspend fun getSegmentListItemsOnce(): List<SegmentListItem> {
        val segments = _getSegmentsOnce()
        val aggregates = _getAttemptAggregates().associateBy { it.segmentId }
        // Walk the newest-first list; first hit per segment = last, second = previous.
        val lastTime = mutableMapOf<String, Long>()
        val prevTime = mutableMapOf<String, Long>()
        for (row in _getAttemptsChronological()) {
            when {
                row.segmentId !in lastTime -> lastTime[row.segmentId] = row.elapsedTimeMs
                row.segmentId !in prevTime -> prevTime[row.segmentId] = row.elapsedTimeMs
            }
        }
        return segments.map { seg ->
            val agg = aggregates[seg.segmentId]
            SegmentListItem(
                segmentId     = seg.segmentId,
                name          = seg.name,
                distanceM     = seg.distanceM,
                isFavourite   = seg.isFavourite,
                isGoal        = seg.isGoal,
                runCount      = agg?.runCount ?: 0,
                bestTimeMs    = agg?.bestTimeMs,
                lastTimeMs    = lastTime[seg.segmentId],
                previousTimeMs = prevTime[seg.segmentId]
            )
        }
    }

    /**
     * Observable version: re-emits whenever segments or attempts change.
     * Uses [combine] to watch both tables, then re-runs the suspend aggregation
     * inside a [flow] builder so the suspend call is legal.
     */
    fun getSegmentListItems(): Flow<List<SegmentListItem>> =
        combine(
            getSegments(),
            getAttemptsForAllSegments()
        ) { segments, _ -> segments }   // any change triggers a new emission
            .flatMapLatest { _ ->
                flow { emit(getSegmentListItemsOnce()) }
            }

    /** Used only to trigger re-emission in [getSegmentListItems]. */
    @Query("SELECT * FROM segment_attempts")
    fun getAttemptsForAllSegments(): Flow<List<SegmentAttempt>>
}

// ---------------------------------------------------------------------------
// Internal result holders (package-private by Kotlin default)
// ---------------------------------------------------------------------------

data class AttemptAggregate(
    val segmentId: String,
    val runCount: Int,
    val bestTimeMs: Long?
)

data class AttemptThin(
    val segmentId: String,
    val elapsedTimeMs: Long,
    val dateMs: Long
)
