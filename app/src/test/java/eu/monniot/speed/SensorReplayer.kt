package eu.monniot.speed

import eu.monniot.speed.sensor.ImuSample
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Scanner

sealed class RawEvent {
    abstract val timestampNs: Long

    @Suppress("ArrayInDataClass") // we aren't using its equality/hash methods
    data class Imu(
        override val timestampNs: Long,
        val accel: FloatArray,
        val rotationVector: FloatArray
    ) : RawEvent()

    data class Gps(
        override val timestampNs: Long,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val speedMs: Float,
        val accuracyM: Float,
        val bearing: Float
    ) : RawEvent()

    data class Status(
        override val timestampNs: Long,
        val used: Int,
        val visible: Int
    ) : RawEvent()
}

/**
 * Parses raw trace CSVs and provides events for replay.
 */
class SensorReplayer(inputStream: InputStream) {
    private val events = mutableListOf<RawEvent>()

    companion object {
        fun fromRawTraceResource(path: String): SensorReplayer {
            val traceFile = File(path)
            if (!traceFile.exists()) {
                println("Trace file not found, skipping test. Run on machine with traces.")
                throw FileNotFoundException("Trace file not found")
            }

            return SensorReplayer(traceFile.inputStream())
        }
    }

    init {
        val scanner = Scanner(inputStream)
        if (scanner.hasNextLine()) scanner.nextLine() // Skip header

        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            val parts = line.split(",")
            if (parts.size < 2) continue

            val type = parts[0]
            val timestampNs = parts[1].toLong()

            when (type) {
                "I" -> {
                    // I,timestamp,accelX,accelY,accelZ,rotX,rotY,rotZ,rotW
                    val accel =
                        floatArrayOf(parts[2].toFloat(), parts[3].toFloat(), parts[4].toFloat())
                    val rot = floatArrayOf(
                        parts[5].toFloat(),
                        parts[6].toFloat(),
                        parts[7].toFloat(),
                        parts[8].toFloat()
                    )
                    events.add(RawEvent.Imu(timestampNs, accel, rot))
                }

                "G" -> {
                    // G,timestamp,lat,lon,alt,speed,accuracy,bearing
                    events.add(
                        RawEvent.Gps(
                            timestampNs = timestampNs,
                            latitude = parts[2].toDouble(),
                            longitude = parts[3].toDouble(),
                            altitude = parts[4].toDouble(),
                            speedMs = parts[5].toFloat(),
                            accuracyM = parts[6].toFloat(),
                            bearing = parts[7].toFloat()
                        )
                    )
                }

                "S" -> {
                    // S,timestamp,used,visible
                    events.add(
                        RawEvent.Status(
                            timestampNs = timestampNs,
                            used = parts[2].trim().toInt(),
                            visible = parts[3].trim().toInt()
                        )
                    )
                }
            }
        }
        // Sort by timestamp just in case
        events.sortBy { it.timestampNs }
    }

    fun getEvents(): List<RawEvent> = events
}
