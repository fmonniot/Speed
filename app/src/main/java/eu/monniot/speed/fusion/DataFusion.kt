package eu.monniot.speed.fusion

import android.location.Location
import android.os.SystemClock
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.sensor.ImuSample
import eu.monniot.speed.sensor.SatelliteInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DataFusion(
    private val gpsFlow: SharedFlow<Location>,
    private val satellitesFlow: StateFlow<SatelliteInfo>,
    private val imuFlow: SharedFlow<ImuSample>,
    private val scope: CoroutineScope
) {
    private val _dataPointFlow = MutableSharedFlow<DataPoint>(extraBufferCapacity = 32)
    val dataPointFlow: SharedFlow<DataPoint> = _dataPointFlow

    var currentSessionId: String? = null
        set(value) {
            if (value != field && value != null) {
                velocityFusion.reset()
            }
            field = value
        }

    private val velocityFusion = VelocityFusion()
    private var lastGpsLocation: Location? = null
    private var lastTickTimeNs: Long = 0L
    
    private val imuSamples = mutableListOf<ImuSample>()

    fun start() {
        scope.launch(Dispatchers.Default) {
            // Collect IMU samples in real-time
            launch {
                imuFlow.collect { 
                    synchronized(imuSamples) {
                        imuSamples.add(it)
                    }
                }
            }

            // Collect GPS locations
            launch {
                gpsFlow.collect { lastGpsLocation = it }
            }

            lastTickTimeNs = SystemClock.elapsedRealtimeNanos()

            // 100ms Loop
            while (isActive) {
                val tickStartNs = SystemClock.elapsedRealtimeNanos()
                val dt = (tickStartNs - lastTickTimeNs) / 1_000_000_000f
                lastTickTimeNs = tickStartNs

                val currentImuSamples = synchronized(imuSamples) {
                    val samples = imuSamples.toList()
                    imuSamples.clear()
                    samples
                }

                val imuWindow = ImuWindow.fromSamples(currentImuSamples)

                val gps = lastGpsLocation
                val gpsObs = gps?.let {
                    val ageMs = (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000L
                    GpsObservation(
                        speedMs = it.speed,
                        bearingRad = it.bearing * (Math.PI.toFloat() / 180f),
                        accuracyM = it.accuracy,
                        ageMs = ageMs
                    )
                }

                val fused = velocityFusion.tick(dt, imuWindow, gpsObs)

                val sats = satellitesFlow.value
                val dataPoint = DataPoint(
                    sessionId = currentSessionId ?: "LIVE",
                    elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos(),
                    wallClockMs = System.currentTimeMillis(),
                    latitude = gps?.latitude,
                    longitude = gps?.longitude,
                    altitude = gps?.altitude,
                    gpsSpeedMs = gps?.speed, // Raw GPS speed
                    gpsAccuracyM = gps?.accuracy,
                    satellitesUsed = sats.usedInFix,
                    satellitesVisible = sats.visible,
                    accelX = imuWindow?.accelX ?: 0f,
                    accelY = imuWindow?.accelY ?: 0f,
                    accelZ = imuWindow?.accelZ ?: 0f,
                    accelMagnitude = imuWindow?.accelMagnitude ?: 0f,
                    derivedSpeedMs = fused.speedMs,
                    derivedAccelMs2 = fused.derivedAccelMs2 ?: 0f
                )

                _dataPointFlow.emit(dataPoint)

                val elapsedMs = (SystemClock.elapsedRealtimeNanos() - tickStartNs) / 1_000_000L
                delay(maxOf(0L, 100L - elapsedMs))
            }
        }
    }
}
