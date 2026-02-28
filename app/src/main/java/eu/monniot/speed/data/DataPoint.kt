package eu.monniot.speed.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_points")
data class DataPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val elapsedRealtimeNs: Long,
    val wallClockMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val gpsSpeedMs: Float?,
    val gpsAccuracyM: Float?,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val accelMagnitude: Float,
    val derivedSpeedMs: Float?,
    val derivedAccelMs2: Float?
)
