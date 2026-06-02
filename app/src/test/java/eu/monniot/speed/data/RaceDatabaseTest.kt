package eu.monniot.speed.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RaceDatabaseTest {
    private lateinit var db: RaceDatabase
    private lateinit var dao: DataPointDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RaceDatabase::class.java).build()
        dao = db.dataPointDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeDataPointAndReadInList() = runBlocking {
        val sessionId = "test-session"
        val point = DataPoint(
            sessionId = sessionId,
            elapsedRealtimeNs = 1000L,
            wallClockMs = System.currentTimeMillis(),
            latitude = 0.0,
            longitude = 0.0,
            altitude = 0.0,
            gpsSpeedMs = 10.5f,
            gpsAccuracyM = 3.0f,
            satellitesUsed = 10,
            satellitesVisible = 12,
            accelX = 0f,
            accelY = 0f,
            accelZ = 0f,
            accelMagnitude = 1.0f,
            derivedSpeedMs = 10.6f,
            derivedAccelMs2 = 0.1f
        )
        dao.insertDataPoint(point)
        val points = dao.getPointsForSession(sessionId)
        assertEquals(1, points.size)
        assertEquals(10.5f, points[0].gpsSpeedMs)
    }

    @Test
    @Throws(Exception::class)
    fun deleteFullSessionCascades() = runBlocking {
        val sessionId = "session-to-delete"
        val session = Session(sessionId, System.currentTimeMillis(), null, 0, 0f)
        dao.insertSession(session)
        
        val point = DataPoint(
            sessionId = sessionId,
            elapsedRealtimeNs = 1000L,
            wallClockMs = System.currentTimeMillis(),
            latitude = null,
            longitude = null,
            altitude = null,
            gpsSpeedMs = null,
            gpsAccuracyM = null,
            satellitesUsed = null,
            satellitesVisible = null,
            accelX = 0f,
            accelY = 0f,
            accelZ = 0f,
            accelMagnitude = 0f,
            derivedSpeedMs = null,
            derivedAccelMs2 = null
        )
        dao.insertDataPoint(point)
        
        // Verify they exist
        assertEquals(1, dao.getPointsForSession(sessionId).size)
        assertEquals(sessionId, dao.getSession(sessionId)?.sessionId)
        
        // Delete
        dao.deleteFullSession(sessionId)
        
        // Verify they are gone
        assertEquals(0, dao.getPointsForSession(sessionId).size)
        assertNull(dao.getSession(sessionId))
    }
}
