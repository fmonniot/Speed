package eu.monniot.speed.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionsTest {

    @Test
    fun `m per s to km per h`() {
        assertEquals(0f, Conversions.msToKmh(0f), 0.001f)
        assertEquals(3.6f, Conversions.msToKmh(1f), 0.001f)
        assertEquals(36f, Conversions.msToKmh(10f), 0.001f)
        assertEquals(100f, Conversions.msToKmh(100f / 3.6f), 0.001f)
    }

    @Test
    fun `m per s2 to G`() {
        assertEquals(0f, Conversions.ms2ToG(0f), 0.001f)
        assertEquals(1f, Conversions.ms2ToG(9.81f), 0.001f)
        assertEquals(-1f, Conversions.ms2ToG(-9.81f), 0.001f)
        assertEquals(2f, Conversions.ms2ToG(19.62f), 0.001f)
    }
}
