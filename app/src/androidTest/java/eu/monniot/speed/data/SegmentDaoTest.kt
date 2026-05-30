package eu.monniot.speed.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SegmentDaoTest {
    private lateinit var db: RaceDatabase
    private lateinit var dao: SegmentDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RaceDatabase::class.java).build()
        dao = db.segmentDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun segment(id: String, name: String) = Segment(
        segmentId = id,
        name = name,
        distanceM = 2840f,
        pathPolyline = encodePath(listOf(45.0 to 7.0, 45.01 to 7.01)),
        isFavourite = false,
        isGoal = false,
        createdAtMs = System.currentTimeMillis(),
    )

    private fun attempt(segmentId: String, elapsedMs: Long, dateMs: Long) = SegmentAttempt(
        segmentId = segmentId,
        sessionId = "session-$dateMs",
        elapsedTimeMs = elapsedMs,
        dateMs = dateMs,
        maxSpeedMs = 39.4f,
        maxLateralG = 0.91f,
        maxLeanDeg = 38f,
    )

    @Test
    fun upsertAndReadSegment() = runBlocking {
        dao.upsertSegment(segment("s1", "Ascent, west"))
        assertEquals("Ascent, west", dao.getSegment("s1")?.name)
        assertEquals(1, dao.getSegments().first().size)
    }

    @Test
    fun polylineRoundTrips() {
        val pts = listOf(45.0 to 7.0, 45.5 to 7.5)
        val decoded = decodePath(encodePath(pts))
        assertEquals(2, decoded.size)
        assertEquals(45.0, decoded[0].first, 1e-6)
        assertEquals(7.5, decoded[1].second, 1e-6)
    }

    @Test
    fun listItemAggregatesBestRunCountAndTrend() = runBlocking {
        dao.upsertSegment(segment("s1", "Ascent, west"))
        // Three attempts, increasing date; times: 140s, 138s (best), 141s (latest, slower than prev).
        dao.insertAttempt(attempt("s1", 140_000, dateMs = 1_000))
        dao.insertAttempt(attempt("s1", 138_000, dateMs = 2_000))
        dao.insertAttempt(attempt("s1", 141_000, dateMs = 3_000))

        val item = dao.getSegmentListItems().first().single()
        assertEquals(3, item.runCount)
        assertEquals(138_000L, item.bestTimeMs)
        assertEquals(141_000L, item.lastTimeMs)      // newest by dateMs
        assertEquals(138_000L, item.previousTimeMs)  // second-newest by dateMs
        assertTrue((item.lastTimeMs ?: 0) > (item.previousTimeMs ?: 0)) // slower → positive trend
    }

    @Test
    fun deleteSegmentRemovesAttempts() = runBlocking {
        dao.upsertSegment(segment("s1", "Ascent, west"))
        dao.insertAttempt(attempt("s1", 140_000, dateMs = 1_000))
        // Mirror repository cascade ordering.
        dao.deleteAttemptsForSegment("s1")
        dao.deleteSegment("s1")
        assertNull(dao.getSegment("s1"))
        assertEquals(0, dao.getAttemptsForSegment("s1").first().size)
    }
}
