package eu.monniot.speed.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import eu.monniot.speed.sensor.GpsCollector
import eu.monniot.speed.sensor.ImuCollector
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RaceRecordingServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * These instrumented tests share one process, and [RaceRecordingService.state] is backed by a
     * process-static flow whose transitions happen asynchronously (collectors/wakelock/foreground
     * start off the main thread). Reset that flow before each test so state leaked from a prior
     * test can't make this test's assertions pass or fail spuriously.
     */
    @Before
    fun resetServiceState() = writeState(ServiceState())

    /**
     * Tear the service down between tests. A test that fails mid-method would otherwise leave the
     * service recording with sensors on (and a pending start coroutine), contaminating the next
     * test. Stopping sensors here also stops any active recording (see [stopSensors]).
     */
    @After
    fun tearDownService() {
        sendAction(RaceRecordingService.ACTION_STOP_SENSORS)
        // Let the stop propagate, then reset so the next test's @Before starts from a clean slate.
        Thread.sleep(200)
        writeState(ServiceState())
    }

    @Test
    fun testServiceStartStopRecordingAndWakeLock() {
        val service = bindAndGetService()

        assertFalse(RaceRecordingService.state.value.isRecording)

        sendAction(RaceRecordingService.ACTION_START_RECORDING)
        awaitCondition { RaceRecordingService.state.value.isRecording }

        // WakeLock is acquired on the service thread after isRecording flips (after startForeground),
        // so poll the field rather than asserting the instant we observe isRecording — on a slow
        // emulator the acquire lags the state update.
        val wakeLockField = RaceRecordingService::class.java.getDeclaredField("wakeLock").apply {
            isAccessible = true
        }
        fun wakeLock() = wakeLockField.get(service) as PowerManager.WakeLock?
        awaitCondition("WakeLock should be acquired during recording") { wakeLock()?.isHeld == true }

        sendAction(RaceRecordingService.ACTION_STOP)
        awaitCondition { !RaceRecordingService.state.value.isRecording }
        awaitCondition("WakeLock should be released after recording") { wakeLock()?.isHeld != true }
    }

    @Test
    fun testSensorsToggleAndCollection() {
        val service = bindAndGetService()

        sendAction(RaceRecordingService.ACTION_START_SENSORS)
        awaitCondition { RaceRecordingService.state.value.isSensorsEnabled }

        val gpsCollector = readField<GpsCollector>(service, "gpsCollector")
        val imuCollector = readField<ImuCollector>(service, "imuCollector")
        awaitCondition("GPS Collector should be active") { gpsCollector.isActive.value }
        awaitCondition("IMU Collector should be active") { imuCollector.isActive.value }

        sendAction(RaceRecordingService.ACTION_STOP_SENSORS)
        awaitCondition { !RaceRecordingService.state.value.isSensorsEnabled }
        awaitCondition("GPS Collector should be inactive") { !gpsCollector.isActive.value }
        awaitCondition("IMU Collector should be inactive") { !imuCollector.isActive.value }

        // Regression guard for the a3bade2 fix: stopping sensors must clear the last-known
        // satellite/accuracy readings so the HUD doesn't keep showing stale fix data.
        val state = RaceRecordingService.state.value
        assertEquals(0, state.satellites.usedInFix)
        assertEquals(0, state.satellites.visible)
        assertNull(state.currentAccuracyM)
    }

    // ---- helpers ----

    private fun bindAndGetService(): RaceRecordingService {
        val binder = serviceRule.bindService(
            Intent(context, RaceRecordingService::class.java)
        ) as RaceRecordingService.LocalBinder
        return binder.getService()
    }

    private fun sendAction(action: String) {
        context.startService(Intent(context, RaceRecordingService::class.java).apply {
            this.action = action
        })
    }

    private fun writeState(value: ServiceState) {
        val field = RaceRecordingService::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(null) as MutableStateFlow<ServiceState>).value = value
    }

    private inline fun <reified T> readField(service: RaceRecordingService, name: String): T {
        val field = RaceRecordingService::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(service) as T
    }

    /**
     * Polls [condition] until it becomes true or [timeoutMs] elapses, instead of a flat
     * [Thread.sleep]. The timeout is generous because CI emulators (ATD on a KVM runner) are much
     * slower than a local device at processing startService/foreground transitions.
     */
    private fun awaitCondition(
        message: String = "Condition not met in time",
        timeoutMs: Long = 10_000,
        intervalMs: Long = 50,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(intervalMs)
        }
        assertTrue(message, condition())
    }
}
