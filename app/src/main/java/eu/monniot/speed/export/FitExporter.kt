package eu.monniot.speed.export

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Encodes one session's GPS record stream into a valid Garmin FIT file (Activity type).
 *
 * ## FIT binary format overview
 * A FIT file is structured as:
 *   [14-byte file header] [records section] [2-byte file CRC]
 *
 * The records section contains alternating *definition* messages (describe schema) and *data*
 * messages (carry values).  All multi-byte integers are **little-endian** (architecture = 0).
 *
 * ### CRC algorithm
 * FIT uses a custom CRC-16 applied both to the file header (bytes 0–11) and to the entire file
 * (header + records) as a final 2-byte trailer.
 *
 * ### FIT epoch
 * FIT timestamps are seconds since 1989-12-31 00:00:00 UTC.
 * Unix epoch offset: 631 065 600 seconds.
 *
 * ### Scaling
 * - **Position** (semicircles): degrees × (2^31 / 180), rounded to Int.
 * - **Altitude** (UInt16, metres): (altitude_m + 500) × 5, clamped to [0, 65534].
 * - **Speed** (UInt16, m/s): speed_ms × 1000, clamped to [0, 65534].
 *
 * The stream is **flushed but not closed**; lifecycle is the caller's responsibility.
 */
object FitExporter {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Encodes [session] and its [points] as a FIT Activity file, writing to [out].
     *
     * Only [DataPoint]s that have non-null [DataPoint.latitude] **and** [DataPoint.longitude]
     * are included; points missing either coordinate are silently skipped.
     *
     * All blocking I/O runs on [Dispatchers.IO].
     */
    suspend fun write(session: Session, points: List<DataPoint>, out: OutputStream) =
        withContext(Dispatchers.IO) {
            // 1. Build the records section into a byte buffer first so we know its length,
            //    which is required in the file header.
            val recordsBuf = ByteArrayOutputStream()
            writeRecords(session, points, recordsBuf)
            val recordsBytes = recordsBuf.toByteArray()

            // 2. Build the 14-byte file header.
            val headerBuf = ByteArrayOutputStream(14)
            writeFileHeader(recordsBytes.size, headerBuf)
            val headerBytes = headerBuf.toByteArray()

            // 3. Compute file CRC over header + records combined.
            val combined = headerBytes + recordsBytes
            val fileCrc = crc16(combined, combined.size)

            // 4. Write everything to the output stream.
            out.write(headerBytes)
            out.write(recordsBytes)
            writeUInt16LE(fileCrc, out)

            out.flush()
        }

    // -------------------------------------------------------------------------
    // File header
    // -------------------------------------------------------------------------

    /**
     * Writes the 14-byte FIT file header.
     *
     *  Offset  Size  Description
     *  ------  ----  -----------
     *    0       1   Header size = 14
     *    1       1   Protocol version = 0x10 (FIT 2.0)
     *    2–3     2   Profile version = 2078 (LE)
     *    4–7     4   Data size = byte count of records section (LE)
     *    8–11    4   ASCII ".FIT"
     *   12–13   2   Header CRC (LE) over bytes 0–11
     */
    private fun writeFileHeader(dataSize: Int, out: OutputStream) {
        val tmp = ByteArrayOutputStream(12)
        tmp.write(14)               // header size
        tmp.write(0x10)             // protocol version 2.0
        writeUInt16LE(2078, tmp)    // profile version (arbitrary valid value)
        writeUInt32LE(dataSize, tmp)
        tmp.write(".FIT".toByteArray(Charsets.US_ASCII))

        val firstTwelve = tmp.toByteArray()
        out.write(firstTwelve)

        // Header CRC (bytes 0–11)
        val headerCrc = crc16(firstTwelve, firstTwelve.size)
        writeUInt16LE(headerCrc, out)
    }

    // -------------------------------------------------------------------------
    // Records section
    // -------------------------------------------------------------------------

    /**
     * Writes all definition + data messages into [out].
     *
     * Message layout:
     *  - local message type 0 → global message 0  (file_id)
     *  - local message type 1 → global message 20 (record / GPS track point)
     */
    private fun writeRecords(session: Session, points: List<DataPoint>, out: OutputStream) {
        // --- file_id definition (local 0) ---
        // Fields: type(1B enum), manufacturer(2B uint16), product(2B uint16),
        //         time_created(4B uint32)
        writeDefinitionMessage(
            out = out,
            localMsgType = 0,
            globalMsgNum = 0,   // file_id
            fields = listOf(
                FieldDef(fieldDefNum = 3, size = 1, baseType = BASE_ENUM),   // type
                FieldDef(fieldDefNum = 1, size = 2, baseType = BASE_UINT16), // manufacturer
                FieldDef(fieldDefNum = 2, size = 2, baseType = BASE_UINT16), // product
                FieldDef(fieldDefNum = 4, size = 4, baseType = BASE_UINT32)  // time_created
            )
        )

        // --- file_id data ---
        val timeCreated = toFitTimestamp(session.startTimeMs)
        writeDataMessageHeader(out, localMsgType = 0)
        out.write(4)                          // type = activity (enum 4)
        writeUInt16LE(255, out)               // manufacturer = development (255)
        writeUInt16LE(0, out)                 // product = 0
        writeUInt32LE(timeCreated, out)       // time_created

        // --- record definition (local 1) ---
        // Fields (per FIT Profile):
        //   253 timestamp   uint32  (FIT date_time, seconds since FIT epoch)
        //     0 position_lat sint32  (semicircles)
        //     1 position_long sint32  (semicircles)
        //     2 altitude     uint16  (scale: (m + 500) × 5, unit: m/5 + offset)
        //     6 speed        uint16  (scale: m/s × 1000)
        writeDefinitionMessage(
            out = out,
            localMsgType = 1,
            globalMsgNum = 20,  // record
            fields = listOf(
                FieldDef(fieldDefNum = 253, size = 4, baseType = BASE_UINT32), // timestamp
                FieldDef(fieldDefNum = 0,   size = 4, baseType = BASE_SINT32), // position_lat
                FieldDef(fieldDefNum = 1,   size = 4, baseType = BASE_SINT32), // position_long
                FieldDef(fieldDefNum = 2,   size = 2, baseType = BASE_UINT16), // altitude
                FieldDef(fieldDefNum = 6,   size = 2, baseType = BASE_UINT16)  // speed
            )
        )

        // --- one record data message per point with valid lat/lon ---
        for (point in points) {
            val lat = point.latitude ?: continue
            val lon = point.longitude ?: continue

            val timestamp  = toFitTimestamp(point.wallClockMs)
            val latSc      = degreesToSemicircles(lat)
            val lonSc      = degreesToSemicircles(lon)
            // altitude: (m + 500) × 5  →  clamped UInt16. Invalid = 0xFFFF.
            val altRaw     = point.altitude
                ?.let { ((it + 500.0) * 5.0).toInt().coerceIn(0, 65534) }
                ?: 0xFFFF
            // speed: prefer GPS speed, fall back to derived; scale m/s × 1000 → UInt16.
            val speedMs    = point.gpsSpeedMs ?: point.derivedSpeedMs
            val speedRaw   = speedMs
                ?.let { (it * 1000f).toInt().coerceIn(0, 65534) }
                ?: 0xFFFF

            writeDataMessageHeader(out, localMsgType = 1)
            writeUInt32LE(timestamp, out)
            writeSInt32LE(latSc, out)
            writeSInt32LE(lonSc, out)
            writeUInt16LE(altRaw, out)
            writeUInt16LE(speedRaw, out)
        }
    }

    // -------------------------------------------------------------------------
    // Definition and data message helpers
    // -------------------------------------------------------------------------

    /** Holds the three bytes that describe one field inside a definition message. */
    private data class FieldDef(val fieldDefNum: Int, val size: Int, val baseType: Int)

    /**
     * Writes a FIT *definition* message.
     *
     * Record header byte for definitions: `0x40 OR localMsgType`.
     *
     * Definition body layout:
     *   reserved(1)=0, architecture(1)=0 (LE), globalMsgNum(UInt16 LE),
     *   numFields(1), [fieldDefNum(1) size(1) baseType(1)] × numFields
     */
    private fun writeDefinitionMessage(
        out: OutputStream,
        localMsgType: Int,
        globalMsgNum: Int,
        fields: List<FieldDef>
    ) {
        // Header: definition flag (0x40) OR local message type (0..15)
        out.write(0x40 or (localMsgType and 0x0F))
        out.write(0)                            // reserved
        out.write(0)                            // architecture = 0 (little-endian)
        writeUInt16LE(globalMsgNum, out)
        out.write(fields.size)
        for (f in fields) {
            out.write(f.fieldDefNum)
            out.write(f.size)
            out.write(f.baseType)
        }
    }

    /**
     * Writes the 1-byte record header for a *data* message.
     *
     * Header byte for data: `0x00 OR localMsgType`.
     */
    private fun writeDataMessageHeader(out: OutputStream, localMsgType: Int) {
        out.write(0x00 or (localMsgType and 0x0F))
    }

    // -------------------------------------------------------------------------
    // Encoding utilities
    // -------------------------------------------------------------------------

    /** Writes a 2-byte unsigned integer in little-endian order. */
    private fun writeUInt16LE(value: Int, out: OutputStream) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    /** Writes a 4-byte unsigned integer in little-endian order. */
    private fun writeUInt32LE(value: Int, out: OutputStream) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    /** Writes a 4-byte signed integer in little-endian order (two's complement). */
    private fun writeSInt32LE(value: Int, out: OutputStream) = writeUInt32LE(value, out)

    // -------------------------------------------------------------------------
    // FIT-specific conversions
    // -------------------------------------------------------------------------

    /**
     * Converts a Unix wall-clock millisecond timestamp to a FIT timestamp (UInt32, seconds).
     *
     * FIT epoch offset: 1989-12-31 00:00:00 UTC = 631 065 600 Unix seconds.
     */
    private fun toFitTimestamp(wallClockMs: Long): Int {
        val fitEpochOffsetSeconds = 631_065_600L
        val fitSeconds = (wallClockMs / 1000L) - fitEpochOffsetSeconds
        // Clamp to valid UInt32 range just in case of bogus data.
        return fitSeconds.coerceIn(0L, 0xFFFFFFFFL).toInt()
    }

    /**
     * Converts geographic degrees to FIT semicircles.
     *
     * Formula: semicircles = round(degrees × (2^31 / 180))
     *
     * The result fits in a signed 32-bit integer (SInt32) which is what FIT uses for
     * position_lat and position_long.
     */
    private fun degreesToSemicircles(degrees: Double): Int =
        (degrees * (Int.MAX_VALUE.toLong() + 1L).toDouble() / 180.0).toLong().toInt()

    // -------------------------------------------------------------------------
    // FIT CRC-16
    // -------------------------------------------------------------------------

    // Lookup table for the FIT CRC-16 algorithm (defined in the FIT Protocol specification).
    private val CRC_TABLE = intArrayOf(
        0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401,
        0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400
    )

    /**
     * Computes the FIT CRC-16 over the first [len] bytes of [data].
     *
     * The algorithm processes each byte in two 4-bit nibbles using the [CRC_TABLE].
     * Returns a value in [0, 65535].
     */
    fun crc16(data: ByteArray, len: Int): Int {
        var crc = 0
        for (i in 0 until len) {
            val byte = data[i].toInt() and 0xFF
            // Low nibble
            var tmp = CRC_TABLE[crc and 0xF]
            crc = (crc ushr 4) and 0x0FFF
            crc = crc xor tmp xor CRC_TABLE[byte and 0xF]
            // High nibble
            tmp = CRC_TABLE[crc and 0xF]
            crc = (crc ushr 4) and 0x0FFF
            crc = crc xor tmp xor CRC_TABLE[(byte ushr 4) and 0xF]
        }
        return crc and 0xFFFF
    }

    // -------------------------------------------------------------------------
    // Base type constants (FIT Protocol Table 7)
    // -------------------------------------------------------------------------

    private const val BASE_ENUM   = 0x00 // 1 byte
    private const val BASE_UINT16 = 0x84 // 2 bytes, unsigned
    private const val BASE_UINT32 = 0x86 // 4 bytes, unsigned
    private const val BASE_SINT32 = 0x85 // 4 bytes, signed
}
