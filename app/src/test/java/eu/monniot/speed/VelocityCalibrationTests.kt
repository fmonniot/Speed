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

        val resultsFile = File("/Users/francoismonniot/Projects/github.com/fmonniot/Speed/app/calibration_results.txt")
        resultsFile.writeText("Calibration Analysis for $traceFileName (Stationary Phone Ground Truth)\n")
        resultsFile.appendText("Total Events: ${events.size}\n")
        
        val imuCount = events.count { it is RawEvent.Imu }
        val gpsCount = events.count { it is RawEvent.Gps }
        resultsFile.appendText("IMU Events: $imuCount, GPS Events: $gpsCount\n")
        
        if (events.isNotEmpty()) {
            resultsFile.appendText("Trace Duration: %.2f seconds\n"
                .format((events.last().timestampNs - events.first().timestampNs) / 1_000_000_000f))
        }
        resultsFile.appendText("\n")

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
                val zuptPercent = if (results.isNotEmpty()) (results.count { it.isStationary }.toFloat() / results.size) * 100f else 0f

                resultsFile.appendText("| %.2f | %.2f |     %.6f     |     %.6f     |  %.1f%%  |\n"
                    .format(q, r, maxSpeedKmh, avgSpeedKmh, zuptPercent))
            }
        }
        
        // Detailed Diagnostic for Default Config
        resultsFile.appendText("\n--- ZUPT Diagnostic (First 50 ticks) for Default (Q=0.5, R=0.3) ---\n")
        resultsFile.appendText("Thresholds: Mag < ${VelocityFusion.ZUPT_IMU_MAGNITUDE_THRESHOLD}, Var < ${VelocityFusion.ZUPT_IMU_VARIANCE_THRESHOLD}, GPS < ${VelocityFusion.ZUPT_GPS_SPEED_THRESHOLD_MS}\n")
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
                    CoordinateTransformer.getRotationMatrixFromVector(event.rotationVector, rotationMatrix)
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }
                is RawEvent.Gps -> {
                    latestGps = event
                }
                else -> {}
            }

            // Trigger ticks for all 100ms intervals passed by this event's timestamp
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
                    CoordinateTransformer.getRotationMatrixFromVector(event.rotationVector, rotationMatrix)
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }
                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= nextTickNs && tickCount < 50) {
                val dt = (nextTickNs - lastTickTimeNs) / 1_000_000_000f
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

                val result = fusion.tick(dt, imuWindow, gpsObs)
                
                val imuMag = imuWindow?.accelMagnitude ?: -1f
                val imuVar = imuWindow?.variance ?: -1f
                val gpsSpeed = gpsObs?.speedMs ?: -1f
                
                resultsFile.appendText("Tick %2d: Speed=%.4f, ZUPT=%b | Samples=%d, IMU_Mag=%.4f, IMU_Var=%.6f, GPS_Speed=%.4f\n"
                    .format(tickCount, result.speedMs, result.isStationary, sampleCount, imuMag, imuVar, gpsSpeed))

                lastTickTimeNs = nextTickNs
                nextTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
                tickCount++
            }
        }
    }
}
