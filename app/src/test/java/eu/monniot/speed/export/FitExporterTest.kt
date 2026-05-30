package eu.monniot.speed.export

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Unit tests for [FitExporter].
 *
 * These tests verify the internal consistency of the generated FIT binary without requiring
 * an external FIT decoder library:
 *
 * 1. The ASCII magic bytes ".FIT" appear at bytes 8–11 of the file header.
 * 2. The data-size field (bytes 4–7, UInt32 LE) equals `totalLength − 14 (header) − 2 (CRC)`.
 * 3. The file CRC appended as the last 2 bytes equals `crc16(allBytesExceptLast2)`.
 */
class FitExporterTest {

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private val session = Session(
        sessionId   = "test-session-1",
        startTimeMs = 1_700_000_000_000L, // 2023-11-14 22:13:20 UTC
        endTimeMs   = 1_700_000_600_000L,
        pointCount  = 3,
        maxSpeedMs  = 12.5f,
        notes       = "FIT exporter test ride"
    )

    /** A [DataPoint] factory that fills in all mandatory fields with sensible defaults. */
    private fun makePoint(
        wallClockMs: Long,
        lat: Double?,
        lon: Double?,
        altM: Double? = 42.0,
        speedMs: Float? = 5.0f
    ) = DataPoint(
        id                   = 0,
        sessionId            = session.sessionId,
        elapsedRealtimeNs    = 0L,
        wallClockMs          = wallClockMs,
        latitude             = lat,
        longitude            = lon,
        altitude             = altM,
        gpsSpeedMs           = speedMs,
        gpsAccuracyM         = null,
        satellitesUsed       = null,
        satellitesVisible    = null,
        accelX               = 0f,
        accelY               = 0f,
        accelZ               = 0f,
        accelMagnitude       = 0f,
        derivedSpeedMs       = null,
        derivedAccelMs2      = null
    )

    private val points = listOf(
        makePoint(wallClockMs = 1_700_000_000_000L, lat = 48.8566,  lon = 2.3522,  altM = 35.0, speedMs = 8.3f),
        makePoint(wallClockMs = 1_700_000_030_000L, lat = 48.8567,  lon = 2.3525,  altM = 35.5, speedMs = 9.1f),
        makePoint(wallClockMs = 1_700_000_060_000L, lat = null,      lon = null,   altM = null,  speedMs = null), // no lat/lon → skipped
        makePoint(wallClockMs = 1_700_000_090_000L, lat = 48.8570,  lon = 2.3531,  altM = 36.0, speedMs = 10.0f)
    )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Reads a UInt32 little-endian from [buf] at [offset]. */
    private fun readUInt32LE(buf: ByteArray, offset: Int): Long {
        return ((buf[offset].toLong() and 0xFF) or
                ((buf[offset + 1].toLong() and 0xFF) shl 8) or
                ((buf[offset + 2].toLong() and 0xFF) shl 16) or
                ((buf[offset + 3].toLong() and 0xFF) shl 24))
    }

    /** Reads a UInt16 little-endian from [buf] at [offset]. */
    private fun readUInt16LE(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or
               ((buf[offset + 1].toInt() and 0xFF) shl 8)
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * The ASCII string ".FIT" must appear at bytes 8–11 of every valid FIT file.
     * This is the primary magic-number check performed by FIT decoders.
     */
    @Test
    fun magicBytesAreDotFIT() = runTest {
        val bos = ByteArrayOutputStream()
        FitExporter.write(session, points, bos)
        val bytes = bos.toByteArray()

        // bytes 8–11 must spell ".FIT"
        val magic = String(bytes, 8, 4, Charsets.US_ASCII)
        assertEquals(".FIT", magic)
    }

    /**
     * The data-size field (header bytes 4–7, UInt32 LE) must equal:
     *   total file length − 14 (header) − 2 (file CRC trailer).
     *
     * This verifies that [FitExporter.write] computes the records-section size correctly.
     */
    @Test
    fun dataSizeFieldIsConsistent() = runTest {
        val bos = ByteArrayOutputStream()
        FitExporter.write(session, points, bos)
        val bytes = bos.toByteArray()

        val totalLen    = bytes.size
        val headerSize  = 14
        val crcSize     = 2
        val expectedDataSize = totalLen - headerSize - crcSize

        val actualDataSize = readUInt32LE(bytes, 4).toInt()
        assertEquals(
            "data-size field should equal (total − header − CRC)",
            expectedDataSize,
            actualDataSize
        )
    }

    /**
     * The 2-byte file CRC at the end of the file must equal `crc16` computed over all
     * preceding bytes. This is the round-trip integrity check for the FIT CRC algorithm.
     */
    @Test
    fun fileCrcIsCorrect() = runTest {
        val bos = ByteArrayOutputStream()
        FitExporter.write(session, points, bos)
        val bytes = bos.toByteArray()

        val payloadLen  = bytes.size - 2
        val expectedCrc = FitExporter.crc16(bytes, payloadLen)
        val actualCrc   = readUInt16LE(bytes, payloadLen)

        assertEquals(
            "trailing file CRC must match crc16(allBytesExceptLastTwo)",
            expectedCrc,
            actualCrc
        )
    }

    /**
     * Sanity check: encoding a session with zero valid GPS points should still produce a
     * structurally valid file (header + file_id message + empty record set + CRC).
     */
    @Test
    fun emptyPointListProducesValidHeader() = runTest {
        val bos = ByteArrayOutputStream()
        FitExporter.write(session, emptyList(), bos)
        val bytes = bos.toByteArray()

        // Must at minimum be 14 (header) + 2 (file CRC) = 16 bytes.
        assert(bytes.size >= 16) { "File is too short: ${bytes.size} bytes" }

        // Magic bytes check.
        assertEquals(".FIT", String(bytes, 8, 4, Charsets.US_ASCII))

        // CRC round-trip.
        val payloadLen  = bytes.size - 2
        val expectedCrc = FitExporter.crc16(bytes, payloadLen)
        val actualCrc   = readUInt16LE(bytes, payloadLen)
        assertEquals(expectedCrc, actualCrc)
    }

    /**
     * Points with null lat/lon must be silently skipped.
     * The data-size field must still be consistent after skipping.
     */
    @Test
    fun nullLatLonPointsAreSkipped() = runTest {
        val allNullPoints = listOf(
            makePoint(wallClockMs = 1_700_000_000_000L, lat = null, lon = null)
        )

        val bosAll  = ByteArrayOutputStream()
        val bosNone = ByteArrayOutputStream()
        FitExporter.write(session, allNullPoints, bosAll)
        FitExporter.write(session, emptyList(),   bosNone)

        // Both files should be identical because the single null-lat/lon point is skipped.
        assertArrayEquals(bosNone.toByteArray(), bosAll.toByteArray())
    }
}
