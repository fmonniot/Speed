package eu.monniot.speed.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class ImuSample(
    val accelWorld: FloatArray, // x, y, z in world frame
    val timestampNs: Long
)

class ImuCollector(private val sensorManager: SensorManager) : SensorEventListener {

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
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val worldAccel = FloatArray(3)
                if (lastRotationVector != null) {
                    worldAccel[0] = rotationMatrix[0] * event.values[0] + rotationMatrix[1] * event.values[1] + rotationMatrix[2] * event.values[2]
                    worldAccel[1] = rotationMatrix[3] * event.values[0] + rotationMatrix[4] * event.values[1] + rotationMatrix[5] * event.values[2]
                    worldAccel[2] = rotationMatrix[6] * event.values[0] + rotationMatrix[7] * event.values[1] + rotationMatrix[8] * event.values[2]
                    
                    _imuFlow.tryEmit(ImuSample(worldAccel, event.timestamp))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
