package eu.monniot.speed.util

object FormatUtils {
    /**
     * Formats a duration in seconds to HH:MM:SS
     */
    fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
