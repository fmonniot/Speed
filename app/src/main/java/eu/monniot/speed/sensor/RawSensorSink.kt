package eu.monniot.speed.sensor

import android.location.Location

interface RawSensorSink {
    /**
     * Called for every IMU event.
     * @param accel Raw linear acceleration (device frame) [x, y, z]
     * @param rotationVector Raw rotation vector [x, y, z, w, ...]
     * @param timestampNs Monotonic timestamp in nanoseconds
     */
    fun onImuEvent(accel: FloatArray, rotationVector: FloatArray, timestampNs: Long)

    /**
     * Called for every GPS location update.
     */
    fun onLocationEvent(location: Location)

    /**
     * Called when GNSS status (satellite counts) changes.
     */
    fun onGnssStatusEvent(usedInFix: Int, visible: Int)
    
    /**
     * Called when recording starts to initialize the sink.
     */
    fun start(sessionId: String)

    /**
     * Called when recording stops to flush and close the sink.
     */
    fun stop()
}
