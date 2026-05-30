package eu.monniot.speed.export

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class GpxExporterTest {

    private val session = Session(
        sessionId = "test-session-1",
        startTimeMs = 1_700_000_000_000L, // 2023-11-14T22:13:20Z
        endTimeMs = 1_700_000_060_000L,
        pointCount = 3,
        maxSpeedMs = 15.0f,
        notes = ""
    )

    private fun makePoint(
        id: Long,
        lat: Double?,
        lon: Double?,
        alt: Double? = null,
        wallClockMs: Long = 1_700_000_001_000L
    ) = DataPoint(
        id = id,
        sessionId = session.sessionId,
        elapsedRealtimeNs = id * 1_000_000_000L,
        wallClockMs = wallClockMs,
        latitude = lat,
        longitude = lon,
        altitude = alt,
        gpsSpeedMs = null,
        gpsAccuracyM = null,
        satellitesUsed = null,
        satellitesVisible = null,
        accelX = 0f,
        accelY = 0f,
        accelZ = 9.81f,
        accelMagnitude = 9.81f,
        derivedSpeedMs = null,
        derivedAccelMs2 = null
    )

    @Test
    fun `output starts with gpx root element`() = runTest {
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, emptyList(), bos)
        val xml = bos.toString("UTF-8")

        assertTrue("Should contain <gpx", xml.contains("<gpx"))
        assertTrue("Should contain GPX 1.1 version attribute", xml.contains("version=\"1.1\""))
        assertTrue("Should contain GPX namespace", xml.contains("http://www.topografix.com/GPX/1/1"))
    }

    @Test
    fun `points with lat and lon are written as trkpt`() = runTest {
        val points = listOf(
            makePoint(1, lat = 48.8566, lon = 2.3522, alt = 35.0),
            makePoint(2, lat = 48.8600, lon = 2.3550)
        )
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, points, bos)
        val xml = bos.toString("UTF-8")

        val trkptCount = xml.split("<trkpt").size - 1
        assertEquals("Expected 2 trkpt elements", 2, trkptCount)
    }

    @Test
    fun `points missing lat or lon are skipped`() = runTest {
        val points = listOf(
            makePoint(1, lat = 48.8566, lon = 2.3522),  // valid
            makePoint(2, lat = null,    lon = 2.3550),  // missing lat — skip
            makePoint(3, lat = 48.8600, lon = null),    // missing lon — skip
            makePoint(4, lat = null,    lon = null),    // both missing — skip
            makePoint(5, lat = 48.8700, lon = 2.3600)   // valid
        )
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, points, bos)
        val xml = bos.toString("UTF-8")

        val trkptCount = xml.split("<trkpt").size - 1
        assertEquals("Only 2 valid points should appear", 2, trkptCount)
    }

    @Test
    fun `ele element is written when altitude is present`() = runTest {
        val points = listOf(
            makePoint(1, lat = 48.8566, lon = 2.3522, alt = 42.5)
        )
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, points, bos)
        val xml = bos.toString("UTF-8")

        assertTrue("Should contain <ele>", xml.contains("<ele>42.5</ele>"))
    }

    @Test
    fun `ele element is omitted when altitude is null`() = runTest {
        val points = listOf(
            makePoint(1, lat = 48.8566, lon = 2.3522, alt = null)
        )
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, points, bos)
        val xml = bos.toString("UTF-8")

        assertFalse("Should NOT contain <ele> when altitude is null", xml.contains("<ele>"))
    }

    @Test
    fun `time elements use UTC ISO-8601 with Z suffix`() = runTest {
        val points = listOf(
            makePoint(1, lat = 48.8566, lon = 2.3522, wallClockMs = 1_700_000_001_000L)
        )
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, points, bos)
        val xml = bos.toString("UTF-8")

        // All <time> values must end with Z
        val timeRegex = Regex("<time>([^<]+)</time>")
        val times = timeRegex.findAll(xml).map { it.groupValues[1] }.toList()
        assertTrue("Expected at least 2 <time> elements (metadata + trkpt)", times.size >= 2)
        times.forEach { t ->
            assertTrue("Time '$t' should end with Z", t.endsWith("Z"))
        }
    }

    @Test
    fun `metadata time reflects session start`() = runTest {
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, emptyList(), bos)
        val xml = bos.toString("UTF-8")

        // session.startTimeMs = 1_700_000_000_000 → 2023-11-14T22:13:20Z
        assertTrue(
            "Metadata time should contain the session start date",
            xml.contains("2023-11-14T22:13:20Z")
        )
    }

    @Test
    fun `track name falls back to Speed ride sessionId when notes blank`() = runTest {
        val bos = ByteArrayOutputStream()
        GpxExporter.write(session, emptyList(), bos)
        val xml = bos.toString("UTF-8")

        assertTrue(
            "Track name should contain default ride name",
            xml.contains("<name>Speed ride test-session-1</name>")
        )
    }

    @Test
    fun `track name uses session notes when available`() = runTest {
        val sessionWithNotes = session.copy(notes = "Sunday morning ride")
        val bos = ByteArrayOutputStream()
        GpxExporter.write(sessionWithNotes, emptyList(), bos)
        val xml = bos.toString("UTF-8")

        assertTrue(
            "Track name should use session notes",
            xml.contains("<name>Sunday morning ride</name>")
        )
    }

    @Test
    fun `xml special characters in notes are escaped`() = runTest {
        val sessionWithSpecialNotes = session.copy(notes = "Tom & Jerry <race> \"fast\"")
        val bos = ByteArrayOutputStream()
        GpxExporter.write(sessionWithSpecialNotes, emptyList(), bos)
        val xml = bos.toString("UTF-8")

        assertTrue("Ampersand should be escaped", xml.contains("Tom &amp; Jerry"))
        assertTrue("Less-than should be escaped", xml.contains("&lt;race&gt;"))
        assertTrue("Quotes should be escaped", xml.contains("&quot;fast&quot;"))
    }
}
