package eu.monniot.speed.fusion

import android.location.Location
import android.os.SystemClock
import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.sensor.ImuSample
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.sqrt

class SimpleKalmanFilter(private val q: Float, private val r: Float) {
    private var speed: Float = 0f
    private var p: Float = 1f

    fun predict(dt: Float, accel: Float) {
        speed += accel * dt
        p += q
    }

    fun update(measurement: Float) {
        val k = p / (p + r)
        speed += k * (measurement - speed)
        p *= (1 - k)
    }

    fun getSpeed() = speed
}

class DataFusion(
    private val sessionId: String,
    private val gpsFlow: SharedFlow<Location>,
    private val imuFlow: SharedFlow<ImuSample>,
    private val scope: CoroutineScope
) {
    private val _dataPointFlow = MutableSharedFlow<DataPoint>(extraBufferCapacity = 32)
    val dataPointFlow: SharedFlow<DataPoint> = _dataPointFlow

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

                // Kalman prediction
                kalmanFilter.predict(0.1f, accelMagnitude)

                val gps = lastGpsLocation
                val isGpsFresh = gps != null && (SystemClock.elapsedRealtimeNanos() - gps.elapsedRealtimeNanos) < 300_000_000L

                if (isGpsFresh && gps != null) {
                    kalmanFilter.update(gps.speed)
                }

                val currentDerivedSpeed = kalmanFilter.getSpeed()
                val derivedAccel = (currentDerivedSpeed - lastDerivedSpeed) / 0.1f
                lastDerivedSpeed = currentDerivedSpeed

                val dataPoint = DataPoint(
                    sessionId = sessionId,
                    elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos(),
                    wallClockMs = System.currentTimeMillis(),
                    latitude = gps?.latitude,
                    longitude = gps?.longitude,
                    altitude = gps?.altitude,
                    gpsSpeedMs = if (isGpsFresh) gps?.speed else null,
                    gpsAccuracyM = if (isGpsFresh) gps?.accuracy else null,
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
