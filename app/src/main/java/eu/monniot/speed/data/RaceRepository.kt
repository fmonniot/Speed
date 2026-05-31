package eu.monniot.speed.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class RaceRepository(
    private val dao: DataPointDao,
    private val segmentDao: SegmentDao,
) {

    val sessionSummaries: Flow<List<SessionSummary>> = dao.getSessionSummaries()

    // ---- Segments (E4) ----
    val segments: Flow<List<Segment>> = segmentDao.getSegments()
    val segmentListItems: Flow<List<SegmentListItem>> = segmentDao.getSegmentListItems()

    suspend fun upsertSegment(segment: Segment) = segmentDao.upsertSegment(segment)
    suspend fun updateSegment(segment: Segment) = segmentDao.updateSegment(segment)
    suspend fun getSegment(id: String) = segmentDao.getSegment(id)
    fun getAttemptsForSegment(id: String) = segmentDao.getAttemptsForSegment(id)
    suspend fun insertAttempt(attempt: SegmentAttempt) = segmentDao.insertAttempt(attempt)
    suspend fun getSegmentsForMatching(): List<Segment> = segmentDao._getSegmentsOnce()

    /** Count of segments where this session's attempt set the personal best (D3 segment-PB row). */
    suspend fun getPbCountForSession(sessionId: String): Int {
        val attempts = segmentDao.getAttemptsForSession(sessionId)
        return attempts.count { it.elapsedTimeMs == segmentDao.getBestTimeForSegment(it.segmentId) }
    }
    suspend fun deleteSegment(id: String) {
        segmentDao.deleteAttemptsForSegment(id)
        segmentDao.deleteSegment(id)
    }

    suspend fun insertDataPoint(point: DataPoint) = dao.insertDataPoint(point)

    suspend fun startSession(session: Session) = dao.insertSession(session)

    suspend fun updateSession(session: Session) = dao.updateSession(session)

    suspend fun getSession(sessionId: String) = dao.getSession(sessionId)

    fun observeSession(sessionId: String): Flow<Session?> = dao.observeSession(sessionId)

    suspend fun deleteSession(sessionId: String) = dao.deleteFullSession(sessionId)

    suspend fun getPointsForSession(sessionId: String) = dao.getPointsForSession(sessionId)

    suspend fun exportToCsv(sessionId: String, outputStream: OutputStream, sessionStartElapsedNs: Long) = withContext(Dispatchers.IO) {
        val points = dao.getPointsForSession(sessionId)
        val writer = outputStream.bufferedWriter()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

        writer.write("elapsed_ms,wall_clock_iso,lat,lon,altitude_m,gps_speed_ms,gps_accuracy_m,accel_x,accel_y,accel_z,accel_magnitude,derived_speed_ms,derived_accel_ms2\n")
        
        points.forEach { p ->
            val elapsedMs = (p.elapsedRealtimeNs - sessionStartElapsedNs) / 1_000_000
            val wallClockIso = isoFormat.format(Date(p.wallClockMs))
            writer.write("${elapsedMs},${wallClockIso},${p.latitude},${p.longitude},${p.altitude},${p.gpsSpeedMs},${p.gpsAccuracyM},${p.accelX},${p.accelY},${p.accelZ},${p.accelMagnitude},${p.derivedSpeedMs},${p.derivedAccelMs2}\n")
        }
        writer.flush()
    }
}
