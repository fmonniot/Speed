package eu.monniot.speed

import eu.monniot.speed.fusion.CoordinateTransformer
import eu.monniot.speed.fusion.GpsObservation
import eu.monniot.speed.fusion.ImuWindow
import eu.monniot.speed.fusion.VelocityFusion
import eu.monniot.speed.sensor.ImuSample
import org.junit.Test
import java.io.File
import kotlin.math.PI

class VelocityCalibrationTests {

    /**
     * Calibration test for a phone stationary on a flat surface.
     * Ground Truth: Speed = 0.0 km/h, ZUPT = 100%.
     *
     * NOTE ON STATUS (S) EVENTS:
     * Current legacy traces have corrupted timestamps for Status events because they were
     * recorded using System.nanoTime() instead of SystemClock.elapsedRealtimeNanos().
     * This creates a ~5-day gap in the monotonic timeline which starves the fusion logic.
     *
     * We currently filter these out to allow testing of IMU and GPS fusion.
     * This logic will be removed once new traces are recorded with the fixed FileRawSink.
     */
    @Test
    fun testStationaryPhone() {
        val traceFileName = "trace_e739b57d-9d4d-40e0-924b-7521027eb77d.csv"
        val allEvents = try {
            SensorReplayer
                .fromRawTraceResource("src/test/resources/raw_traces/$traceFileName")
                .getEvents()
        } catch (e: Exception) {
            println("FAILED to load trace: ${e.message}")
            return
        }

        val resultsFile =
            File("/Users/francoismonniot/Projects/github.com/fmonniot/Speed/app/calibration_results.txt")

        resultsFile.writeText("=== Calibration Analysis for $traceFileName ===\n\n")

        // 1. Raw Data Stats & Range Analysis
        val imuEvents = allEvents.filterIsInstance<RawEvent.Imu>()
        val gpsEvents = allEvents.filterIsInstance<RawEvent.Gps>()
        val statusEvents = allEvents.filterIsInstance<RawEvent.Status>()

        resultsFile.appendText("Raw Event Statistics:\n")
        resultsFile.appendText("- Total Events: ${allEvents.size}\n")
        if (allEvents.isNotEmpty()) {
            resultsFile.appendText("- Trace Duration (Raw): %.2f seconds\n"
                .format((allEvents.last().timestampNs - allEvents.first().timestampNs) / 1_000_000_000f))
        }
        if (imuEvents.isNotEmpty()) {
            resultsFile.appendText("- IMU Events: ${imuEvents.size} | Range: ${imuEvents.first().timestampNs} to ${imuEvents.last().timestampNs} (%.2f s)\n"
                .format((imuEvents.last().timestampNs - imuEvents.first().timestampNs) / 1_000_000_000f))
        }
        if (gpsEvents.isNotEmpty()) {
            resultsFile.appendText("- GPS Events: ${gpsEvents.size} | Range: ${gpsEvents.first().timestampNs} to ${gpsEvents.last().timestampNs} (%.2f s)\n"
                .format((gpsEvents.last().timestampNs - gpsEvents.first().timestampNs) / 1_000_000_000f))
        }
        if (statusEvents.isNotEmpty()) {
            resultsFile.appendText("- Status Events: ${statusEvents.size} | Range: ${statusEvents.first().timestampNs} to ${statusEvents.last().timestampNs}\n")
            resultsFile.appendText("  (Note: Corrupted timestamps detected in legacy traces for Status events)\n")
        }

        // Timeline Normalization: We exclude outliers (Status events using wrong clock epoch)
        val baseTs = imuEvents.firstOrNull()?.timestampNs ?: 0L
        val events = allEvents.filter {
            it !is RawEvent.Status && Math.abs(it.timestampNs - baseTs) < 3600_000_000_000L
        }.sortedBy { it.timestampNs }

        resultsFile.appendText("\nFinal Filtered Duration: %.2f seconds\n\n"
            .format((events.last().timestampNs - events.first().timestampNs) / 1_000_000_000f))

        // 2. Threshold Context
        resultsFile.appendText("Active Thresholds (from VelocityFusion):\n")
        resultsFile.appendText("- ZUPT IMU Mag: ${VelocityFusion.ZUPT_IMU_MAGNITUDE_THRESHOLD} m/s²\n")
        resultsFile.appendText("- ZUPT IMU Var: ${VelocityFusion.ZUPT_IMU_VARIANCE_THRESHOLD}\n")
        resultsFile.appendText("- ZUPT GPS Speed: ${VelocityFusion.ZUPT_GPS_SPEED_THRESHOLD_MS} m/s\n")
        resultsFile.appendText("- GPS Max Age: ${VelocityFusion.GPS_MAX_AGE_MS} ms\n\n")

        // 3. Grid Search Results
        val qValues = listOf(0.01f, 0.05f, 0.1f, 0.5f)
        val rValues = listOf(0.1f, 0.3f, 0.5f, 1.0f)

        resultsFile.appendText("|  Q   |  R   | Max Speed (km/h) | Avg Speed (km/h) | ZUPT % |\n")
        resultsFile.appendText("|------|------|------------------|------------------|--------|\n")

        for (q in qValues) {
            for (r in rValues) {
                val fusion = VelocityFusion(kalmanQ = q, kalmanR = r)
                val results = runFusion(events, fusion)

                val speeds = results.map { it.speedMs }
                val maxSpeedKmh = (speeds.maxOrNull() ?: 0f) * 3.6f
                val avgSpeedKmh = if (speeds.isNotEmpty()) speeds.average().toFloat() * 3.6f else 0f
                val zuptPercent = if (results.isNotEmpty()) (results.count { it.isStationary }
                    .toFloat() / results.size) * 100f else 0f

                resultsFile.appendText(
                    "| %.2f | %.2f |     %.6f     |     %.6f     |  %.1f%%  |\n"
                        .format(q, r, maxSpeedKmh, avgSpeedKmh, zuptPercent)
                )
            }
        }

        resultsFile.appendText("\n--- ZUPT Diagnostic (First 20 valid ticks) for Default Config ---\n")
        runDiagnostic(events, VelocityFusion(0.5f, 0.3f), resultsFile)
    }

    private data class TickResult(val speedMs: Float, val isStationary: Boolean)

    private fun runFusion(events: List<RawEvent>, fusion: VelocityFusion): List<TickResult> {
        val output = mutableListOf<TickResult>()
        if (events.isEmpty()) return output

        var lastTickTimeNs = events.first().timestampNs
        val rotationMatrix = FloatArray(9)
        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()

        val tickIntervalNs = 100_000_000L
        var nextTickNs = lastTickTimeNs + tickIntervalNs

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(
                        event.rotationVector,
                        rotationMatrix
                    )
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }

                is RawEvent.Gps -> {
                    latestGps = event
                }

                else -> {}
            }

            while (event.timestampNs >= nextTickNs) {
                val dt = (nextTickNs - lastTickTimeNs) / 1_000_000_000f
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (nextTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(dt, imuWindow, gpsObs)
                output.add(TickResult(result.speedMs, result.isStationary))

                lastTickTimeNs = nextTickNs
                nextTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
            }
        }
        return output
    }

    private fun runDiagnostic(events: List<RawEvent>, fusion: VelocityFusion, resultsFile: File) {
        if (events.isEmpty()) return

        var lastTickTimeNs = events.first().timestampNs
        val rotationMatrix = FloatArray(9)
        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()
        val tickIntervalNs = 100_000_000L
        var nextTickNs = lastTickTimeNs + tickIntervalNs
        var tickCount = 0

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(
                        event.rotationVector,
                        rotationMatrix
                    )
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }

                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= nextTickNs && tickCount < 20) {
                val sampleCount = imuSamplesInWindow.size
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (nextTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(0.1f, imuWindow, gpsObs)

                val imuMag = imuWindow?.accelMagnitude ?: -1f
                val imuVar = imuWindow?.variance ?: -1f
                val gpsSpeed = gpsObs?.speedMs ?: -1f

                resultsFile.appendText(
                    "Tick %2d: Speed=%.4f, ZUPT=%b | Samples=%d, IMU_Mag=%.4f, IMU_Var=%.6f, GPS_Speed=%.4f\n"
                        .format(
                            tickCount,
                            result.speedMs,
                            result.isStationary,
                            sampleCount,
                            imuMag,
                            imuVar,
                            gpsSpeed
                        )
                )

                lastTickTimeNs = nextTickNs
                nextTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
                tickCount++
            }
        }
    }
}
