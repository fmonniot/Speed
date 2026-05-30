package eu.monniot.speed.export

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ---------------------------------------------------------------------------
// Public enumerations and option types
// ---------------------------------------------------------------------------

/**
 * The file format to produce inside the ZIP archive.
 *
 * The orchestrator maps between its own `ExportFormat` screen enum and this value
 * before calling [ExportManager].
 */
enum class ExportFmt { CSV, GPX, FIT }

/**
 * Fully describes what should be exported.
 *
 * @property format      Target file format for every trip entry.
 * @property includeGps  When [format] is [ExportFmt.CSV], emit the GPS column group
 *                       (`lat`, `lon`, `altitude_m`, `gps_speed_ms`, `gps_accuracy_m`).
 *                       Ignored for GPX / FIT (those formats are GPS-track files by nature).
 * @property includeImu  When [format] is [ExportFmt.CSV], emit the IMU column group
 *                       (`accel_x`, `accel_y`, `accel_z`, `accel_magnitude`,
 *                       `derived_speed_ms`, `derived_accel_ms2`).
 *                       Ignored for GPX / FIT.
 * @property includeLean When [format] is [ExportFmt.CSV], emit the lean-angle column group
 *                       (`lean_deg`, `lateral_g`).
 *                       Ignored for GPX / FIT.
 */
data class ExportOptions(
    val format: ExportFmt,
    val includeGps: Boolean,
    val includeImu: Boolean,
    val includeLean: Boolean,
)

// ---------------------------------------------------------------------------
// ExportManager
// ---------------------------------------------------------------------------

/**
 * Engine that produces a single ZIP artifact containing one file per trip.
 *
 * This object has no Android framework dependencies and can be tested on the JVM.
 * It delegates GPX and FIT serialisation to [GpxExporter] and [FitExporter] respectively,
 * and handles CSV serialisation itself (honouring the [ExportOptions] INCLUDE toggles).
 */
object ExportManager {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Writes a ZIP archive of all [sessions] to [out].
     *
     * Each session produces one entry named `trip_{index}_{sessionId}.{ext}`,
     * where `index` is the 0-based position in [sessions] and `ext` is derived
     * from [ExportOptions.format].
     *
     * The ZIP stream is finalised (closed) after all sessions are written.
     * [out] itself is not closed; lifecycle is the caller's responsibility.
     *
     * All I/O runs on [Dispatchers.IO].
     *
     * @param sessions    Ordered list of sessions to include.
     * @param loadPoints  Suspend function that returns all [DataPoint]s for a session ID.
     *                    The orchestrator typically passes `repository::getPointsForSession`.
     * @param options     Format and column-group toggles.
     * @param out         Destination stream; must be open and writable.
     * @param onProgress  Optional callback invoked with `(done, total)` after each session
     *                    is written. `done` is in `1..total`.
     */
    suspend fun exportZip(
        sessions: List<Session>,
        loadPoints: suspend (String) -> List<DataPoint>,
        options: ExportOptions,
        out: OutputStream,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        val total = sessions.size
        val ext = options.format.extension()

        // ZipOutputStream wraps the caller's stream.  We close the ZIP after
        // all entries are written so the central directory is flushed, but we
        // do NOT close the underlying [out] (ZipOutputStream.close() would do
        // that, so we finish() instead).
        val zip = ZipOutputStream(out)

        sessions.forEachIndexed { index, session ->
            val entryName = "trip_${index}_${session.sessionId}.$ext"
            zip.putNextEntry(ZipEntry(entryName))

            val points = loadPoints(session.sessionId)

            when (options.format) {
                ExportFmt.CSV -> writeCsv(session, points, options, zip)
                ExportFmt.GPX -> GpxExporter.write(session, points, zip)
                ExportFmt.FIT -> FitExporter.write(session, points, zip)
            }

            zip.closeEntry()
            onProgress(index + 1, total)
        }

        // Finish writes the ZIP central directory without closing the underlying stream.
        zip.finish()
    }

    /**
     * Estimates the total uncompressed byte count for the export described by [sessions]
     * and [options], without loading any [DataPoint]s from storage.
     *
     * The heuristic is intentionally coarse — it is only used to populate a "~X MB"
     * label on the Export button.
     *
     * ### Per-point byte weights
     *
     * **CSV** (characters per row):
     * - Base (`elapsed_ms`, `wall_clock_iso`): ~30 B
     * - GPS group (`lat`, `lon`, `altitude_m`, `gps_speed_ms`, `gps_accuracy_m`): ~45 B
     * - IMU group (`accel_x/y/z`, `accel_magnitude`, `derived_speed_ms`, `derived_accel_ms2`): ~70 B
     * - Lean group (`lean_deg`, `lateral_g`): ~20 B
     *
     * **GPX**: ~80 B per point (XML tags + lat/lon/time values).
     *
     * **FIT**: ~20 B per point (binary record message).
     *
     * A minimum floor of 128 bytes per session is applied to avoid misleading
     * zero-byte estimates for empty sessions.
     *
     * @return Estimated size in bytes (always ≥ 1).
     */
    fun estimateBytes(sessions: List<Session>, options: ExportOptions): Long {
        if (sessions.isEmpty()) return 0L

        var total = 0L
        for (session in sessions) {
            val points = session.pointCount.toLong().coerceAtLeast(0L)
            val perPoint: Long = when (options.format) {
                ExportFmt.CSV -> {
                    var w = BASE_BYTES_PER_POINT_CSV
                    if (options.includeGps) w += GPS_BYTES_PER_POINT_CSV
                    if (options.includeImu) w += IMU_BYTES_PER_POINT_CSV
                    if (options.includeLean) w += LEAN_BYTES_PER_POINT_CSV
                    w
                }
                ExportFmt.GPX -> GPX_BYTES_PER_POINT
                ExportFmt.FIT -> FIT_BYTES_PER_POINT
            }
            val sessionBytes = (points * perPoint).coerceAtLeast(SESSION_FLOOR_BYTES)
            total += sessionBytes
        }
        return total.coerceAtLeast(1L)
    }

    // -----------------------------------------------------------------------
    // CSV writer (private)
    // -----------------------------------------------------------------------

    /**
     * Writes a CSV file for [session] into [out], honouring the INCLUDE toggles in [options].
     *
     * Column groups (always in this order when enabled):
     *  1. **Base** (always present): `elapsed_ms`, `wall_clock_iso`
     *  2. **GPS** (when [ExportOptions.includeGps]):
     *     `lat`, `lon`, `altitude_m`, `gps_speed_ms`, `gps_accuracy_m`
     *  3. **IMU** (when [ExportOptions.includeImu]):
     *     `accel_x`, `accel_y`, `accel_z`, `accel_magnitude`,
     *     `derived_speed_ms`, `derived_accel_ms2`
     *  4. **Lean** (when [ExportOptions.includeLean]):
     *     `lean_deg`, `lateral_g`
     *
     * `elapsed_ms` is computed relative to the `elapsedRealtimeNs` of the **first point**
     * (not the session's `startTimeMs`, which originates from a different clock).
     *
     * The writer is flushed but NOT closed; the ZIP entry lifecycle is managed by the caller.
     */
    private fun writeCsv(
        session: Session,
        points: List<DataPoint>,
        options: ExportOptions,
        out: OutputStream,
    ) {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

        val writer = out.bufferedWriter(Charsets.UTF_8)

        // -- Header --
        val headerCols = buildList {
            add("elapsed_ms")
            add("wall_clock_iso")
            if (options.includeGps) addAll(GPS_COLUMNS)
            if (options.includeImu) addAll(IMU_COLUMNS)
            if (options.includeLean) addAll(LEAN_COLUMNS)
        }
        writer.write(headerCols.joinToString(","))
        writer.write("\n")

        // Baseline nanosecond value for elapsed computation.
        val baseNs = points.firstOrNull()?.elapsedRealtimeNs ?: 0L

        // -- Rows --
        for (p in points) {
            val elapsedMs = (p.elapsedRealtimeNs - baseNs) / 1_000_000L
            val wallIso = isoFormat.format(Date(p.wallClockMs))

            val row = buildList<Any?> {
                add(elapsedMs)
                add(wallIso)
                if (options.includeGps) {
                    add(p.latitude)
                    add(p.longitude)
                    add(p.altitude)
                    add(p.gpsSpeedMs)
                    add(p.gpsAccuracyM)
                }
                if (options.includeImu) {
                    add(p.accelX)
                    add(p.accelY)
                    add(p.accelZ)
                    add(p.accelMagnitude)
                    add(p.derivedSpeedMs)
                    add(p.derivedAccelMs2)
                }
                if (options.includeLean) {
                    add(p.leanAngleDeg)
                    add(p.lateralGz)
                }
            }
            writer.write(row.joinToString(","))
            writer.write("\n")
        }

        writer.flush()
        // Do NOT close — the ZipOutputStream entry is still open.
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private val GPS_COLUMNS  = listOf("lat", "lon", "altitude_m", "gps_speed_ms", "gps_accuracy_m")
    private val IMU_COLUMNS  = listOf("accel_x", "accel_y", "accel_z", "accel_magnitude", "derived_speed_ms", "derived_accel_ms2")
    private val LEAN_COLUMNS = listOf("lean_deg", "lateral_g")

    // CSV byte-weight heuristics
    private const val BASE_BYTES_PER_POINT_CSV: Long = 30L
    private const val GPS_BYTES_PER_POINT_CSV:  Long = 45L
    private const val IMU_BYTES_PER_POINT_CSV:  Long = 70L
    private const val LEAN_BYTES_PER_POINT_CSV: Long = 20L

    // Non-CSV byte-weight heuristics
    private const val GPX_BYTES_PER_POINT: Long = 80L
    private const val FIT_BYTES_PER_POINT: Long = 20L

    // Minimum per-session floor to avoid misleading 0-byte labels
    private const val SESSION_FLOOR_BYTES: Long = 128L
}

// ---------------------------------------------------------------------------
// Extension helpers (file-private)
// ---------------------------------------------------------------------------

private fun ExportFmt.extension(): String = when (this) {
    ExportFmt.CSV -> "csv"
    ExportFmt.GPX -> "gpx"
    ExportFmt.FIT -> "fit"
}
