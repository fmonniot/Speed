package eu.monniot.speed.export

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * JVM unit tests for [ExportManager].
 *
 * No Android framework is used; runs on the standard JVM test runner.
 */
class ExportManagerTest {

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------

    private val session1 = Session(
        sessionId   = "session-alpha",
        startTimeMs = 1_700_000_000_000L,
        endTimeMs   = 1_700_000_600_000L,
        pointCount  = 2,
        maxSpeedMs  = 15f,
    )

    private val session2 = Session(
        sessionId   = "session-beta",
        startTimeMs = 1_700_001_000_000L,
        endTimeMs   = 1_700_001_300_000L,
        pointCount  = 1,
        maxSpeedMs  = 8f,
    )

    private val points1 = listOf(
        makePoint("session-alpha", elapsedNs = 0L,           wallMs = 1_700_000_000_000L),
        makePoint("session-alpha", elapsedNs = 1_000_000_000L, wallMs = 1_700_000_001_000L),
    )

    private val points2 = listOf(
        makePoint("session-beta",  elapsedNs = 0L,           wallMs = 1_700_001_000_000L),
    )

    private fun loadPoints(sessionId: String): List<DataPoint> = when (sessionId) {
        "session-alpha" -> points1
        "session-beta"  -> points2
        else            -> emptyList()
    }

    // -----------------------------------------------------------------------
    // ZIP structure tests
    // -----------------------------------------------------------------------

    /** Exporting 2 sessions must produce exactly 2 ZIP entries. */
    @Test
    fun exportZip_producesOneEntryPerSession() = runTest {
        val baos = ByteArrayOutputStream()
        ExportManager.exportZip(
            sessions    = listOf(session1, session2),
            loadPoints  = ::loadPoints,
            options     = allIncludeOptions(ExportFmt.CSV),
            out         = baos,
        )

        val entries = readZipEntryNames(baos.toByteArray())
        assertEquals(2, entries.size)
    }

    /** Entry names must follow the `trip_{index}_{sessionId}.csv` pattern. */
    @Test
    fun exportZip_entryNamesUseIndexAndSessionId() = runTest {
        val baos = ByteArrayOutputStream()
        ExportManager.exportZip(
            sessions   = listOf(session1, session2),
            loadPoints = ::loadPoints,
            options    = allIncludeOptions(ExportFmt.CSV),
            out        = baos,
        )

        val entries = readZipEntryNames(baos.toByteArray())
        assertTrue(entries.any { it == "trip_0_session-alpha.csv" })
        assertTrue(entries.any { it == "trip_1_session-beta.csv" })
    }

    // -----------------------------------------------------------------------
    // CSV column-group toggle tests
    // -----------------------------------------------------------------------

    /**
     * When [ExportOptions.includeImu] is false, the CSV header must NOT contain
     * any of the IMU column names.
     */
    @Test
    fun exportZip_csv_imuExcluded_whenToggleOff() = runTest {
        val options = ExportOptions(
            format       = ExportFmt.CSV,
            includeGps   = true,
            includeImu   = false,   // <-- off
            includeLean  = true,
        )
        val header = extractFirstEntryHeader(options)

        assertFalse("accel_x should be absent",          header.contains("accel_x"))
        assertFalse("derived_speed_ms should be absent", header.contains("derived_speed_ms"))
    }

    /**
     * When [ExportOptions.includeImu] is true, the CSV header must contain the full
     * set of IMU column names.
     */
    @Test
    fun exportZip_csv_imuIncluded_whenToggleOn() = runTest {
        val options = allIncludeOptions(ExportFmt.CSV)
        val header = extractFirstEntryHeader(options)

        assertTrue("accel_x should be present",          header.contains("accel_x"))
        assertTrue("derived_speed_ms should be present", header.contains("derived_speed_ms"))
    }

    /** Toggling GPS off removes GPS columns from the header. */
    @Test
    fun exportZip_csv_gpsExcluded_whenToggleOff() = runTest {
        val options = ExportOptions(
            format      = ExportFmt.CSV,
            includeGps  = false,
            includeImu  = true,
            includeLean = true,
        )
        val header = extractFirstEntryHeader(options)

        assertFalse("lat should be absent",          header.contains("lat,"))
        assertFalse("gps_speed_ms should be absent", header.contains("gps_speed_ms"))
    }

    /** Toggling lean off removes lean columns from the header. */
    @Test
    fun exportZip_csv_leanExcluded_whenToggleOff() = runTest {
        val options = ExportOptions(
            format      = ExportFmt.CSV,
            includeGps  = true,
            includeImu  = true,
            includeLean = false,
        )
        val header = extractFirstEntryHeader(options)

        assertFalse("lean_deg should be absent",  header.contains("lean_deg"))
        assertFalse("lateral_g should be absent", header.contains("lateral_g"))
    }

    /** The base columns (`elapsed_ms`, `wall_clock_iso`) are always present regardless of toggles. */
    @Test
    fun exportZip_csv_baseColumnsAlwaysPresent() = runTest {
        val options = ExportOptions(
            format      = ExportFmt.CSV,
            includeGps  = false,
            includeImu  = false,
            includeLean = false,
        )
        val header = extractFirstEntryHeader(options)

        assertTrue("elapsed_ms should always be present",    header.contains("elapsed_ms"))
        assertTrue("wall_clock_iso should always be present", header.contains("wall_clock_iso"))
    }

    // -----------------------------------------------------------------------
    // estimateBytes tests
    // -----------------------------------------------------------------------

    /** estimateBytes must be positive even for a single empty session. */
    @Test
    fun estimateBytes_isPositiveForEmptySession() {
        val empty = session1.copy(pointCount = 0)
        val estimate = ExportManager.estimateBytes(listOf(empty), allIncludeOptions(ExportFmt.CSV))
        assertTrue("estimate should be > 0", estimate > 0L)
    }

    /**
     * Enabling more column groups must increase the CSV size estimate
     * (given a session with at least one point).
     */
    @Test
    fun estimateBytes_growsWithMoreIncludeGroups_csv() {
        val session = session1.copy(pointCount = 100)

        val baseOnly = ExportOptions(ExportFmt.CSV, includeGps = false, includeImu = false, includeLean = false)
        val withGps  = baseOnly.copy(includeGps  = true)
        val withImu  = baseOnly.copy(includeImu  = true)
        val all      = allIncludeOptions(ExportFmt.CSV)

        val estBase   = ExportManager.estimateBytes(listOf(session), baseOnly)
        val estGps    = ExportManager.estimateBytes(listOf(session), withGps)
        val estImu    = ExportManager.estimateBytes(listOf(session), withImu)
        val estAll    = ExportManager.estimateBytes(listOf(session), all)

        assertTrue("GPS group increases estimate",  estGps  > estBase)
        assertTrue("IMU group increases estimate",  estImu  > estBase)
        assertTrue("All groups is largest",         estAll  > estGps)
        assertTrue("All groups > IMU alone",        estAll  > estImu)
    }

    /**
     * FIT has a smaller per-point footprint than GPX (binary vs XML),
     * and both are independent of the INCLUDE toggles.
     */
    @Test
    fun estimateBytes_fitSmallerThanGpx() {
        val session = session1.copy(pointCount = 500)

        val fitEst = ExportManager.estimateBytes(listOf(session), allIncludeOptions(ExportFmt.FIT))
        val gpxEst = ExportManager.estimateBytes(listOf(session), allIncludeOptions(ExportFmt.GPX))

        assertTrue("FIT estimate should be smaller than GPX", fitEst < gpxEst)
    }

    /** Summing estimates across sessions should equal the sum of their individual estimates. */
    @Test
    fun estimateBytes_isAdditiveAcrossSessions() {
        val options = allIncludeOptions(ExportFmt.CSV)

        val individual = ExportManager.estimateBytes(listOf(session1), options) +
                         ExportManager.estimateBytes(listOf(session2), options)
        val combined   = ExportManager.estimateBytes(listOf(session1, session2), options)

        assertEquals(individual, combined)
    }

    // -----------------------------------------------------------------------
    // onProgress callback test
    // -----------------------------------------------------------------------

    /** onProgress must be called once per session, incrementing done from 1 to total. */
    @Test
    fun exportZip_progressCallbackInvocations() = runTest {
        val progressLog = mutableListOf<Pair<Int, Int>>()
        ExportManager.exportZip(
            sessions   = listOf(session1, session2),
            loadPoints = ::loadPoints,
            options    = allIncludeOptions(ExportFmt.CSV),
            out        = ByteArrayOutputStream(),
            onProgress = { done, total -> progressLog.add(done to total) },
        )

        assertEquals(listOf(1 to 2, 2 to 2), progressLog)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private suspend fun extractFirstEntryHeader(options: ExportOptions): String {
        val baos = ByteArrayOutputStream()
        ExportManager.exportZip(
            sessions   = listOf(session1),
            loadPoints = ::loadPoints,
            options    = options,
            out        = baos,
        )
        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        zis.nextEntry ?: error("No entry in zip")
        return zis.bufferedReader().readLine() ?: ""
    }

    private fun readZipEntryNames(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        val zis = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zis.nextEntry
        while (entry != null) {
            names.add(entry.name)
            zis.closeEntry()
            entry = zis.nextEntry
        }
        return names
    }

    private fun allIncludeOptions(format: ExportFmt) = ExportOptions(
        format      = format,
        includeGps  = true,
        includeImu  = true,
        includeLean = true,
    )

    private fun makePoint(
        sessionId: String,
        elapsedNs: Long,
        wallMs: Long,
    ) = DataPoint(
        sessionId          = sessionId,
        elapsedRealtimeNs  = elapsedNs,
        wallClockMs        = wallMs,
        latitude           = 48.8566,
        longitude          = 2.3522,
        altitude           = 35.0,
        gpsSpeedMs         = 10f,
        gpsAccuracyM       = 3f,
        satellitesUsed     = 8,
        satellitesVisible  = 10,
        accelX             = 0.1f,
        accelY             = 0.2f,
        accelZ             = 9.8f,
        accelMagnitude     = 9.81f,
        derivedSpeedMs     = 9.9f,
        derivedAccelMs2    = 0.3f,
        leanAngleDeg       = 5f,
        lateralGz          = 0.08f,
    )
}
