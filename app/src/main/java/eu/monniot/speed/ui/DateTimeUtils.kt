package eu.monniot.speed.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class SessionTimeInfo(
    val primary: String,
    val secondary: String
)

// True when the timestamp falls in the current calendar week. Used by the Ride-home
// "This week" count and the Trips "This week" filter. Real aggregate queries arrive in E3.
fun isThisWeek(timeMs: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timeMs }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.WEEK_OF_YEAR) == then.get(Calendar.WEEK_OF_YEAR)
}

fun formatSessionTime(startTimeMs: Long, endTimeMs: Long?): SessionTimeInfo {
    val startCal = Calendar.getInstance().apply { timeInMillis = startTimeMs }
    val endCal = endTimeMs?.let { endTime ->
        Calendar.getInstance().apply { timeInMillis = endTime }
    }

    val dayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val startDateStr = dayFormat.format(startCal.time)
    val startTimeStr = timeFormat.format(startCal.time)

    return if (endCal == null) {
        SessionTimeInfo(startDateStr, startTimeStr)
    } else {
        val isSameDay = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)

        val endTimeStr = timeFormat.format(endCal.time)

        if (isSameDay) {
            SessionTimeInfo(startDateStr, "$startTimeStr - $endTimeStr")
        } else {
            val endDateStr = dayFormat.format(endCal.time)
            SessionTimeInfo("$startDateStr $startTimeStr", "$endDateStr $endTimeStr")
        }
    }
}
