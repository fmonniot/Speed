package eu.monniot.speed.export

import eu.monniot.speed.data.DataPoint
import eu.monniot.speed.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Writes a GPX 1.1 document for a single recorded session.
 *
 * ## Format
 * The output conforms to GPX 1.1 (http://www.topografix.com/GPX/1/1):
 * - A single `<trk>` containing one `<trkseg>`.
 * - Each [DataPoint] with **both** non-null `latitude` and `longitude` becomes a `<trkpt>`.
 *   Points missing either coordinate are silently skipped so the file remains schema-valid.
 * - `<ele>` is included when [DataPoint.altitude] is non-null (already in metres — no conversion).
 * - `<time>` uses the point's [DataPoint.wallClockMs] formatted as ISO-8601 UTC
 *   (`yyyy-MM-dd'T'HH:mm:ss'Z'`).
 * - `<metadata><time>` reflects the session start time.
 *
 * ## Stream contract
 * The stream is **flushed** but **not closed**; the caller owns the stream lifecycle,
 * matching the behaviour of `RaceRepository.exportToCsv`.
 */
object GpxExporter {

    suspend fun write(session: Session, points: List<DataPoint>, out: OutputStream) =
        withContext(Dispatchers.IO) {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val trackName = session.notes
                .takeIf { it.isNotBlank() }
                ?.let { escapeXml(it) }
                ?: "Speed ride ${escapeXml(session.sessionId)}"

            val startTimeIso = isoFormat.format(Date(session.startTimeMs))

            val writer = out.bufferedWriter(Charsets.UTF_8)

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.write(
                "<gpx version=\"1.1\" creator=\"Speed\" " +
                    "xmlns=\"http://www.topografix.com/GPX/1/1\">\n"
            )

            // Metadata block
            writer.write("  <metadata>\n")
            writer.write("    <time>$startTimeIso</time>\n")
            writer.write("  </metadata>\n")

            // Track
            writer.write("  <trk>\n")
            writer.write("    <name>$trackName</name>\n")
            writer.write("    <trkseg>\n")

            for (point in points) {
                val lat = point.latitude ?: continue
                val lon = point.longitude ?: continue

                writer.write("      <trkpt lat=\"$lat\" lon=\"$lon\">\n")

                point.altitude?.let { ele ->
                    writer.write("        <ele>$ele</ele>\n")
                }

                val timeIso = isoFormat.format(Date(point.wallClockMs))
                writer.write("        <time>$timeIso</time>\n")

                writer.write("      </trkpt>\n")
            }

            writer.write("    </trkseg>\n")
            writer.write("  </trk>\n")
            writer.write("</gpx>\n")

            writer.flush()
        }

    /**
     * Escapes the five predefined XML entities so that arbitrary strings
     * (e.g. session notes or IDs) can be safely embedded as element text.
     */
    private fun escapeXml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
