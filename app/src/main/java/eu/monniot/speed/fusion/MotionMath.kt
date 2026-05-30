package eu.monniot.speed.fusion

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

// =================================================================================================
// MotionMath.kt
//
// WHAT THIS DOES
// --------------
// Pure, JVM-testable math functions for deriving lean angle and lateral G-force from sensor data
// that has already been processed into convenient forms:
//
//   • leanAngleDeg  — side-to-side lean of the motorcycle (device→world rotation matrix in)
//   • lateralG      — centripetal / cornering force expressed as a fraction of g
//
// These functions are PURE: no Android imports, no side effects, no state. They can be called
// from the fusion pipeline, from tests, or from any other context.
//
//
// COORDINATE CONVENTIONS
// ----------------------
// Rotation matrix (device → world):
//   The 9-element float array is row-major, matching Android's SensorManager.getRotationMatrix
//   output and the CoordinateTransformer in this package:
//
//     [ R[0]  R[1]  R[2] ]   ← world-X (East)  components of device axes
//     [ R[3]  R[4]  R[5] ]   ← world-Y (North) components of device axes
//     [ R[6]  R[7]  R[8] ]   ← world-Z (Up)    components of device axes
//
//   Column 0 = where the device's +X axis points in the world.
//   Column 2 = where the device's +Z axis (screen normal) points in the world.
//
// World frame (shared with VelocityFusion):
//   accelX → East   (+x = East)
//   accelY → North  (+y = North)
//   accelZ → Up     (+z = Up)
//
// Bearing:
//   Clockwise from North, in radians. Matches Android Location.getBearing (caller converts deg→rad).
//   bearing = 0   → heading North  (h = (0, 1) in East/North)
//   bearing = π/2 → heading East   (h = (1, 0) in East/North)
// =================================================================================================

/**
 * Pure math utilities for motorcycle lean angle and lateral G-force.
 *
 * All functions are stateless and dependency-free (only `kotlin.math`).
 * They are designed to be called from the DataFusion pipeline on each 100 ms tick
 * but are equally usable in JVM unit tests without any Android instrumentation.
 *
 * **Coordinate frames** — see file-level documentation for the full picture.
 */
object MotionMath {

    /**
     * Derives the motorcycle's lean angle from a 9-element device→world rotation matrix.
     *
     * **What "lean angle" means here:**
     * Lean is the rotation of the device about the world's vertical axis projected onto the
     * bike's longitudinal axis — i.e., side-to-side tilt. In Android's rotation math this is
     * the *roll* component, extracted as `atan2(−R[6], R[8])`.
     *
     * **Why this formula:**
     * For a pure roll by angle φ about the device's Y-axis (the long axis of the phone, mounted
     * with Y pointing forward along the bike), the rotation matrix rows are:
     *
     *     Row 0: [ cosφ,  0,  sinφ ]
     *     Row 1: [  0,    1,   0   ]
     *     Row 2: [−sinφ,  0,  cosφ ]
     *
     * So R[6] = −sinφ and R[8] = cosφ, which gives:
     *   `atan2(−R[6], R[8])` = `atan2(sinφ, cosφ)` = φ.
     *
     * **Sign convention (positive = leaning right):**
     * With the phone mounted in the standard landscape-portrait orientation on the motorcycle
     * (screen facing the rider, Y-axis pointing toward the front wheel), rolling the bike to
     * the rider's right produces a positive φ in the formula above. No negation is applied.
     * The sign convention is therefore:
     *   • Positive angle → motorcycle leaning to the rider's **right**
     *   • Negative angle → motorcycle leaning to the rider's **left**
     *
     * **Degenerate case:**
     * When both R[6] and R[8] are near zero the bike is in a 90° gimbal-lock configuration
     * (nearly vertical relative to the world Z-axis). This is physically impossible during
     * normal riding; the function returns 0° to avoid a noisy undefined result.
     *
     * @param rotationMatrix 9-element row-major rotation matrix (device→world),
     *   as produced by [CoordinateTransformer.getRotationMatrixFromVector] or
     *   Android's `SensorManager.getRotationMatrix`.
     * @return Lean angle in degrees. Positive = leaning right, negative = leaning left.
     *   Range is (−180°, +180°] but in practice will be within ±60° for motorcycles.
     */
    fun leanAngleDeg(rotationMatrix: FloatArray): Float {
        val r6 = rotationMatrix[6]
        val r8 = rotationMatrix[8]

        // Guard degenerate case: when both components are nearly zero the atan2 result is
        // undefined (or extremely noisy). This arises when the device is near 90° pitch
        // (screen pointing straight up or straight down) — not a real riding scenario.
        if (abs(r6) < 1e-7f && abs(r8) < 1e-7f) return 0f

        // roll = atan2(sinφ, cosφ) = φ   [see KDoc above for derivation]
        // Positive result = leaning right. No negation needed.
        val rollRad = atan2(-r6, r8)

        return Math.toDegrees(rollRad.toDouble()).toFloat()
    }

    /**
     * Computes the lateral (side-to-side) acceleration experienced by the rider, in units of G.
     *
     * **Geometry:**
     * Given a direction of travel described by [bearingRad] (clockwise from North), the
     * heading unit vector in the East/North world plane is:
     *
     *     h = (hE, hN) = (sin(bearing), cos(bearing))
     *
     * Rotating h by −90° (i.e. to the right of travel) gives the "rightward" unit vector:
     *
     *     right = (cos(bearing), −sin(bearing))
     *
     * The component of acceleration in the rightward direction is:
     *
     *     a_lateral = accelEast·cos(bearing) − accelNorth·sin(bearing)
     *
     * Dividing by 9.81 m/s² converts m/s² → G.
     *
     * **Sign convention (positive = force toward rider's right):**
     * When cornering right, centripetal acceleration points toward the centre of the turn
     * (i.e. to the right of the rider). The world-frame acceleration vector therefore has a
     * rightward component → positive lateralG. Cornering left → negative lateralG.
     *
     * @param accelEast   World-frame East acceleration in m/s² (from the fusion pipeline).
     * @param accelNorth  World-frame North acceleration in m/s² (from the fusion pipeline).
     * @param bearingRad  Current direction of travel in radians, clockwise from North.
     *   Convert from Android's `Location.getBearing()` (degrees) with `bearing * PI / 180`.
     *   Only meaningful when speed > ~0.5 m/s (same threshold as [VelocityFusion]).
     * @return Lateral acceleration in units of G (1 G ≈ 9.81 m/s²).
     *   Positive = force toward rider's right (right-hand turn).
     *   Negative = force toward rider's left  (left-hand turn).
     */
    fun lateralG(accelEast: Float, accelNorth: Float, bearingRad: Float): Float {
        // Project the world-frame acceleration onto the rightward-of-travel axis.
        //   right = (cos(bearing), −sin(bearing))  [see KDoc above]
        //   a_lateral = accelEast·right.E + accelNorth·right.N
        //             = accelEast·cos(bearing) − accelNorth·sin(bearing)
        val aLateral = accelEast * cos(bearingRad) - accelNorth * sin(bearingRad)
        return aLateral / 9.81f
    }
}
