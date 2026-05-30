package eu.monniot.speed.util

import eu.monniot.speed.data.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitFormatTest {

    @Test
    fun `speed metric formats km per h`() {
        // 100 km/h = 27.777 m/s
        assertEquals("100 km/h", UnitFormat.speed(100f / 3.6f, Units.METRIC))
        assertEquals("km/h", UnitFormat.speedUnit(Units.METRIC))
    }

    @Test
    fun `speed imperial formats mph`() {
        // 100 km/h ≈ 62 mph
        assertEquals("62 mph", UnitFormat.speed(100f / 3.6f, Units.IMPERIAL))
        assertEquals("mph", UnitFormat.speedUnit(Units.IMPERIAL))
    }

    @Test
    fun `distance metric and imperial`() {
        // 1000 m = 1.0 km ≈ 0.6 mi
        assertEquals("1.0 km", UnitFormat.distance(1000f, Units.METRIC))
        assertEquals("0.6 mi", UnitFormat.distance(1000f, Units.IMPERIAL))
    }

    @Test
    fun `altitude metric and imperial`() {
        // 1000 m ≈ 3281 ft
        assertEquals("1000 m", UnitFormat.altitude(1000f, Units.METRIC))
        assertEquals("3281 ft", UnitFormat.altitude(1000f, Units.IMPERIAL))
    }

    @Test
    fun `lateral G is unit independent`() {
        assertEquals("0.84 g lat", UnitFormat.lateralG(0.84f))
    }

    @Test
    fun `lean reports magnitude and direction`() {
        assertEquals("38° right", UnitFormat.lean(38f))
        assertEquals("38° left", UnitFormat.lean(-38f))
        assertEquals("0°", UnitFormat.lean(0f))
    }

    @Test
    fun `imperial round trips back to SI`() {
        assertEquals(27.78f, Conversions.mphToMs(Conversions.msToMph(27.78f)), 0.01f)
        assertEquals(5f, Conversions.miToKm(Conversions.kmToMi(5f)), 0.001f)
        assertEquals(100f, Conversions.ftToM(Conversions.mToFt(100f)), 0.01f)
    }
}
