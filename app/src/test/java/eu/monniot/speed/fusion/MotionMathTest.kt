package eu.monniot.speed.fusion

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =================================================================================================
// MotionMathTest.kt
//
// Unit tests for MotionMath — pure JVM, no Android instrumentation required.
//
// SIGN CONVENTIONS UNDER TEST
// ---------------------------
//   leanAngleDeg: positive = leaning to the rider's right
//   lateralG:     positive = centripetal force toward the rider's right (right-hand turn)
//
// ROTATION MATRIX FIXTURE CONSTRUCTION
// ------------------------------------
// All lean-angle fixtures use a pure roll about the device's Y-axis.
// For roll angle φ (positive = lean right), the row-major 3x3 matrix is:
//
//   [ cosφ   0   sinφ ]    ← R[0..2]
//   [  0     1    0   ]    ← R[3..5]
//   [−sinφ   0   cosφ ]    ← R[6..8]
//
// Key elements for leanAngleDeg: R[6] = −sinφ, R[8] = cosφ.
// atan2(−R[6], R[8]) = atan2(sinφ, cosφ) = φ → positive φ returns positive degrees.
// =================================================================================================

class MotionMathTest {

    // ==============================================================================================
    // lateralG tests
    // ==============================================================================================

    /**
     * Travelling due North (bearing = 0), pure East acceleration of +9.81 m/s².
     *
     * The rightward-of-travel vector when heading North is (1, 0) in (E, N).
     * The full East acceleration projects entirely onto that rightward axis → +1.0 G.
     * This models a right-hand corner at exactly 1 G.
     */
    @Test
    fun `lateralG - heading North, pure East acceleration yields +1 G`() {
        val result = MotionMath.lateralG(
            accelEast = 9.81f,
            accelNorth = 0f,
            bearingRad = 0f
        )
        assertEquals(1.0f, result, 0.01f)
    }

    /**
     * Travelling due North (bearing = 0), pure North acceleration of +9.81 m/s².
     *
     * Longitudinal (forward) acceleration has zero projection onto the rightward axis.
     * This models hard braking or acceleration in a straight line → 0 G lateral.
     */
    @Test
    fun `lateralG - heading North, pure North acceleration yields 0 G`() {
        val result = MotionMath.lateralG(
            accelEast = 0f,
            accelNorth = 9.81f,
            bearingRad = 0f
        )
        assertEquals(0.0f, result, 0.01f)
    }

    /**
     * Travelling due East (bearing = π/2), pure North acceleration of +9.81 m/s².
     *
     * When heading East, the "right of travel" direction is South (−North). A Northward
     * world-frame acceleration therefore pushes the rider to the *left* → −1.0 G.
     *
     * Derivation:
     *   right = (cos(π/2), −sin(π/2)) = (0, −1) in (E, N)
     *   a_lat  = 0·(E) + 9.81·(−1) = −9.81 m/s²  →  −1.0 G
     */
    @Test
    fun `lateralG - heading East, pure North acceleration yields -1 G`() {
        val result = MotionMath.lateralG(
            accelEast = 0f,
            accelNorth = 9.81f,
            bearingRad = (PI / 2).toFloat()
        )
        assertEquals(-1.0f, result, 0.01f)
    }

    /**
     * Braking while heading East — deceleration is opposite to travel direction (i.e. Westward).
     *
     * accelEast = −9.81, heading East (bearing π/2).
     * The rightward-of-travel vector when heading East is (0, −1) in (E, N).
     * a_lat = (−9.81)·cos(π/2) − 0·sin(π/2) = 0  → no lateral component.
     *
     * This confirms that pure braking (force along the longitudinal axis) produces zero lateral G,
     * regardless of heading.
     */
    @Test
    fun `lateralG - braking while heading East produces 0 lateral G`() {
        val result = MotionMath.lateralG(
            accelEast = -9.81f,
            accelNorth = 0f,
            bearingRad = (PI / 2).toFloat()
        )
        assertEquals(0.0f, result, 0.01f)
    }

    /**
     * Symmetry check: heading South-West (bearing = 5π/4), acceleration with a known vector.
     *
     * Heading SW: bearing = 5π/4 = 225°
     *   right = (cos(5π/4), −sin(5π/4)) = (−1/√2, 1/√2)
     * Acceleration = (9.81/√2, 9.81/√2) → pure NE at 45°.
     * a_lat = (9.81/√2)·(−1/√2) + (9.81/√2)·(1/√2)
     *       = −9.81/2 + 9.81/2 = 0
     * → NE acceleration is purely longitudinal when heading SW, so lateral G = 0.
     */
    @Test
    fun `lateralG - NE acceleration while heading SW is purely longitudinal, 0 lateral G`() {
        val bearing = (5 * PI / 4).toFloat()
        val component = (9.81 / Math.sqrt(2.0)).toFloat()
        val result = MotionMath.lateralG(
            accelEast = component,
            accelNorth = component,
            bearingRad = bearing
        )
        assertEquals(0.0f, result, 0.05f)
    }

    // ==============================================================================================
    // leanAngleDeg tests
    // ==============================================================================================

    /**
     * The identity matrix represents no rotation (device perfectly upright, aligned with world).
     * Expected lean angle: 0°.
     *
     * Identity matrix in row-major:
     *   R[0]=1, R[1]=0, R[2]=0
     *   R[3]=0, R[4]=1, R[5]=0
     *   R[6]=0, R[7]=0, R[8]=1
     * atan2(−0, 1) = 0°.
     */
    @Test
    fun `leanAngleDeg - identity matrix returns 0 degrees`() {
        val identity = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        val result = MotionMath.leanAngleDeg(identity)
        assertEquals(0f, result, 0.01f)
    }

    /**
     * Roll of +30° about the Y-axis (leaning right).
     *
     * Rotation matrix for φ = +30°:
     *   R[0] = cos(30°) ≈  0.8660
     *   R[2] = sin(30°) ≈  0.5000
     *   R[6] = −sin(30°) ≈ −0.5000
     *   R[8] = cos(30°) ≈  0.8660
     *
     * atan2(−R[6], R[8]) = atan2(0.5, 0.8660) = 30°.
     * Expected: +30° (positive = right lean).
     */
    @Test
    fun `leanAngleDeg - 30 degree right roll returns +30 degrees`() {
        val phi = (30.0 * PI / 180.0)
        val matrix = rollMatrix(phi.toFloat())
        val result = MotionMath.leanAngleDeg(matrix)
        assertEquals(30f, result, 0.1f)
    }

    /**
     * Roll of −30° about the Y-axis (leaning left).
     *
     * By symmetry the matrix elements flip sign on R[2] and R[6]:
     *   R[6] = −sin(−30°) = sin(30°) ≈ +0.5
     *   R[8] = cos(−30°) ≈ +0.8660
     *
     * atan2(−R[6], R[8]) = atan2(−0.5, 0.8660) = −30°.
     * Expected: −30° (negative = left lean).
     */
    @Test
    fun `leanAngleDeg - 30 degree left roll returns -30 degrees`() {
        val phi = (-30.0 * PI / 180.0)
        val matrix = rollMatrix(phi.toFloat())
        val result = MotionMath.leanAngleDeg(matrix)
        assertEquals(-30f, result, 0.1f)
    }

    /**
     * Roll of +45° — checks an angle not on a cardinal boundary.
     */
    @Test
    fun `leanAngleDeg - 45 degree right roll returns +45 degrees`() {
        val phi = (45.0 * PI / 180.0)
        val matrix = rollMatrix(phi.toFloat())
        val result = MotionMath.leanAngleDeg(matrix)
        assertEquals(45f, result, 0.1f)
    }

    /**
     * Degenerate case: both R[6] and R[8] are exactly zero.
     *
     * This corresponds to a theoretically-impossible gimbal lock during normal riding.
     * The function must return 0° rather than NaN or crashing.
     */
    @Test
    fun `leanAngleDeg - degenerate matrix with R6 and R8 both zero returns 0`() {
        val degenerate = floatArrayOf(
            0f, 1f, 0f,
            0f, 0f, 1f,
            0f, 0f, 0f   // R[6]=0, R[7]=0, R[8]=0 — artificially degenerate
        )
        val result = MotionMath.leanAngleDeg(degenerate)
        assertEquals(0f, result, 0.0f)
    }

    // ==============================================================================================
    // Helpers
    // ==============================================================================================

    /**
     * Builds a 9-element row-major rotation matrix for a pure roll by [phi] radians about Y.
     *
     * Layout (see file-level comment):
     *   [ cosφ   0   sinφ ]   R[0], R[1], R[2]
     *   [  0     1    0   ]   R[3], R[4], R[5]
     *   [−sinφ   0   cosφ ]   R[6], R[7], R[8]
     */
    private fun rollMatrix(phi: Float): FloatArray {
        val c = cos(phi)
        val s = sin(phi)
        return floatArrayOf(
            c, 0f, s,
            0f, 1f, 0f,
            -s, 0f, c
        )
    }
}
