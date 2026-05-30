package eu.monniot.speed.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// pathPolyline encodes an ordered list of (latitude, longitude) waypoints as:
//   "lat,lon;lat,lon;..."
// Each coordinate is a decimal degree value separated by a comma.
// Waypoints are separated by semicolons.
// Example: "48.8566,2.3522;48.8600,2.3510"
// Use encodePath / decodePath helpers below.
@Entity(tableName = "segments")
data class Segment(
    @PrimaryKey val segmentId: String,
    val name: String,
    val distanceM: Float,
    val pathPolyline: String,
    val isFavourite: Boolean = false,
    val isGoal: Boolean = false,
    val createdAtMs: Long
)

/** Encodes a list of (latitude, longitude) pairs into the pathPolyline format. */
fun encodePath(points: List<Pair<Double, Double>>): String =
    points.joinToString(separator = ";") { (lat, lon) -> "$lat,$lon" }

/** Decodes a pathPolyline string back to a list of (latitude, longitude) pairs.
 *  Returns an empty list if [s] is blank. */
fun decodePath(s: String): List<Pair<Double, Double>> {
    if (s.isBlank()) return emptyList()
    return s.split(";").map { segment ->
        val (lat, lon) = segment.split(",")
        lat.toDouble() to lon.toDouble()
    }
}
