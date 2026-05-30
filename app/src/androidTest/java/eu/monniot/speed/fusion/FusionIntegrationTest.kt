package eu.monniot.speed.fusion

import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.monniot.speed.sensor.ImuSample
import eu.monniot.speed.sensor.SatelliteInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FusionIntegrationTest {

    @Test
    fun testDataFusionIntegration() = runTest {
        val gpsFlow = MutableSharedFlow<Location>()
        val satellitesFlow = MutableStateFlow(SatelliteInfo(8, 12))
        val imuFlow = MutableSharedFlow<ImuSample>()
        
        val fusion = DataFusion(gpsFlow, satellitesFlow, imuFlow, this)
        fusion.start()
        
        // 1. Send some IMU data
        val t0 = android.os.SystemClock.elapsedRealtimeNanos()
        imuFlow.emit(ImuSample(floatArrayOf(0f, 1f, 0f), t0))
        
        // 2. Send GPS data
        val mockLocation = mock(Location::class.java)
        `when`(mockLocation.speed).thenReturn(5f)
        `when`(mockLocation.bearing).thenReturn(0f)
        `when`(mockLocation.accuracy).thenReturn(2f)
        `when`(mockLocation.latitude).thenReturn(45.0)
        `when`(mockLocation.longitude).thenReturn(5.0)
        `when`(mockLocation.altitude).thenReturn(100.0)
        `when`(mockLocation.elapsedRealtimeNanos).thenReturn(t0)
        gpsFlow.emit(mockLocation)
        
        // Collect first output
        val firstPoint = fusion.dataPointFlow.first()
        
        assertEquals(8, firstPoint.satellitesUsed)
        assertTrue("Speed should be positive", (firstPoint.derivedSpeedMs ?: 0f) >= 0f)
    }
}
