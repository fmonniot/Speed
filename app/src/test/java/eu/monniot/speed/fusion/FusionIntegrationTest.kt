package eu.monniot.speed.fusion

import android.location.Location
import eu.monniot.speed.sensor.ImuSample
import eu.monniot.speed.sensor.SatelliteInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class FusionIntegrationTest {

    @Test
    fun `DataFusion - integrates IMU and GPS into DataPoints`() = runTest {
        val gpsFlow = MutableSharedFlow<Location>()
        val satellitesFlow = MutableStateFlow(SatelliteInfo(8, 12))
        val imuFlow = MutableSharedFlow<ImuSample>()
        
        // Using a test scope to control time
        val fusion = DataFusion(gpsFlow, satellitesFlow, imuFlow, this)
        fusion.start()
        
        val results = mutableListOf<eu.monniot.speed.data.DataPoint>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fusion.dataPointFlow.collect {
                results.add(it)
            }
        }
        
        // 1. Send some IMU data
        val t0 = 1000L
        imuFlow.emit(ImuSample(floatArrayOf(0f, 1f, 0f), t0))
        
        // 2. Send GPS data
        val mockLocation = mock(Location::class.java)
        `when`(mockLocation.speed).thenReturn(5f)
        `when`(mockLocation.bearing).thenReturn(0f)
        `when`(mockLocation.accuracy).thenReturn(2f)
        `when`(mockLocation.elapsedRealtimeNanos).thenReturn(t0)
        gpsFlow.emit(mockLocation)
        
        // Wait for at least one tick of the 100ms loop
        // Since DataFusion uses Dispatchers.Default and real delay(), 
        // we might still need a small real-world wait unless we refactor DataFusion to accept a Dispatcher.
        // For this test, let's just wait a bit.
        
        kotlinx.coroutines.delay(300)
        
        assertTrue("Should have collected data points", results.isNotEmpty())
        val lastPoint = results.last()
        assertEquals(8, lastPoint.satellitesUsed)
        
        job.cancel()
    }
}
