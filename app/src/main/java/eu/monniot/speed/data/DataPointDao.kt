package eu.monniot.speed.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DataPointDao {
    @Insert
    suspend fun insertDataPoint(point: DataPoint)

    @Query("SELECT * FROM data_points WHERE sessionId = :sessionId ORDER BY elapsedRealtimeNs ASC")
    suspend fun getPointsForSession(sessionId: String): List<DataPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT sessionId, startTimeMs, endTimeMs, pointCount, maxSpeedMs, distanceM, avgSpeedMs, maxLateralG, maxLeanDeg FROM sessions ORDER BY startTimeMs DESC")
    fun getSessionSummaries(): Flow<List<SessionSummary>>

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): Session?

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM data_points WHERE sessionId = :sessionId")
    suspend fun deleteDataPointsForSession(sessionId: String)

    @Transaction
    suspend fun deleteFullSession(sessionId: String) {
        deleteDataPointsForSession(sessionId)
        deleteSession(sessionId)
    }
}
