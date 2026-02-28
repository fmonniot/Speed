package eu.monniot.speed.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import eu.monniot.speed.fusion.CoordinateTransformer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class ImuSample(
    val accelWorld: FloatArray, // x, y, z in world frame
    val timestampNs: Long
)

class ImuCollector(
    private val sensorManager: SensorManager,
    private val rawSink: RawSensorSink? = null
) : SensorEventListener {

    private val _imuFlow = MutableSharedFlow<ImuSample>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val imuFlow: SharedFlow<ImuSample> = _imuFlow

    private var rotationMatrix = FloatArray(9)
    private var lastRotationVector: FloatArray? = null
    private var isStarted = false

    fun start() {
        if (isStarted) return
        
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val rotVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, rotVec, SensorManager.SENSOR_DELAY_GAME)
        isStarted = true
    }

    fun stop() {
        if (!isStarted) return
        
        sensorManager.unregisterListener(this)
        isStarted = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                lastRotationVector = event.values.clone()
                CoordinateTransformer.getRotationMatrixFromVector(event.values, rotationMatrix)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (lastRotationVector != null) {
                    val worldAccel = CoordinateTransformer.transform(event.values, rotationMatrix)
                    
                    _imuFlow.tryEmit(ImuSample(worldAccel, event.timestamp))
                    
                    // Pipe raw data to sink if available
                    rawSink?.onImuEvent(event.values.clone(), lastRotationVector!!, event.timestamp)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
