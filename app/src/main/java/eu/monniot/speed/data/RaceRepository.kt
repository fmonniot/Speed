package eu.monniot.speed.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class RaceRepository(private val dao: DataPointDao) {

    val sessionSummaries: Flow<List<SessionSummary>> = dao.getSessionSummaries()

    suspend fun insertDataPoint(point: DataPoint) = dao.insertDataPoint(point)

    suspend fun startSession(session: Session) = dao.insertSession(session)

    suspend fun updateSession(session: Session) = dao.updateSession(session)

    suspend fun getSession(sessionId: String) = dao.getSession(sessionId)

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
