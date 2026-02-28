package eu.monniot.speed.fusion

import eu.monniot.speed.sensor.ImuSample
import kotlin.math.PI
import kotlin.math.sqrt

// =================================================================================================
// VelocityFusion.kt
//
// WHAT THIS DOES
// --------------
// Estimates the vehicle's speed at 100ms intervals by combining two imperfect sources:
//
//   • GPS   — accurate absolute speed, but slow (up to 300ms latency) and unavailable in tunnels
//   • IMU   — fast and continuous, but drifts over time due to sensor noise integration
//
// The fusion strategy is:
//   1. Track velocity as a 2D vector (North/East components) rather than a scalar speed.
//      This gives signed acceleration for free — braking is negative, no bearing math needed.
//   2. Use a Kalman filter on each axis to weight GPS vs. IMU optimally at each timestep.
//   3. Apply Zero Velocity Updates (ZUPT) when the vehicle is detected to be stationary,
//      which prevents IMU noise from accumulating into phantom speed during standstills.
//
//
// WHY A VECTOR, NOT A SCALAR
// --------------------------
// Speed is scalar (always ≥ 0), but velocity is a signed vector. Working in vector space means:
//
//   • Braking and accelerating are naturally represented as negative/positive IMU components
//   • GPS updates decompose into (vx, vy) without needing a valid bearing (critical at low speed,
//     where GPS bearing becomes unreliable or undefined)
//   • The final speed = sqrt(vx² + vy²) is always positive and physically correct
//
// If you tracked a scalar speed and fed it accelMagnitude (always ≥ 0), speed could only ever
// increase. That's the bug this design avoids.
//
//
// COORDINATE SYSTEM
// -----------------
// All vectors are in the *world frame* (North/East/Up), not the device frame.
// The caller is responsible for rotating IMU samples from device frame to world frame before
// passing them here. See ImuCollector.kt for how to do this using the rotation matrix from
// TYPE_ROTATION_VECTOR.
//
//   accelX → East  (positive = accelerating East)
//   accelY → North (positive = accelerating North)
//   accelZ → Up    (positive = accelerating Up — not used for speed, but available)
//
//
// THREADING
// ---------
// This class is NOT thread-safe. All calls must come from the same coroutine dispatcher
// (Dispatchers.Default is appropriate). The DataFusion coroutine loop should be the only caller.
// =================================================================================================


/**
 * One averaged IMU sample, covering a 100ms window.
 *
 * @param accelX  World-frame East acceleration in m/s². Negative = decelerating East.
 * @param accelY  World-frame North acceleration in m/s². Negative = decelerating North.
 * @param accelZ  World-frame Up acceleration in m/s². Not used for speed integration.
 * @param variance Statistical variance across the raw samples in this window.
 *                 Low variance = the phone was barely moving. Used for ZUPT detection.
 */
data class ImuWindow(
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val variance: Float
) {
    companion object {
        /**
         * Creates an ImuWindow by aggregating a list of IMU samples.
         * Aggregation averages out high-frequency noise/vibration before the Kalman Predict phase.
         */
        fun fromSamples(samples: List<ImuSample>): ImuWindow? {
            if (samples.isEmpty()) return null

            val ax = samples.map { it.accelWorld[0] }.average().toFloat()
            val ay = samples.map { it.accelWorld[1] }.average().toFloat()
            val az = samples.map { it.accelWorld[2] }.average().toFloat()

            // Calculate variance for ZUPT. 
            // We use the magnitude of the acceleration vector to detect overall stillness.
            val magnitudes = samples.map { s ->
                sqrt(s.accelWorld[0] * s.accelWorld[0] + s.accelWorld[1] * s.accelWorld[1] + s.accelWorld[2] * s.accelWorld[2])
            }
            val avgMag = magnitudes.average().toFloat()
            val variance = magnitudes.map { m -> (m - avgMag) * (m - avgMag) }.average().toFloat()

            return ImuWindow(ax, ay, az, variance)
        }
    }

    /** Convenience for magnitude calculation used in ZUPT logic. */
    val accelMagnitude: Float get() = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
}

/**
 * The most recent GPS observation, passed in on each 100ms tick.
 *
 * @param speedMs     Speed in m/s from Location.getSpeed(). Always ≥ 0.
 * @param bearingRad  Direction of travel in radians, clockwise from North.
 *                    Only valid when speedMs > ~0.5 m/s. Below that, treat as unreliable.
 * @param accuracyM   Horizontal position accuracy in meters. Used to gate whether we trust
 *                    this fix at all — if accuracy > 20m, the fix is too poor to use.
 * @param ageMs       How many milliseconds ago this fix was received.
 *                    Fixes older than GPS_MAX_AGE_MS are ignored.
 */
data class GpsObservation(
    val speedMs: Float,
    val bearingRad: Float,
    val accuracyM: Float,
    val ageMs: Long
)

/**
 * Output produced every 100ms.
 *
 * @param speedMs          Best estimate of current speed in m/s. Always ≥ 0.
 * @param derivedAccelMs2  Change in speed since the last tick, divided by dt (≈ 0.1s).
 *                         Positive = accelerating, negative = braking.
 *                         Null on the very first tick (no previous speed to diff against).
 * @param isStationary     Whether ZUPT was active this tick. Useful for the UI to show
 *                         a "stopped" indicator rather than a tiny non-zero speed.
 * @param gpsWasUsed       Whether a fresh GPS fix contributed to this estimate.
 *                         False during tunnels / GPS outages.
 */
data class FusedVelocity(
    val speedMs: Float,
    val derivedAccelMs2: Float?,
    val isStationary: Boolean,
    val gpsWasUsed: Boolean
)


// =================================================================================================
// SimpleKalmanFilter
//
// A minimal 1D Kalman filter tracking a single state variable (one velocity component).
//
// HOW A KALMAN FILTER WORKS (in plain terms)
// ------------------------------------------
// The filter maintains two things: an *estimate* and an *uncertainty* about that estimate.
// Each cycle has two phases:
//
//   PREDICT: "I know where I was. Given the IMU, where do I think I am now?"
//     - The estimate moves forward by: state += control * dt
//     - The uncertainty grows by Q (process noise) because the IMU isn't perfect
//
//   UPDATE: "I got a GPS reading. How should I adjust my estimate?"
//     - The filter blends GPS and its own estimate proportionally to their uncertainties
//     - If GPS is high-quality (low R), the filter trusts GPS more
//     - If GPS is stale or inaccurate (high R), the filter trusts its own IMU-propagated estimate
//     - After the update, uncertainty shrinks
//
// The key insight: the filter automatically figures out the right blend. You just tune Q and R.
//
//
// TUNING Q AND R
// --------------
//   Q (process noise):
//     How much do you trust the IMU between GPS fixes?
//     Too low  → filter is sluggish, lags behind real motion
//     Too high → filter is jittery, IMU noise bleeds through
//     Good starting point: 0.5 (m/s)² for a car; lower (0.1) for smooth highway driving
//
//   R (measurement noise for GPS):
//     How much do you trust GPS speed?
//     Too low  → filter slavishly follows GPS, including its latency and noise
//     Too high → filter ignores GPS, drifts on IMU alone
//     Good starting point: 0.3 (m/s)² for a modern phone GPS
//
//   R for ZUPT (passed in dynamically):
//     Should be very low (0.01) — you're saying "I am certain the speed is zero right now".
//     This aggressively resets drift during stops.
// =================================================================================================

/**
 * 1D Kalman filter for a single velocity component (e.g., North velocity or East velocity).
 *
 * @param q Process noise variance (m/s)². Controls how much IMU noise accumulates per second.
 * @param r Default measurement noise variance (m/s)². Controls GPS trust. Can be overridden
 *          per-update call for ZUPT (use a very low value like 0.01 to force reset to zero).
 */
class SimpleKalmanFilter(private val q: Float, private val r: Float) {

    /** Current velocity estimate in m/s. Can be negative (moving in the negative axis direction). */
    var estimate: Float = 0f
        private set

    /**
     * Current uncertainty of the estimate. High at startup, shrinks after GPS updates.
     * Initialized to a large value (high uncertainty) so the first GPS fix is trusted fully.
     */
    private var uncertainty: Float = 10f

    /**
     * PREDICT phase — advance the estimate forward in time using the IMU.
     *
     * Call this every 100ms tick, even when GPS is unavailable. This is what keeps the
     * estimate moving between GPS fixes.
     *
     * @param dt       Time step in seconds. Should be ~0.1 but use the actual measured dt
     *                 rather than a hardcoded constant — the OS loop isn't perfectly precise.
     * @param control  Signed acceleration in m/s² for this axis (world-frame). Negative = braking.
     *                 This is what makes speed go *down* during braking — the signed value
     *                 is critical. Do NOT pass accelMagnitude here.
     */
    fun predict(dt: Float, control: Float) {
        estimate += control * dt
        uncertainty += q * dt   // uncertainty grows over time without a GPS fix
    }

    /**
     * UPDATE phase — correct the estimate with a measurement (GPS or ZUPT).
     *
     * Call this only when a fresh, trustworthy measurement is available. Calling it with
     * a bad measurement (stale GPS, wrong bearing) is worse than not calling it at all.
     *
     * @param measurement      The measured velocity for this axis in m/s. For GPS: decompose
     *                         speed * bearing into North/East components before calling.
     *                         For ZUPT: pass 0f.
     * @param measurementNoise Override for R. Pass the default `r` for GPS, or a very small
     *                         value (e.g. 0.01f) for ZUPT to forcibly drive uncertainty down.
     */
    fun update(measurement: Float, measurementNoise: Float = r) {
        // Kalman gain: how much weight to give the new measurement vs. our current estimate.
        // When uncertainty >> measurementNoise → gain ≈ 1 → trust the measurement almost fully
        // When uncertainty << measurementNoise → gain ≈ 0 → trust our estimate, ignore measurement
        val gain = uncertainty / (uncertainty + measurementNoise)

        estimate += gain * (measurement - estimate)
        uncertainty *= (1f - gain)  // uncertainty always shrinks after an update
    }

    /** Reset state. Call when starting a new session or after a long GPS outage. */
    fun reset() {
        estimate = 0f
        uncertainty = 10f
    }
}


// =================================================================================================
// VelocityFusion
//
// CALLER CONTRACT
// ---------------
// This class expects to be called once every 100ms from a single coroutine.
// A minimal integration looks like:
//
//   val fusion = VelocityFusion()
//
//   // In your 100ms coroutine loop:
//   while (isRecording) {
//       val tickStart = SystemClock.elapsedRealtimeNanos()
//
//       val imuWindow = imuCollector.drainWindow()    // average + variance of last 100ms of samples
//       val gpsObs   = gpsCollector.latestObservation()  // most recent GPS fix, with age
//
//       val result = fusion.tick(dt = 0.1f, imu = imuWindow, gps = gpsObs)
//
//       emit(result)   // push to UI / Room
//
//       // Sleep for the remainder of the 100ms window, accounting for processing time
//       val elapsed = (SystemClock.elapsedRealtimeNanos() - tickStart) / 1_000_000L
//       delay(maxOf(0L, 100L - elapsed))
//   }
//
//
// IMPORTANT: The `dt` you pass to tick() should be the *actual* elapsed time since the last
// call, measured from SystemClock.elapsedRealtimeNanos(). Don't hardcode 0.1f — the OS timer
// has jitter (±5ms is typical), and accumulated error over a 10-minute race is significant.
// =================================================================================================

/**
 * Fuses IMU and GPS into a single best-estimate speed at each 100ms tick.
 *
 * See file-level documentation for architecture overview and threading requirements.
 *
 * @param kalmanQ  Process noise for the Kalman filters. Default 0.5 (m/s)² suits typical
 *                 road racing. Lower this (e.g. 0.1) for very smooth, high-speed circuits
 *                 where the vehicle rarely changes speed abruptly.
 * @param kalmanR  GPS measurement noise. Default 0.3 (m/s)² suits modern phone GPS.
 *                 Increase if you observe the speed output being jittery on straights.
 */
class VelocityFusion(
    kalmanQ: Float = 0.5f,
    kalmanR: Float = 0.3f
) {
    // Two independent 1D filters — one per horizontal world-frame axis.
    // Running them independently is simpler than a 2D filter and works well in practice
    // because North and East accelerations are not physically coupled for a ground vehicle.
    private val kalmanNorth = SimpleKalmanFilter(q = kalmanQ, r = kalmanR)
    private val kalmanEast  = SimpleKalmanFilter(q = kalmanQ, r = kalmanR)

    private var lastSpeedMs: Float? = null  // for derivedAccel computation

    companion object {
        /** GPS fixes older than this are discarded entirely rather than risk using stale data. */
        const val GPS_MAX_AGE_MS = 300L

        /**
         * GPS accuracy threshold above which we distrust the fix.
         * 20m is fairly loose — in clear-sky conditions you'll typically see 3–8m.
         * You could tighten this to 10m for track use where sky visibility is good.
         */
        const val GPS_MIN_ACCURACY_M = 20f

        /**
         * Speed below which GPS bearing is considered unreliable.
         * At very low speeds, the phone's heading estimate becomes noisy.
         * The ZUPT detector handles the truly-zero case separately.
         */
        const val GPS_MIN_SPEED_FOR_BEARING_MS = 0.5f

        // ZUPT thresholds — both IMU and GPS must agree before we declare a stop.
        // Requiring both prevents false stops on: slow GPS drift, sensor glitches,
        // brief decelerations, and momentary GPS outages.
        const val ZUPT_GPS_SPEED_THRESHOLD_MS  = 0.3f   // ~1 km/h
        const val ZUPT_IMU_MAGNITUDE_THRESHOLD = 0.25f  // m/s² average. Tightened slightly based on stationary tests
        const val ZUPT_IMU_VARIANCE_THRESHOLD  = 0.015f // low variance = stable/still. Tightened slightly based on stationary tests
        const val ZUPT_MEASUREMENT_NOISE       = 0.01f  // very confident: velocity = 0
    }

    /**
     * Advance the fusion by one timestep.
     *
     * This is the only public method callers need. Call it once per 100ms tick.
     *
     * @param dt   Actual elapsed time since the last call, in seconds. Measure this from
     *             SystemClock.elapsedRealtimeNanos() — do not hardcode 0.1f.
     * @param imu  Averaged IMU window for this tick. Must be in world-frame coordinates.
     *             If the IMU was unavailable this tick (e.g., sensor registration failed),
     *             pass null and predict() will be skipped — uncertainty will grow.
     * @param gps  Most recent GPS observation, with its age. Pass null if GPS is unavailable
     *             (tunnel, cold start, permissions denied). The filter will coast on IMU alone.
     *
     * @return     Best-estimate velocity for this tick.
     */
    fun tick(dt: Float, imu: ImuWindow?, gps: GpsObservation?): FusedVelocity {

        // ── 1. PREDICT ────────────────────────────────────────────────────────────────────────
        // Advance both Kalman filters forward using IMU acceleration.
        // If IMU is null, we skip predict — uncertainty grows but estimate holds. This is
        // preferable to predicting with zero (which would incorrectly suggest no acceleration).
        if (imu != null) {
            kalmanNorth.predict(dt, control = imu.accelY)  // accelY = North in world frame
            kalmanEast.predict(dt,  control = imu.accelX)  // accelX = East in world frame
        }

        // ── 2. DETECT STATIONARY ──────────────────────────────────────────────────────────────
        // Check for stillness *after* predict, *before* GPS update.
        // ZUPT overrides both IMU drift and GPS noise simultaneously.
        val isStationary = isStationary(imu, gps)

        var gpsWasUsed = false

        if (isStationary) {
            // ZUPT: inject a zero-velocity measurement with very high confidence.
            // This aggressively collapses any accumulated IMU drift.
            // The very low measurementNoise overrides the filter's current uncertainty,
            // driving the estimate to zero regardless of how long we've been drifting.
            kalmanNorth.update(measurement = 0f, measurementNoise = ZUPT_MEASUREMENT_NOISE)
            kalmanEast.update(measurement = 0f,  measurementNoise = ZUPT_MEASUREMENT_NOISE)

        } else {
            // ── 3. GPS UPDATE ─────────────────────────────────────────────────────────────────
            // Only update from GPS if the fix is fresh and trustworthy.
            // Stale or inaccurate GPS is worse than no GPS — a bad update moves the estimate
            // in the wrong direction and then uncertainty shrinks, making recovery slow.
            val freshGps = gps?.takeIf { fix ->
                fix.ageMs < GPS_MAX_AGE_MS
                        && fix.accuracyM < GPS_MIN_ACCURACY_M
                        && fix.speedMs > GPS_MIN_SPEED_FOR_BEARING_MS
            }

            if (freshGps != null) {
                // Decompose GPS speed + bearing into North/East velocity components.
                // This is why Option B beats Option A: GPS speed at near-zero is ~0 regardless
                // of bearing, so low-speed GPS corrections are still correct even if bearing
                // is noisy. We only skip bearing-based decomposition below the threshold above.
                val vNorth = freshGps.speedMs * kotlin.math.cos(freshGps.bearingRad)
                val vEast  = freshGps.speedMs * kotlin.math.sin(freshGps.bearingRad)

                kalmanNorth.update(vNorth)
                kalmanEast.update(vEast)
                gpsWasUsed = true
            }
        }

        // ── 4. DERIVE OUTPUT ──────────────────────────────────────────────────────────────────
        val speedMs = sqrt(
            kalmanNorth.estimate * kalmanNorth.estimate +
                    kalmanEast.estimate  * kalmanEast.estimate
        ).coerceAtLeast(0f)  // sqrt is always ≥ 0 mathematically, but guard float rounding

        // Derived acceleration: Δspeed / Δtime. Signed — negative means braking.
        // Note: this is the rate of change of *speed* (scalar), not the raw IMU acceleration.
        // It answers "is the car getting faster or slower?" rather than "what is the 3D force?"
        val derivedAccel = lastSpeedMs?.let { prev -> (speedMs - prev) / dt }
        lastSpeedMs = speedMs

        return FusedVelocity(
            speedMs          = speedMs,
            derivedAccelMs2  = derivedAccel,
            isStationary     = isStationary,
            gpsWasUsed       = gpsWasUsed
        )
    }

    /**
     * Determines whether the vehicle is stationary this tick.
     *
     * DESIGN DECISION — why treat stale GPS as "no information":
     *
     *   GPS updates typically arrive at 1Hz, while fusion ticks run at 10Hz.
     *   For ~90% of ticks, the most recent GPS fix is "stale" (age > 300ms).
     *   If we strictly required BOTH to agree at all times, ZUPT would only fire
     *   once per second even if the phone is perfectly still.
     *
     *   Correct strategy:
     *     • If GPS is FRESH: BOTH must agree (stops false IMU triggers on smooth roads).
     *     • If GPS is STALE or MISSING: Rely on IMU ONLY (avoids ZUPT blocking while parked).
     *
     *   This ensures continuous ZUPT during standstills while maintaining the safety
     *   of GPS-gated ZUPT during smooth driving.
     */
    private fun isStationary(imu: ImuWindow?, gps: GpsObservation?): Boolean {
        // IMU check is always mandatory for ZUPT
        val imuSaysStill = imu != null
                && imu.accelMagnitude < ZUPT_IMU_MAGNITUDE_THRESHOLD
                && imu.variance       < ZUPT_IMU_VARIANCE_THRESHOLD

        // Only consider GPS if it's fresh. Stale GPS shouldn't block ZUPT.
        val freshGps = gps?.takeIf {
            it.ageMs < GPS_MAX_AGE_MS && it.accuracyM < GPS_MIN_ACCURACY_M
        }

        return if (freshGps != null) {
            // If GPS is fresh, BOTH must agree
            imuSaysStill && freshGps.speedMs < ZUPT_GPS_SPEED_THRESHOLD_MS
        } else {
            // If GPS is stale or missing (outage/tunnel), rely on IMU only
            imuSaysStill
        }
    }

    /**
     * Reset all state. Call at the start of each new recording session.
     * Failure to call this means a new session starts with the speed estimate and uncertainty
     * from the end of the previous session.
     */
    fun reset() {
        kalmanNorth.reset()
        kalmanEast.reset()
        lastSpeedMs = null
    }
}
