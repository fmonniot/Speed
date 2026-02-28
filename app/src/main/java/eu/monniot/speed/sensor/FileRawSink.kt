package eu.monniot.speed.sensor

import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class FileRawSink(private val context: Context) : RawSensorSink {
    private var writer: BufferedWriter? = null
    private var currentSessionId: String? = null

    override fun start(sessionId: String) {
        try {
            currentSessionId = sessionId
            val dir = File(context.getExternalFilesDir(null), "raw_traces")
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(dir, "trace_$sessionId.csv")
            writer = BufferedWriter(FileWriter(file))
            
            // Header
            writer?.write("type,timestampNs,val1,val2,val3,val4,val5,val6\n")
            Log.d("FileRawSink", "Started recording raw trace to ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("FileRawSink", "Failed to start raw recording", e)
        }
    }

    override fun stop() {
        try {
            writer?.flush()
            writer?.close()
            writer = null
            currentSessionId = null
            Log.d("FileRawSink", "Stopped raw recording")
        } catch (e: IOException) {
            Log.e("FileRawSink", "Failed to stop raw recording", e)
        }
    }

    override fun onImuEvent(accel: FloatArray, rotationVector: FloatArray, timestampNs: Long) {
        // I,timestamp,accelX,accelY,accelZ,rotX,rotY,rotZ,rotW
        val line = "I,$timestampNs,${accel[0]},${accel[1]},${accel[2]},${rotationVector[0]},${rotationVector[1]},${rotationVector[2]},${rotationVector.getOrNull(3) ?: 0f}\n"
        writeLine(line)
    }

    override fun onLocationEvent(location: Location) {
        // G,timestamp,lat,lon,alt,speed,accuracy,bearing
        val line = "G,${location.elapsedRealtimeNanos},${location.latitude},${location.longitude},${location.altitude},${location.speed},${location.accuracy},${location.bearing}\n"
        writeLine(line)
    }

    override fun onGnssStatusEvent(usedInFix: Int, visible: Int) {
        // S,timestamp,used,visible
        // Fix: Use SystemClock.elapsedRealtimeNanos() for consistency with other sensors
        val line = "S,${SystemClock.elapsedRealtimeNanos()}, $usedInFix, $visible\n"
        writeLine(line)
    }

    private fun writeLine(line: String) {
        try {
            writer?.write(line)
        } catch (e: IOException) {
            // Log once or handle
        }
    }
}
