package eu.monniot.speed.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import eu.monniot.speed.sensor.GpsCollector
import eu.monniot.speed.sensor.ImuCollector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RaceRecordingServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

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
        Thread.sleep(1000) 
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

        Thread.sleep(1000)
        assertFalse(RaceRecordingService.state.value.isRecording)
        
        // Note: WakeLock might be released asynchronously or after some cleanup
        // But it should eventually be released.
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
        
        Thread.sleep(1000)
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
        
        Thread.sleep(1000)
        assertFalse(RaceRecordingService.state.value.isSensorsEnabled)
        assertFalse("GPS Collector should be inactive", gpsCollector.isActive.value)
        assertFalse("IMU Collector should be inactive", imuCollector.isActive.value)
    }
}
