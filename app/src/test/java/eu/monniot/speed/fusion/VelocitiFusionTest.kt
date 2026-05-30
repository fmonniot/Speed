package eu.monniot.speed.fusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class VelocitiFusionTest {

    @Test
    fun `Kalman Filter Convergence - static GPS and IMU`() {
        val fusion = VelocityFusion()
        val dt = 0.1f
        
        // Target speed: 10 m/s North (bearing 0)
        val targetSpeed = 10f
        val gps = GpsObservation(
            speedMs = targetSpeed,
            bearingRad = 0f,
            accuracyM = 5f,
            ageMs = 0
        )
        
        // No acceleration
        val imu = ImuWindow(0f, 0f, 0f, 0.01f)
        
        // Feed the filter for 5 seconds (50 ticks)
        repeat(50) {
            fusion.tick(dt, imu, gps)
        }
        
        val result = fusion.tick(dt, imu, gps)
        
        // Should converge to target speed
        assertEquals(targetSpeed, result.speedMs, 0.1f)
        assertTrue(result.gpsWasUsed)
    }

    @Test
    fun `Predict Phase - acceleration advances velocity`() {
        val fusion = VelocityFusion()
        
        // Constant acceleration: 1 m/s² North
        val imu = ImuWindow(accelX = 0f, accelY = 1f, accelZ = 0f, variance = 0.1f)
        
        // Tick 1: dt = 0.1s
        val result1 = fusion.tick(dt = 0.1f, imu = imu, gps = null)
        // v = u + at = 0 + 1 * 0.1 = 0.1
        assertEquals(0.1f, result1.speedMs, 0.01f)
        
        // Tick 2: dt = 0.2s (dynamic dt)
        val result2 = fusion.tick(dt = 0.2f, imu = imu, gps = null)
        // v = 0.1 + 1 * 0.2 = 0.3
        assertEquals(0.3f, result2.speedMs, 0.01f)
    }

    @Test
    fun `Update Phase - GPS corrects IMU drift`() {
        val fusion = VelocityFusion()
        
        // 1. Simulate drift: IMU says we are accelerating, but we aren't (GPS says 0)
        val imu = ImuWindow(0f, 1f, 0f, 0.1f) // 1 m/s² North drift
        
        repeat(10) {
            fusion.tick(0.1f, imu, gps = null)
        }
        
        // After 1s of 1m/s² drift, speed should be ~1.0m/s
        val driftResult = fusion.tick(0.1f, imu, gps = null)
        assertEquals(1.1f, driftResult.speedMs, 0.1f)
        
        // 2. Apply fresh GPS fix saying we are actually at 0 m/s
        val gps = GpsObservation(speedMs = 0f, bearingRad = 0f, accuracyM = 5f, ageMs = 0)
        
        // A single GPS update should significantly pull the estimate towards 0
        val correctedResult = fusion.tick(0.1f, imu, gps)
        
        assertTrue(correctedResult.speedMs < driftResult.speedMs)
        assertTrue(correctedResult.gpsWasUsed)
    }

    @Test
    fun `Update Phase - GPS gating by age and accuracy`() {
        val fusion = VelocityFusion()
        
        // Inaccurate GPS (> 20m)
        val inaccurateGps = GpsObservation(speedMs = 10f, bearingRad = 0f, accuracyM = 50f, ageMs = 0)
        val res1 = fusion.tick(0.1f, null, inaccurateGps)
        assertFalse(res1.gpsWasUsed)
        assertEquals(0f, res1.speedMs, 0.001f)
        
        // Stale GPS (> 300ms)
        val staleGps = GpsObservation(speedMs = 10f, bearingRad = 0f, accuracyM = 5f, ageMs = 500)
        val res2 = fusion.tick(0.1f, null, staleGps)
        assertFalse(res2.gpsWasUsed)
        assertEquals(0f, res2.speedMs, 0.001f)
        
        // Good GPS
        val goodGps = GpsObservation(speedMs = 10f, bearingRad = 0f, accuracyM = 5f, ageMs = 0)
        val res3 = fusion.tick(0.1f, null, goodGps)
        assertTrue(res3.gpsWasUsed)
        assertTrue(res3.speedMs > 0f)
    }

    @Test
    fun `ZUPT - Snaps to zero when stationary`() {
        val fusion = VelocityFusion()
        
        // 1. Get some speed going
        val gpsMoving = GpsObservation(speedMs = 10f, bearingRad = 0f, accuracyM = 5f, ageMs = 0)
        repeat(10) { fusion.tick(0.1f, null, gpsMoving) }
        
        val resultBefore = fusion.tick(0.1f, null, gpsMoving)
        assertTrue(resultBefore.speedMs > 5f)
        
        // 2. Stationary conditions: Low IMU magnitude, low IMU variance, low GPS speed
        val imuStill = ImuWindow(accelX = 0.01f, accelY = 0.01f, accelZ = 0.01f, variance = 0.001f)
        val gpsStill = GpsObservation(speedMs = 0.1f, bearingRad = 0f, accuracyM = 5f, ageMs = 0)
        
        // Give it a few ticks to fully snap to zero
        repeat(5) {
            fusion.tick(0.1f, imuStill, gpsStill)
        }
        val resultAfter = fusion.tick(0.1f, imuStill, gpsStill)
        
        assertTrue(resultAfter.isStationary)
        // ZUPT uses very low R (0.01), so it should snap to ~0 quickly
        assertEquals(0f, resultAfter.speedMs, 0.05f)
    }

    @Test
    fun `Timing Logic - integration is correct with varying dt`() {
        val fusion = VelocityFusion()
        
        // Constant acceleration 1 m/s²
        val imu = ImuWindow(0f, 1f, 0f, 0.1f)
        
        // 10 ticks of 0.1s = 1.0s total, expected speed = 1.0 m/s
        repeat(10) { fusion.tick(0.1f, imu, null) }
        // Use 0s tick to check current estimate without advancing
        assertEquals(1.0f, fusion.tick(0f, null, null).speedMs, 0.01f)
        
        fusion.reset()
        
        // 5 ticks of 0.2s = 1.0s total, expected speed = 1.0 m/s
        repeat(5) { fusion.tick(0.2f, imu, null) }
        assertEquals(1.0f, fusion.tick(0f, null, null).speedMs, 0.01f)
        
        fusion.reset()
        
        // Mixed dts: 0.1, 0.5, 0.4 = 1.0s total
        fusion.tick(0.1f, imu, null)
        fusion.tick(0.5f, imu, null)
        fusion.tick(0.4f, imu, null)
        assertEquals(1.0f, fusion.tick(0f, null, null).speedMs, 0.01f)
    }
}
