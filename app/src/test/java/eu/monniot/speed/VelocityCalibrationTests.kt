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

    @Test
    fun testStationaryPhone() {
        val traceFileName = "trace_e739b57d-9d4d-40e0-924b-7521027eb77d.csv"
        val events = try {
            SensorReplayer
                .fromRawTraceResource("src/test/resources/raw_traces/$traceFileName")
                .getEvents()
        } catch (e: Exception) {
            println("FAILED to load trace: ${e.message}")
            return
        }

        // Output to absolute path to avoid missing it
        val resultsFile = File("/Users/francoismonniot/Projects/github.com/fmonniot/Speed/app/calibration_results.txt")
        resultsFile.writeText("Calibration Analysis for $traceFileName (Stationary Phone Ground Truth)\n")
        resultsFile.appendText("Total Events: ${events.size}\n")
        
        val imuCount = events.count { it is RawEvent.Imu }
        val gpsCount = events.count { it is RawEvent.Gps }
        resultsFile.appendText("IMU Events: $imuCount, GPS Events: $gpsCount\n\n")

        if (imuCount == 0 || gpsCount == 0) {
            resultsFile.appendText("ERROR: Missing sensor data in trace!\n")
            return
        }

        val gpsEvents = events.filterIsInstance<RawEvent.Gps>()
        resultsFile.appendText("Raw GPS Stats:\n")
        resultsFile.appendText("- Max GPS Speed: %.6f m/s (%.4f km/h)\n".format(gpsEvents.maxOf { it.speedMs }, gpsEvents.maxOf { it.speedMs } * 3.6f))
        resultsFile.appendText("- Avg GPS Speed: %.6f m/s\n\n".format(gpsEvents.map { it.speedMs }.average()))

        val qValues = listOf(0.01f, 0.05f, 0.1f, 0.5f)
        val rValues = listOf(0.1f, 0.3f, 0.5f, 1.0f)

        resultsFile.appendText("| Q | R | Max Speed (km/h) | Avg Speed (km/h) | ZUPT % |\n")
        resultsFile.appendText("|---|---|---|---|---|\n")

        for (q in qValues) {
            for (r in rValues) {
                val fusion = VelocityFusion(kalmanQ = q, kalmanR = r)
                val results = runFusion(events, fusion)
                
                val speeds = results.map { it.speedMs }
                val maxSpeedKmh = (speeds.maxOrNull() ?: 0f) * 3.6f
                val avgSpeedKmh = if (speeds.isNotEmpty()) speeds.average().toFloat() * 3.6f else 0f
                val zuptPercent = if (results.isNotEmpty()) (results.count { it.isStationary }.toFloat() / results.size) * 100f else 0f

                val output = "| %.2f | %.2f | %.6f | %.6f | %.1f%% |\n"
                    .format(q, r, maxSpeedKmh, avgSpeedKmh, zuptPercent)
                resultsFile.appendText(output)
            }
        }
        
        // Detailed Diagnostic for Default
        resultsFile.appendText("\nZUPT Diagnostic (First 20 ticks) for Default (Q=0.5, R=0.3):\n")
        runDiagnostic(events, VelocityFusion(0.5f, 0.3f), resultsFile)
    }

    private data class TickResult(val speedMs: Float, val isStationary: Boolean)

    private fun runFusion(events: List<RawEvent>, fusion: VelocityFusion): List<TickResult> {
        val output = mutableListOf<TickResult>()
        var lastTimestampNs = events.first().timestampNs
        val rotationMatrix = FloatArray(9)
        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()

        val tickIntervalNs = 100_000_000L
        var currentTickNs = lastTimestampNs + tickIntervalNs

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(event.rotationVector, rotationMatrix)
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }
                is RawEvent.Gps -> {
                    latestGps = event
                }
                else -> {}
            }

            while (event.timestampNs >= currentTickNs) {
                val dt = (currentTickNs - lastTimestampNs) / 1_000_000_000f
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (currentTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(dt, imuWindow, gpsObs)
                output.add(TickResult(result.speedMs, result.isStationary))

                lastTimestampNs = currentTickNs
                currentTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
            }
        }
        return output
    }

    private fun runDiagnostic(events: List<RawEvent>, fusion: VelocityFusion, resultsFile: File) {
        var lastTimestampNs = events.first().timestampNs
        val rotationMatrix = FloatArray(9)
        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()
        val tickIntervalNs = 100_000_000L
        var currentTickNs = lastTimestampNs + tickIntervalNs
        var tickCount = 0

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(event.rotationVector, rotationMatrix)
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }
                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= currentTickNs && tickCount < 20) {
                val dt = (currentTickNs - lastTimestampNs) / 1_000_000_000f
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (currentTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(dt, imuWindow, gpsObs)
                
                val imuMag = imuWindow?.accelMagnitude ?: 0f
                val imuVar = imuWindow?.variance ?: 0f
                val gpsSpeed = gpsObs?.speedMs ?: -1f
                
                resultsFile.appendText("Tick %d: Speed=%.4f, ZUPT=%b | IMU_Mag=%.4f (thr 0.3), IMU_Var=%.6f (thr 0.02), GPS_Speed=%.4f (thr 0.3)\n"
                    .format(tickCount, result.speedMs, result.isStationary, imuMag, imuVar, gpsSpeed))

                lastTimestampNs = currentTickNs
                currentTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
                tickCount++
            }
        }
    }
}
