package eu.monniot.speed

import eu.monniot.speed.fusion.CoordinateTransformer
import eu.monniot.speed.fusion.GpsObservation
import eu.monniot.speed.fusion.ImuWindow
import eu.monniot.speed.fusion.VelocityFusion
import eu.monniot.speed.sensor.ImuSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class VelocityCalibrationTests {

    /**
     * This test run against raw traces where the phone was sitting on a desktop. It wasn't
     * moving to the best of my ability (e.g. may have some acceleration when interacting
     * with the side button).
     */
    @Test
    fun testStationaryPhone() {
        val traceFileName = "trace_e739b57d-9d4d-40e0-924b-7521027eb77d.csv"
        val events = SensorReplayer
            .fromRawTraceResource("src/test/resources/raw_traces/$traceFileName")
            .getEvents()

        // Initializing with default car-tuning parameters
        val fusion = VelocityFusion(kalmanQ = 0.5f, kalmanR = 0.3f)

        var lastTimestampNs = events.first().timestampNs
        
        // Reusable buffers for rotation
        val rotationMatrix = FloatArray(9)
        var lastRotationVector: FloatArray? = null
        var latestGps: RawEvent.Gps? = null

        val imuSamplesInWindow = mutableListOf<ImuSample>()
        val outputSpeeds = mutableListOf<Float>()
        val outputAccels = mutableListOf<Float>()

        // Simulate 100ms ticks as defined in the app's requirements
        val tickIntervalNs = 100_000_000L
        var currentTickNs = lastTimestampNs + tickIntervalNs

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    // 1. Update rotation state
                    lastRotationVector = event.rotationVector
                    CoordinateTransformer.getRotationMatrixFromVector(event.rotationVector, rotationMatrix)
                    
                    // 2. Transform device accel to world frame using the same logic as the app
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }
                is RawEvent.Gps -> {
                    latestGps = event
                }
                else -> {}
            }

            // Process one or more ticks if enough time has passed
            while (event.timestampNs >= currentTickNs) {
                val dt = (currentTickNs - lastTimestampNs) / 1_000_000_000f

                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)

                val gpsObs = latestGps?.let {
                    val ageMs = (currentTickNs - it.timestampNs) / 1_000_000L
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = ageMs
                    )
                }

                val result = fusion.tick(dt, imuWindow, gpsObs)
                outputSpeeds.add(result.speedMs)
                result.derivedAccelMs2?.let { outputAccels.add(it) }

                // Advance state for next tick
                lastTimestampNs = currentTickNs
                currentTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
            }
        }

        println("Replay Summary for $traceFileName:")
        println("- Events processed: ${events.size}")
        println("- Fusion ticks generated: ${outputSpeeds.size}")
        if (outputSpeeds.isNotEmpty()) {
            println("- Max Speed: ${outputSpeeds.maxOrNull()?.let { it * 3.6f }} km/h")
            println("- Avg Accel: ${outputAccels.average()} m/s²")
        }

        // --- Assertions ---
        assertTrue("Fusion should have produced output points", outputSpeeds.isNotEmpty())
        val maxPossibleSpeed = 100f // 360 km/h
        assertTrue("Speed estimate exceeds physical reality: ${outputSpeeds.maxOrNull()}", (outputSpeeds.maxOrNull() ?: 0f) < maxPossibleSpeed)
        assertFalse("Speed contains NaN", outputSpeeds.any { it.isNaN() })
    }
}
