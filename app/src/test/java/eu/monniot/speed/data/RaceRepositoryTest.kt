package eu.monniot.speed.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Repository-level integration test against an in-memory Room DB (Robolectric, JVM). Covers the
 * data-layer wiring the ViewModel delegates to — session summaries and the CSV export path, which
 * the GPX/FIT exporter unit tests don't touch.
 */
@RunWith(AndroidJUnit4::class)
class RaceRepositoryTest {
    private lateinit var db: RaceDatabase
    private lateinit var repository: RaceRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RaceDatabase::class.java).build()
        repository = RaceRepository(db.dataPointDao(), db.segmentDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun point(sessionId: String, elapsedNs: Long, speed: Float) = DataPoint(
        sessionId = sessionId,
        elapsedRealtimeNs = elapsedNs,
        wallClockMs = 1_700_000_000_000L,
        latitude = 45.0,
        longitude = 7.0,
        altitude = 100.0,
        gpsSpeedMs = speed,
        gpsAccuracyM = 3.0f,
        satellitesUsed = 10,
        satellitesVisible = 12,
        accelX = 0f,
        accelY = 0f,
        accelZ = 9.8f,
        accelMagnitude = 9.8f,
        derivedSpeedMs = speed,
        derivedAccelMs2 = 0.2f,
    )

    @Test
    fun sessionSummaries_reflectInsertedSessions() = runBlocking {
        repository.startSession(Session("s1", startTimeMs = 1_000, endTimeMs = 2_000, pointCount = 2, maxSpeedMs = 12f))
        repository.startSession(Session("s2", startTimeMs = 3_000, endTimeMs = 4_000, pointCount = 5, maxSpeedMs = 20f))

        val summaries = repository.sessionSummaries.first()
        assertEquals(setOf("s1", "s2"), summaries.map { it.sessionId }.toSet())
    }

    @Test
    fun exportToCsv_writesHeaderAndElapsedRelativeToSessionStart() = runBlocking {
        val sessionId = "export-session"
        repository.startSession(Session(sessionId, startTimeMs = 0, endTimeMs = null, pointCount = 2, maxSpeedMs = null))
        val startNs = 1_000_000_000L // 1s of elapsed-realtime baseline
        repository.insertDataPoint(point(sessionId, elapsedNs = startNs, speed = 10f))
        repository.insertDataPoint(point(sessionId, elapsedNs = startNs + 500_000_000L, speed = 11f)) // +500ms

        val out = ByteArrayOutputStream()
        repository.exportToCsv(sessionId, out, startNs)
        val lines = out.toString("UTF-8").trim().lines()

        assertEquals(3, lines.size) // header + 2 rows
        assertTrue("header should list columns", lines[0].startsWith("elapsed_ms,wall_clock_iso,lat,lon"))
        // First row's elapsed_ms is relative to startNs → 0; second is +500ms.
        assertEquals("0", lines[1].substringBefore(","))
        assertEquals("500", lines[2].substringBefore(","))
    }
}
