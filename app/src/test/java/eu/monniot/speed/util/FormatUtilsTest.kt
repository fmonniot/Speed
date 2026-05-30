package eu.monniot.speed.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatDuration() {
        assertEquals("00:00:00", FormatUtils.formatDuration(0))
        assertEquals("00:00:59", FormatUtils.formatDuration(59))
        assertEquals("00:01:00", FormatUtils.formatDuration(60))
        assertEquals("00:01:01", FormatUtils.formatDuration(61))
        assertEquals("01:00:00", FormatUtils.formatDuration(3600))
        assertEquals("01:01:01", FormatUtils.formatDuration(3661))
        assertEquals("10:00:00", FormatUtils.formatDuration(36000))
    }
}
