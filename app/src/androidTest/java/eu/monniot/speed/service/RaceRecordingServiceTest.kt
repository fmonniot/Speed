package eu.monniot.speed.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import eu.monniot.speed.sensor.GpsCollector
import eu.monniot.speed.sensor.ImuCollector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RaceRecordingServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    /**
     * Polls [condition] until it becomes true or [timeoutMs] elapses, instead of a flat
     * [Thread.sleep]. Service state updates arrive asynchronously off the fusion loop, so a fixed
     * sleep is either too short (flaky) or needlessly slow.
     */
    private fun awaitCondition(
        timeoutMs: Long = 5_000,
        intervalMs: Long = 50,
        message: String = "Condition not met within ${timeoutMs}ms",
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(intervalMs)
        }
        assertTrue(message, condition())
    }

    @Test
    fun testServiceStartStopRecordingAndWakeLock() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, RaceRecordingService::class.java)

        // Bind the service to access its instance
        val binder = serviceRule.bindService(intent) as RaceRecordingService.LocalBinder
        val service = binder.getService()

        // Initially not recording
        assertFalse(RaceRecordingService.state.value.isRecording)

        // Start recording
        val startIntent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_START_RECORDING
        }
        context.startService(startIntent)

        // Wait for state update
        awaitCondition { RaceRecordingService.state.value.isRecording }
        assertTrue(RaceRecordingService.state.value.isRecording)

        // Verify WakeLock is held via reflection
        val wakeLockField = RaceRecordingService::class.java.getDeclaredField("wakeLock")
        wakeLockField.isAccessible = true
        val wakeLock = wakeLockField.get(service) as PowerManager.WakeLock?
        assertTrue("WakeLock should be acquired during recording", wakeLock?.isHeld == true)

        // Stop recording
        val stopIntent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_STOP
        }
        context.startService(stopIntent)

        awaitCondition { !RaceRecordingService.state.value.isRecording }
        assertFalse(RaceRecordingService.state.value.isRecording)

        // WakeLock is released asynchronously during cleanup; poll until it is.
        awaitCondition(message = "WakeLock should be released after recording") { wakeLock?.isHeld == false }
        assertTrue("WakeLock should be released after recording", wakeLock?.isHeld == false)
    }

    @Test
    fun testSensorsToggleAndCollection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, RaceRecordingService::class.java)

        val binder = serviceRule.bindService(intent) as RaceRecordingService.LocalBinder
        val service = binder.getService()

        // Start sensors
        val startSensorsIntent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_START_SENSORS
        }
        context.startService(startSensorsIntent)

        awaitCondition { RaceRecordingService.state.value.isSensorsEnabled }
        assertTrue(RaceRecordingService.state.value.isSensorsEnabled)

        // Verify collectors are active via reflection
        val gpsField = RaceRecordingService::class.java.getDeclaredField("gpsCollector")
        gpsField.isAccessible = true
        val gpsCollector = gpsField.get(service) as GpsCollector
        assertTrue("GPS Collector should be active", gpsCollector.isActive.value)

        val imuField = RaceRecordingService::class.java.getDeclaredField("imuCollector")
        imuField.isAccessible = true
        val imuCollector = imuField.get(service) as ImuCollector
        assertTrue("IMU Collector should be active", imuCollector.isActive.value)
        
        // Stop sensors
        val stopSensorsIntent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_STOP_SENSORS
        }
        context.startService(stopSensorsIntent)

        awaitCondition { !RaceRecordingService.state.value.isSensorsEnabled }
        assertFalse(RaceRecordingService.state.value.isSensorsEnabled)
        assertFalse("GPS Collector should be inactive", gpsCollector.isActive.value)
        assertFalse("IMU Collector should be inactive", imuCollector.isActive.value)

        // Regression guard for the a3bade2 fix: stopping sensors must clear the last-known
        // satellite/accuracy readings so the HUD doesn't keep showing stale fix data.
        val state = RaceRecordingService.state.value
        assertEquals(0, state.satellites.usedInFix)
        assertEquals(0, state.satellites.visible)
        assertNull(state.currentAccuracyM)
    }
}
