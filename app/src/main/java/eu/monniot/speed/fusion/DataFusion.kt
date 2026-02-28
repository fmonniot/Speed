package eu.monniot.speed.fusion

import android.location.Location
import android.os.SystemClock
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.sensor.ImuSample
import eu.monniot.speed.sensor.SatelliteInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*


class DataFusion(
    private val gpsFlow: SharedFlow<Location>,
    private val satellitesFlow: StateFlow<SatelliteInfo>,
    private val imuFlow: SharedFlow<ImuSample>,
    private val scope: CoroutineScope
) {
    private val _dataPointFlow = MutableSharedFlow<DataPoint>(extraBufferCapacity = 32)
    val dataPointFlow: SharedFlow<DataPoint> = _dataPointFlow

    var currentSessionId: String? = null

    private val kalmanFilter = SimpleKalmanFilter(0.1f, 0.5f)
    private var lastGpsLocation: Location? = null
    private var lastDerivedSpeed: Float = 0f
    
    private val imuSamples = mutableListOf<ImuSample>()

    fun start() {
        scope.launch(Dispatchers.Default) {
            // Collect IMU samples in real-time
            launch {
                imuFlow.collect { imuSamples.add(it) }
            }

            // Collect GPS locations
            launch {
                gpsFlow.collect { lastGpsLocation = it }
            }

            // 100ms Loop
            while (isActive) {
                delay(100)
                
                val currentImuSamples = synchronized(imuSamples) {
                    val samples = imuSamples.toList()
                    imuSamples.clear()
                    samples
                }

                val avgAccel = if (currentImuSamples.isNotEmpty()) {
                    floatArrayOf(
                        currentImuSamples.map { it.accelWorld[0] }.average().toFloat(),
                        currentImuSamples.map { it.accelWorld[1] }.average().toFloat(),
                        currentImuSamples.map { it.accelWorld[2] }.average().toFloat()
                    )
                } else floatArrayOf(0f, 0f, 0f)

                val accelMagnitude = sqrt(avgAccel[0] * avgAccel[0] + avgAccel[1] * avgAccel[1] + avgAccel[2] * avgAccel[2])

                val gps = lastGpsLocation
                val isGpsFresh = gps != null && (SystemClock.elapsedRealtimeNanos() - gps.elapsedRealtimeNanos) < 300_000_000L

                // FIX: Speed always going up was caused by integrating the absolute magnitude of acceleration.
                // We now project the 3D acceleration onto the direction of travel (longitudinal axis) 
                // to get a signed value (positive for acceleration, negative for braking).
                var longitudinalAccel = 0f
                if (gps != null && (gps.speed > 0.5f || gps.hasBearing())) {
                    val bearingRad = gps.bearing * PI.toFloat() / 180f
                    // In world frame: 0 is East (X), 1 is North (Y). Bearing 0 is North.
                    // Unit vector for bearing theta: [sin(theta), cos(theta)]
                    longitudinalAccel = avgAccel[0] * sin(bearingRad) + avgAccel[1] * cos(bearingRad)
                }
                
                // Deadzone to filter out sensor bias and noise when stationary or at constant speed
                if (abs(longitudinalAccel) < 0.15f) longitudinalAccel = 0f

                // Kalman prediction step using signed acceleration
                kalmanFilter.predict(0.1f, longitudinalAccel)

                if (isGpsFresh && gps != null) {
                    kalmanFilter.update(gps.speed)
                }

                val currentDerivedSpeed = kalmanFilter.getSpeed()
                val derivedAccel = (currentDerivedSpeed - lastDerivedSpeed) / 0.1f
                lastDerivedSpeed = currentDerivedSpeed

                val sats = satellitesFlow.value
                val dataPoint = DataPoint(
                    sessionId = currentSessionId ?: "LIVE",
                    elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos(),
                    wallClockMs = System.currentTimeMillis(),
                    latitude = gps?.latitude,
                    longitude = gps?.longitude,
                    altitude = gps?.altitude,
                    gpsSpeedMs = if (isGpsFresh) gps?.speed else null,
                    gpsAccuracyM = if (isGpsFresh) gps?.accuracy else null,
                    satellitesUsed = sats.usedInFix,
                    satellitesVisible = sats.visible,
                    accelX = avgAccel[0],
                    accelY = avgAccel[1],
                    accelZ = avgAccel[2],
                    accelMagnitude = accelMagnitude,
                    derivedSpeedMs = currentDerivedSpeed,
                    derivedAccelMs2 = derivedAccel
                )

                _dataPointFlow.emit(dataPoint)
            }
        }
    }
}
