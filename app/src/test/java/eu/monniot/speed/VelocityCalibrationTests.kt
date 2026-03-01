package eu.monniot.speed

import eu.monniot.speed.fusion.CoordinateTransformer
import eu.monniot.speed.fusion.GpsObservation
import eu.monniot.speed.fusion.ImuWindow
import eu.monniot.speed.fusion.VelocityFusion
import eu.monniot.speed.sensor.ImuSample
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.time.LocalDateTime
import kotlin.math.PI


private data class GpsTickStats(
    val totalTicks: Int,
    val freshGpsTicks: Int,
    val staleGpsTicks: Int,
    val minAgeMs: Float,
    val maxAgeMs: Float,
    val avgAgeMs: Float,
    val minSpeedMs: Float,
    val maxSpeedMs: Float,
    val avgSpeedMs: Float
)

class VelocityCalibrationTests {

    @get:Rule
    val name: TestName = TestName()

    @Test
    fun testAcceleration() {
        runCalibration(
            traceId = "7a5777c1-49c6-4afb-a52f-71192748f57b",
            description = "acceleration only (more or less). fort to community turnaround thing."
        )
    }

    @Test
    fun testHighwayLike() {
        runCalibration(
            traceId = "153bc543-230f-41d3-b07a-36223c37e92c",
            description = "Pomeroy to fort. highway like."
        )
    }

    @Test
    fun testStillness() {
        runCalibration(
            traceId = "264ce16e-9a36-4a05-a7f9-eeead996ec2f",
            description = "22 seconds of nothingness"
        )
    }

    @Test
    fun testCityTraffic2() {
        runCalibration(
            traceId = "aa9b96f2-d579-4603-93a0-dd43f3bb5ca0",
            description = "going home from fort"
        )
    }

    @Test
    fun testCityTraffic1() {
        runCalibration(
            traceId = "ba19d424-ffc2-4964-827d-163bc193d7aa",
            description = "Aggressive style. low city traffic. home to Pomeroy center."
        )
    }

    @Test
    fun testStationaryPhone() {
        runCalibration(
            traceId = "e739b57d-9d4d-40e0-924b-7521027eb77d",
            description = "Stationary Phone Ground Truth (Phone sitting on a flat desk)"
        )
    }

    /**
     * Calibration logic for a given trace.
     *
     * NOTE ON STATUS (S) EVENTS:
     * Current legacy traces have corrupted timestamps for Status events because they were
     * recorded using System.nanoTime() instead of SystemClock.elapsedRealtimeNanos().
     * This creates a ~5-day gap in the monotonic timeline which starves the fusion logic.
     *
     * We currently filter these out to allow testing of IMU and GPS fusion.
     * This logic will be removed once new traces are recorded with the fixed FileRawSink.
     */
    private fun runCalibration(traceId: String, description: String) {
        val traceFileName = "trace_$traceId.csv"
        val allEvents = try {
            SensorReplayer
                .fromRawTraceResource("src/test/resources/raw_traces/$traceFileName")
                .getEvents()
        } catch (e: Exception) {
            println("FAILED to load trace $traceFileName: ${e.message}")
            return
        }

        // TODO Don't hardcode my local path
        val resultsFile =
            File("/Users/francoismonniot/Projects/github.com/fmonniot/Speed/app/calibration_reports/${name.methodName}.txt")

        // This reset the results file for each run
        resultsFile.writeText("=".repeat(80) + "\n")
        resultsFile.appendText("CALIBRATION ANALYSIS: $description\n")
        resultsFile.appendText("Trace File: $traceFileName\n")
        resultsFile.appendText("Generated at: ${LocalDateTime.now()}\n")
        resultsFile.appendText("=".repeat(80) + "\n\n")

        // 1. Raw Data Stats & Timeline Analysis
        val imuEvents = allEvents.filterIsInstance<RawEvent.Imu>()
        val gpsEvents = allEvents.filterIsInstance<RawEvent.Gps>()
        val statusEvents = allEvents.filterIsInstance<RawEvent.Status>()

        resultsFile.appendText("Raw Event Statistics:\n")
        resultsFile.appendText("- Total Events: ${allEvents.size}\n")
        if (allEvents.isNotEmpty()) {
            resultsFile.appendText(
                "- Trace Duration (Raw): %.2f seconds\n"
                    .format((allEvents.last().timestampNs - allEvents.first().timestampNs) / 1_000_000_000f)
            )
        }
        if (imuEvents.isNotEmpty()) {
            resultsFile.appendText(
                "- IMU Events: ${imuEvents.size} | Range: ${imuEvents.first().timestampNs} to ${imuEvents.last().timestampNs} (%.2f s)\n"
                    .format((imuEvents.last().timestampNs - imuEvents.first().timestampNs) / 1_000_000_000f)
            )
        }
        if (gpsEvents.isNotEmpty()) {
            resultsFile.appendText(
                "- GPS Events: ${gpsEvents.size} | Range: ${gpsEvents.first().timestampNs} to ${gpsEvents.last().timestampNs} (%.2f s)\n"
                    .format((gpsEvents.last().timestampNs - gpsEvents.first().timestampNs) / 1_000_000_000f)
            )
        }
        if (statusEvents.isNotEmpty()) {
            resultsFile.appendText("- Status Events: ${statusEvents.size} | Range: ${statusEvents.first().timestampNs} to ${statusEvents.last().timestampNs}\n")
            resultsFile.appendText("  (Note: Corrupted timestamps detected in legacy traces for Status events)\n")
        }

        // Timeline Normalization: We exclude outliers (Status events using wrong clock epoch)
        val baseTs = imuEvents.firstOrNull()?.timestampNs ?: 0L
        val events = allEvents.filter {
            it !is RawEvent.Status && Math.abs(it.timestampNs - baseTs) < 3600_000_000_000L
        }.sortedBy { it.timestampNs }

        resultsFile.appendText(
            "\nFinal Filtered Duration: %.2f seconds\n\n"
                .format((events.last().timestampNs - events.first().timestampNs) / 1_000_000_000f)
        )

        // 2. Threshold Context
        resultsFile.appendText("Active Thresholds (from VelocityFusion):\n")
        resultsFile.appendText("- ZUPT IMU Mag: ${VelocityFusion.ZUPT_IMU_MAGNITUDE_THRESHOLD} m/s²\n")
        resultsFile.appendText("- ZUPT IMU Var: ${VelocityFusion.ZUPT_IMU_VARIANCE_THRESHOLD}\n")
        resultsFile.appendText("- ZUPT GPS Speed: ${VelocityFusion.ZUPT_GPS_SPEED_THRESHOLD_MS} m/s\n")
        resultsFile.appendText("- GPS Max Age: ${VelocityFusion.GPS_MAX_AGE_MS} ms\n\n")

        // 2b. GPS Tick Analysis
        val gpsStats = analyzeGpsTicks(events)
        val freshPercent =
            if (gpsStats.totalTicks > 0) (gpsStats.freshGpsTicks.toFloat() / gpsStats.totalTicks) * 100 else 0f
        resultsFile.appendText("GPS Tick Analysis:\n")
        resultsFile.appendText("- Total Ticks: ${gpsStats.totalTicks}\n")
        resultsFile.appendText("- Ticks with Fresh GPS (<${VelocityFusion.GPS_MAX_AGE_MS}ms): ${gpsStats.freshGpsTicks} ($freshPercent%%)\n")
        resultsFile.appendText("- Ticks with Stale GPS: ${gpsStats.staleGpsTicks}\n")
        resultsFile.appendText("- GPS Age (ms): min=${gpsStats.minAgeMs.toInt()}, max=${gpsStats.maxAgeMs.toInt()}, avg=${gpsStats.avgAgeMs.toInt()}\n")
        resultsFile.appendText(
            "- GPS Speed (m/s): min=${gpsStats.minSpeedMs}, max=${gpsStats.maxSpeedMs}, avg=${gpsStats.avgSpeedMs} (%.2f km/h)\n\n"
                .format(gpsStats.avgSpeedMs * 3.6f)
        )

        // 3. Grid Search Results (Q/R)
        val qValues = listOf(0.01f, 0.05f, 0.1f, 0.5f)
        val rValues = listOf(0.1f, 0.3f, 0.5f, 1.0f)

        resultsFile.appendText("|  Q   |  R   | Max Speed (km/h) | Avg Speed (km/h) | ZUPT % |\n")
        resultsFile.appendText("|------|------|------------------|------------------|--------|\n")

        for (q in qValues) {
            for (r in rValues) {
                val fusion = VelocityFusion(kalmanQ = q, kalmanR = r)
                val results = runTickLoop(events, fusion)

                val speeds = results.map { it.speedMs }
                val maxSpeedKmh = (speeds.maxOrNull() ?: 0f) * 3.6f
                val avgSpeedKmh = if (speeds.isNotEmpty()) speeds.average().toFloat() * 3.6f else 0f
                val zuptPercent = if (results.isNotEmpty()) (results.count { it.isStationary }
                    .toFloat() / results.size) * 100f else 0f

                resultsFile.appendText(
                    "| %.2f | %.2f |     %09.6f     |     %09.6f     |  %.1f%%  |\n"
                        .format(q, r, maxSpeedKmh, avgSpeedKmh, zuptPercent)
                )
            }
        }

        // 4. ZUPT Threshold Grid Search (fixed Q=0.3, R=0.3)
        resultsFile.appendText("\n--- ZUPT Threshold Analysis (Q=0.3, R=0.3) ---\n")
        resultsFile.appendText("| IMU Mag | IMU Var | GPS Spd | Max Speed | Avg Speed | ZUPT %% |\n")
        resultsFile.appendText("|---------|---------|---------|-----------|-----------|--------|\n")

        val imuMagThresholds = listOf(0.4f, 1.0f, 2.0f, 4.0f)
        val imuVarThresholds = listOf(0.015f, 0.1f, 0.5f, 1.0f)
        val gpsSpeedThresholds = listOf(0.5f, 2.0f, 4.0f)

        for (gpsThresh in gpsSpeedThresholds) {
            for (magThresh in imuMagThresholds) {
                for (varThresh in imuVarThresholds) {
                    val fusion = VelocityFusion(
                        kalmanQ = 0.3f, kalmanR = 0.3f,
                        zuptImuMagnitudeThreshold = magThresh,
                        zuptImuVarianceThreshold = varThresh,
                        zuptGpsSpeedThreshold = gpsThresh
                    )
                    val results = runTickLoop(events, fusion)

                    val speeds = results.map { it.speedMs }
                    val maxSpeedKmh = (speeds.maxOrNull() ?: 0f) * 3.6f
                    val avgSpeedKmh =
                        if (speeds.isNotEmpty()) speeds.average().toFloat() * 3.6f else 0f
                    val zuptPercent = if (results.isNotEmpty()) (results.count { it.isStationary }
                        .toFloat() / results.size) * 100f else 0f

                    resultsFile.appendText(
                        "|   %.1f   |  %.3f  |   %.1f   |   %6.2f   |   %5.2f   |  %.1f%%  |\n"
                            .format(
                                magThresh,
                                varThresh,
                                gpsThresh,
                                maxSpeedKmh,
                                avgSpeedKmh,
                                zuptPercent
                            )
                    )
                }
            }
        }

        // 5. Best Q/R with relaxed ZUPT - with segment analysis
        resultsFile.appendText("\n--- Q/R Sweep with Relaxed ZUPT (Mag=4.0, Var=1.0, GPS=4.0) ---\n")
        resultsFile.appendText("|  Q   |  R   | Max Speed | Avg Speed | ZUPT %% | Moving Speed Ratio | Moving ZUPT FP%% |\n")
        resultsFile.appendText("|------|------|-----------|-----------|---------|-------------------|------------------|\n")

        val qSweep = listOf(0.01f, 0.05f, 0.1f, 0.2f, 0.3f, 0.5f, 1.0f)
        for (q in qSweep) {
            for (r in listOf(0.1f, 0.3f)) {
                val fusion = VelocityFusion(
                    kalmanQ = q, kalmanR = r,
                    zuptImuMagnitudeThreshold = 4.0f, zuptImuVarianceThreshold = 1.0f,
                    zuptGpsSpeedThreshold = 4.0f
                )
                val results = runTickLoop(events, fusion)
                val speeds = results.map { it.speedMs }
                val maxKmh = (speeds.maxOrNull() ?: 0f) * 3.6f
                val avgKmh = if (speeds.isNotEmpty()) speeds.average().toFloat() * 3.6f else 0f
                val zupt = if (results.isNotEmpty()) (results.count { it.isStationary }
                    .toFloat() / results.size) * 100f else 0f

                // Segment analysis
                val segments = analyzeSegments(events, fusion)
                val speedRatioPct = segments.speedRatio * 100f

                resultsFile.appendText(
                    "| %.2f | %.1f |   %6.2f   |   %5.2f   |  %.1f%%  |       %.1f%%        |      %.1f%%        |\n"
                        .format(
                            q,
                            r,
                            maxKmh,
                            avgKmh,
                            zupt,
                            speedRatioPct,
                            segments.movingZuptFalsePosPercent
                        )
                )
            }
        }

        // 6. Segment Analysis (stationary vs moving)
        resultsFile.appendText("\n--- Segment Analysis: Stationary vs Moving ---\n")
        val segmentResults = analyzeSegments(events, VelocityFusion(kalmanQ = 0.3f, kalmanR = 0.3f))
        resultsFile.appendText("Stationary (GPS < 1 m/s):\n")
        resultsFile.appendText("  - Duration: %.1f%% of trace\n".format(segmentResults.stationaryPercent))
        resultsFile.appendText("  - Filter Avg Speed: %.2f km/h\n".format(segmentResults.stationaryFilterAvg * 3.6f))
        resultsFile.appendText("  - ZUPT Activation: %.1f%%\n\n".format(segmentResults.stationaryZuptPercent))

        resultsFile.appendText("Moving (GPS >= 1 m/s):\n")
        resultsFile.appendText("  - Duration: %.1f%% of trace\n".format(segmentResults.movingPercent))
        resultsFile.appendText("  - GPS Avg Speed: %.2f km/h\n".format(segmentResults.movingGpsAvg * 3.6f))
        resultsFile.appendText("  - Filter Avg Speed: %.2f km/h\n".format(segmentResults.movingFilterAvg * 3.6f))
        resultsFile.appendText("  - Speed Ratio (Filter/GPS): %.2f\n".format(segmentResults.speedRatio))
        resultsFile.appendText("  - ZUPT False Positive Rate: %.1f%%\n\n".format(segmentResults.movingZuptFalsePosPercent))

        resultsFile.appendText("\n--- ZUPT Diagnostic (First 20 valid ticks) for Default Config ---\n")
        runDiagnostic(events, VelocityFusion(0.5f, 0.3f), resultsFile)
    }

    private data class TickResult(val speedMs: Float, val isStationary: Boolean)

    private fun runTickLoop(events: List<RawEvent>, fusion: VelocityFusion): List<TickResult> {
        val output = mutableListOf<TickResult>()
        if (events.isEmpty()) return output

        var lastTickTimeNs = events.first().timestampNs
        val rotationMatrix = FloatArray(9)
        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()

        val tickIntervalNs = 100_000_000L
        var nextTickNs = lastTickTimeNs + tickIntervalNs

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(
                        event.rotationVector,
                        rotationMatrix
                    )
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }

                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= nextTickNs) {
                val dt = (nextTickNs - lastTickTimeNs) / 1_000_000_000f
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (nextTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(dt, imuWindow, gpsObs)
                output.add(TickResult(result.speedMs, result.isStationary))

                lastTickTimeNs = nextTickNs
                nextTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
            }
        }
        return output
    }

    private fun runDiagnostic(events: List<RawEvent>, fusion: VelocityFusion, resultsFile: File) {
        if (events.isEmpty()) return

        var lastTickTimeNs = events.first().timestampNs
        val rotationMatrix = FloatArray(9)
        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()
        val tickIntervalNs = 100_000_000L
        var nextTickNs = lastTickTimeNs + tickIntervalNs
        var tickCount = 0

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(
                        event.rotationVector,
                        rotationMatrix
                    )
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }

                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= nextTickNs && tickCount < 20) {
                val sampleCount = imuSamplesInWindow.size
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (nextTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(0.1f, imuWindow, gpsObs)

                val imuMag = imuWindow?.accelMagnitude ?: -1f
                val imuVar = imuWindow?.variance ?: -1f
                val gpsSpeed = gpsObs?.speedMs ?: -1f

                resultsFile.appendText(
                    "Tick %2d: Speed=%.4f, ZUPT=%b | Samples=%d, IMU_Mag=%.4f, IMU_Var=%.6f, GPS_Speed=%.4f\n"
                        .format(
                            tickCount,
                            result.speedMs,
                            result.isStationary,
                            sampleCount,
                            imuMag,
                            imuVar,
                            gpsSpeed
                        )
                )

                lastTickTimeNs = nextTickNs
                nextTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
                tickCount++
            }
        }
    }

    private fun analyzeGpsTicks(events: List<RawEvent>): GpsTickStats {
        if (events.isEmpty()) return GpsTickStats(0, 0, 0, 0f, 0f, 0f, 0f, 0f, 0f)

        val firstTs = events.first().timestampNs
        val tickIntervalNs = 100_000_000L
        var nextTickNs = firstTs + tickIntervalNs

        var latestGps: RawEvent.Gps? = null
        var totalTicks = 0
        var freshGpsTicks = 0
        var staleGpsTicks = 0
        val gpsAges = mutableListOf<Float>()
        val gpsSpeeds = mutableListOf<Float>()

        for (event in events) {
            when (event) {
                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= nextTickNs) {
                totalTicks++

                if (latestGps != null) {
                    val ageMs = (nextTickNs - latestGps.timestampNs) / 1_000_000L.toFloat()
                    gpsAges.add(ageMs)
                    gpsSpeeds.add(latestGps.speedMs)

                    if (ageMs < VelocityFusion.GPS_MAX_AGE_MS) {
                        freshGpsTicks++
                    } else {
                        staleGpsTicks++
                    }
                }

                nextTickNs += tickIntervalNs
            }
        }

        val avgAge = if (gpsAges.isNotEmpty()) gpsAges.average().toFloat() else 0f
        val minAge = gpsAges.minOrNull() ?: 0f
        val maxAge = gpsAges.maxOrNull() ?: 0f

        val avgSpeed = if (gpsSpeeds.isNotEmpty()) gpsSpeeds.average().toFloat() else 0f
        val minSpeed = gpsSpeeds.minOrNull() ?: 0f
        val maxSpeed = gpsSpeeds.maxOrNull() ?: 0f

        return GpsTickStats(
            totalTicks,
            freshGpsTicks,
            staleGpsTicks,
            minAge,
            maxAge,
            avgAge,
            minSpeed,
            maxSpeed,
            avgSpeed
        )
    }

    private data class SegmentStats(
        val stationaryPercent: Float,
        val stationaryFilterAvg: Float,
        val stationaryZuptPercent: Float,
        val movingPercent: Float,
        val movingGpsAvg: Float,
        val movingFilterAvg: Float,
        val speedRatio: Float,
        val movingZuptFalsePosPercent: Float
    )

    private fun analyzeSegments(events: List<RawEvent>, fusion: VelocityFusion): SegmentStats {
        if (events.isEmpty()) return SegmentStats(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

        val firstTs = events.first().timestampNs
        val tickIntervalNs = 100_000_000L
        var nextTickNs = firstTs + tickIntervalNs

        var latestGps: RawEvent.Gps? = null
        val imuSamplesInWindow = mutableListOf<ImuSample>()
        val rotationMatrix = FloatArray(9)

        val stationarySpeeds = mutableListOf<Float>()
        val stationaryZuptCount = mutableListOf<Boolean>()
        val movingGpsSpeeds = mutableListOf<Float>()
        val movingFilterSpeeds = mutableListOf<Float>()
        val movingZuptCount = mutableListOf<Boolean>()

        for (event in events) {
            when (event) {
                is RawEvent.Imu -> {
                    CoordinateTransformer.getRotationMatrixFromVector(
                        event.rotationVector,
                        rotationMatrix
                    )
                    val worldAccel = CoordinateTransformer.transform(event.accel, rotationMatrix)
                    imuSamplesInWindow.add(ImuSample(worldAccel, event.timestampNs))
                }

                is RawEvent.Gps -> latestGps = event
                else -> {}
            }

            while (event.timestampNs >= nextTickNs) {
                val imuWindow = ImuWindow.fromSamples(imuSamplesInWindow)
                val gpsObs = latestGps?.let {
                    GpsObservation(
                        speedMs = it.speedMs,
                        bearingRad = it.bearing * (PI.toFloat() / 180f),
                        accuracyM = it.accuracyM,
                        ageMs = (nextTickNs - it.timestampNs) / 1_000_000L
                    )
                }

                val result = fusion.tick(0.1f, imuWindow, gpsObs)
                val gpsSpeed = gpsObs?.speedMs ?: -1f

                // Segment based on GPS speed (ground truth)
                if (gpsSpeed >= 0f && gpsSpeed < 1.0f) {
                    // Stationary
                    stationarySpeeds.add(result.speedMs)
                    stationaryZuptCount.add(result.isStationary)
                } else if (gpsSpeed >= 1.0f) {
                    // Moving
                    movingGpsSpeeds.add(gpsSpeed)
                    movingFilterSpeeds.add(result.speedMs)
                    movingZuptCount.add(result.isStationary)
                }

                nextTickNs += tickIntervalNs
                imuSamplesInWindow.clear()
            }
        }

        val totalTicks = stationarySpeeds.size + movingFilterSpeeds.size
        if (totalTicks == 0) return SegmentStats(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

        val stationaryPercent = (stationarySpeeds.size.toFloat() / totalTicks) * 100f
        val stationaryFilterAvg =
            if (stationarySpeeds.isNotEmpty()) stationarySpeeds.average().toFloat() else 0f
        val stationaryZuptPercent = if (stationaryZuptCount.isNotEmpty())
            (stationaryZuptCount.count { it }.toFloat() / stationaryZuptCount.size) * 100f else 0f

        val movingPercent = (movingFilterSpeeds.size.toFloat() / totalTicks) * 100f
        val movingGpsAvg =
            if (movingGpsSpeeds.isNotEmpty()) movingGpsSpeeds.average().toFloat() else 0f
        val movingFilterAvg =
            if (movingFilterSpeeds.isNotEmpty()) movingFilterSpeeds.average().toFloat() else 0f
        val speedRatio = if (movingGpsAvg > 0) movingFilterAvg / movingGpsAvg else 0f
        val movingZuptFalsePos = if (movingZuptCount.isNotEmpty())
            (movingZuptCount.count { it }.toFloat() / movingZuptCount.size) * 100f else 0f

        return SegmentStats(
            stationaryPercent, stationaryFilterAvg, stationaryZuptPercent,
            movingPercent, movingGpsAvg, movingFilterAvg, speedRatio, movingZuptFalsePos
        )
    }
}
