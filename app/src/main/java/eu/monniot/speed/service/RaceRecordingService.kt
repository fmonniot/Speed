package eu.monniot.speed.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.LocationManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import eu.monniot.speed.MainActivity
import eu.monniot.speed.R
import eu.monniot.speed.data.*
import eu.monniot.speed.fusion.DataFusion
import eu.monniot.speed.sensor.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

data class ServiceState(
    val isRecording: Boolean = false,
    val isSensorsEnabled: Boolean = false,
    val sessionId: String? = null,
    val latestPoint: DataPoint? = null,
    val pointCount: Int = 0,
    val elapsedSeconds: Int = 0,
    val sessionStartTimeMs: Long = 0L,
    val sessionStartElapsedNs: Long = 0L,
    val satellites: SatelliteInfo = SatelliteInfo(),
    // Live metrics even when not recording
    val currentSpeedMs: Float = 0f,
    val currentAccelMs2: Float = 0f,
    val currentG: Float = 0f,
    // E1: live lean (+ = right) and lateral G (+ = toward rider's right) for the Live HUD.
    val currentLeanDeg: Float = 0f,
    val currentLateralG: Float = 0f,
    val currentAccuracyM: Float? = null,
    // Battery metrics
    val batteryPercent: Int? = null,
    val batteryWattage: Float? = null,
    val batteryCapacityMah: Int? = null,
    val batteryTimeRemainingMs: Long? = null
)

class RaceRecordingService : LifecycleService() {

    private lateinit var repository: RaceRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var gpsCollector: GpsCollector
    private lateinit var imuCollector: ImuCollector
    private var dataFusion: DataFusion? = null
    private var rawSink: RawSensorSink? = null
    
    private var wakeLock: PowerManager.WakeLock? = null

    // Auto-pause (§4.9): when on, stationary points are not persisted so the recorded track and
    // stats reflect moving time only. Mirrored from the setting flow; read on the fusion thread.
    @Volatile private var autoPauseEnabled = false

    private var batteryVoltageMv: Int = 0
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                batteryVoltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            }
        }
    }
    
    companion object {
        const val ACTION_START_SENSORS = "ACTION_START_SENSORS"
        const val ACTION_STOP_SENSORS = "ACTION_STOP_SENSORS"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "race_recording"
        const val NOTIFICATION_ID = 1

        // Below this fused speed (m/s) the rider is treated as stationary for auto-pause.
        // Matches SessionStatsComputer's "moving" cutoff so distance/moving% stay consistent.
        private const val AUTO_PAUSE_SPEED_THRESHOLD_MS = 0.5f

        private val _state = MutableStateFlow(ServiceState())
        val state: StateFlow<ServiceState> = _state.asStateFlow()
    }

    inner class LocalBinder : Binder() {
        fun getService(): RaceRecordingService = this@RaceRecordingService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        val db = RaceDatabase.getDatabase(this)
        repository = RaceRepository(db.dataPointDao(), db.segmentDao())
        settingsRepository = SettingsRepository(this)
        
        // Initialize Raw Sink
        rawSink = FileRawSink(this)

        gpsCollector = GpsCollector(
            LocationServices.getFusedLocationProviderClient(this),
            getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            rawSink
        )
        imuCollector = ImuCollector(
            getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager,
            rawSink
        )

        dataFusion = DataFusion(
            gpsCollector.locationFlow,
            gpsCollector.satellitesFlow,
            imuCollector.imuFlow,
            lifecycleScope
        ).apply {
            start()
            lifecycleScope.launch {
                dataPointFlow.collect { point ->
                    // Update live metrics in state
                    _state.update { 
                        it.copy(
                            currentSpeedMs = point.derivedSpeedMs ?: 0f,
                            currentAccelMs2 = point.derivedAccelMs2 ?: 0f,
                            currentG = point.accelMagnitude / 9.81f,
                            currentLeanDeg = point.leanAngleDeg ?: 0f,
                            currentLateralG = point.lateralGz ?: 0f,
                            currentAccuracyM = point.gpsAccuracyM,
                            satellites = SatelliteInfo(point.satellitesUsed ?: 0, point.satellitesVisible ?: 0)
                        )
                    }

                    // Save to DB and update recording stats if recording. When auto-pause is on,
                    // skip persisting points captured while stationary so the session pauses itself.
                    if (_state.value.isRecording) {
                        val stationary = (point.derivedSpeedMs ?: 0f) < AUTO_PAUSE_SPEED_THRESHOLD_MS
                        if (!(autoPauseEnabled && stationary)) {
                            repository.insertDataPoint(point)
                            _state.update {
                                it.copy(
                                    latestPoint = point,
                                    pointCount = it.pointCount + 1,
                                    elapsedSeconds = ((point.elapsedRealtimeNs - it.sessionStartElapsedNs) / 1_000_000_000).toInt()
                                )
                            }
                            if (_state.value.pointCount % 10 == 0) {
                                updateNotification(point)
                            }
                        }
                    }
                }
            }
        }

        // Keep the auto-pause flag in sync with the setting (read on the fusion thread above).
        lifecycleScope.launch {
            settingsRepository.autoPause.collect { autoPauseEnabled = it }
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        startBatteryStatsCollection()
    }

    private fun startBatteryStatsCollection() {
        lifecycleScope.launch {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            while (isActive) {
                val currentUa = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                // currentUa is positive when charging, negative when discharging on most devices.
                
                val wattage = if (batteryVoltageMv > 0) {
                    (currentUa.toFloat() / 1_000_000f) * (batteryVoltageMv.toFloat() / 1_000f)
                } else {
                    null
                }

                val chargeCounterUah = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                val capacityPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                
                val totalCapacityMah = if (capacityPercent > 0) {
                    (chargeCounterUah / 1000.0 / (capacityPercent / 100.0)).toInt()
                } else {
                    null
                }

                // Estimate time remaining
                var timeRemainingMs: Long? = null
                if (currentUa < 0) {
                    // Discharging: remaining mAh / current mA
                    val currentMa = abs(currentUa) / 1000.0
                    val remainingMah = chargeCounterUah / 1000.0
                    if (currentMa > 0) {
                        timeRemainingMs = (remainingMah / currentMa * 3600 * 1000).toLong()
                    }
                } else if (currentUa > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Charging
                    val remaining = batteryManager.computeChargeTimeRemaining()
                    if (remaining > 0) timeRemainingMs = -remaining
                }

                _state.update {
                    it.copy(
                        batteryPercent = capacityPercent.takeIf { p -> p in 0..100 },
                        batteryWattage = wattage,
                        batteryCapacityMah = totalCapacityMah,
                        batteryTimeRemainingMs = timeRemainingMs
                    )
                }
                
                delay(5000) // Update every 5 seconds
            }
        }
    }

    override fun onDestroy() {
        stopSensors()
        unregisterReceiver(batteryReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_SENSORS -> startSensors()
            ACTION_STOP_SENSORS -> stopSensors()
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        
        return START_STICKY
    }

    private fun startSensors() {
        // startForeground must run promptly; read the rate settings and start the collectors on a
        // coroutine (the collectors gate on _isActive, so a few ms of latency is harmless).
        startForeground(NOTIFICATION_ID, createNotification("Sensors active. This will drain your battery."))
        lifecycleScope.launch {
            val gpsHz = settingsRepository.gpsRateHz.first().coerceAtLeast(1)
            val imuHz = settingsRepository.imuRateHz.first().coerceAtLeast(1)
            gpsCollector.start(intervalMs = (1000L / gpsHz).coerceAtLeast(1L))
            imuCollector.start(samplingPeriodUs = 1_000_000 / imuHz)
            _state.update { it.copy(isSensorsEnabled = true) }
        }
    }

    private fun stopSensors() {
        if (_state.value.isRecording) {
            stopRecording()
        }
        gpsCollector.stop()
        imuCollector.stop()
        _state.update { it.copy(isSensorsEnabled = false, satellites = SatelliteInfo(), currentAccuracyM = null) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startRecording() {
        if (_state.value.isRecording) return
        
        // Ensure sensors are started if they weren't
        if (!_state.value.isSensorsEnabled) {
            startSensors()
        }

        val sessionId = UUID.randomUUID().toString()
        val startNs = SystemClock.elapsedRealtimeNanos()
        val startMs = System.currentTimeMillis()

        _state.update {
            it.copy(
                isRecording = true,
                sessionId = sessionId,
                sessionStartTimeMs = startMs,
                sessionStartElapsedNs = startNs,
                pointCount = 0,
                elapsedSeconds = 0,
                latestPoint = null
            )
        }
        
        dataFusion?.currentSessionId = sessionId

        // Start raw logging if enabled in settings
        lifecycleScope.launch {
            if (settingsRepository.recordRawTraces.first()) {
                rawSink?.start(sessionId)
            }
        }

        startForeground(NOTIFICATION_ID, createNotification("Recording... (Battery drain high)"))

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RaceLogger::Recording").apply {
                acquire()
            }
        }

        lifecycleScope.launch {
            repository.startSession(Session(sessionId, startMs, null, 0, 0f))
        }
    }

    private fun stopRecording() {
        if (!_state.value.isRecording) return

        dataFusion?.currentSessionId = null
        wakeLock?.let { if (it.isHeld) it.release() }
        
        rawSink?.stop()

        val currentState = _state.value
        lifecycleScope.launch {
            currentState.sessionId?.let { sid ->
                val points = repository.getPointsForSession(sid)
                // E2/E3: compute and persist per-session aggregates at finalize.
                val stats = eu.monniot.speed.domain.SessionStatsComputer.compute(points)
                repository.updateSession(
                    Session(
                        sessionId = sid,
                        startTimeMs = currentState.sessionStartTimeMs,
                        endTimeMs = System.currentTimeMillis(),
                        pointCount = points.size,
                        maxSpeedMs = stats.maxSpeedMs,
                        distanceM = stats.distanceM,
                        avgSpeedMs = stats.avgSpeedMs,
                        maxLateralG = stats.maxLateralG,
                        maxLeanDeg = stats.maxLeanDeg,
                        hardBrakeG = stats.hardBrakeG,
                        movingPercent = stats.movingPercent,
                    )
                )
                // E5: match this ride's track against defined segments and record attempts.
                runCatching {
                    val segments = repository.getSegmentsForMatching()
                    if (segments.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        eu.monniot.speed.domain.SegmentMatcher.match(points, segments).forEach { m ->
                            repository.insertAttempt(
                                SegmentAttempt(
                                    segmentId = m.segmentId,
                                    sessionId = sid,
                                    elapsedTimeMs = m.elapsedTimeMs,
                                    dateMs = now,
                                    maxSpeedMs = m.maxSpeedMs,
                                    maxLateralG = m.maxLateralG,
                                    maxLeanDeg = m.maxLeanDeg,
                                )
                            )
                        }
                    }
                }
                // F8: auto-name the ride from its start location. Runs in its own coroutine
                // (off the main thread, on IO) so it never blocks finalisation below.
                lifecycleScope.launch { autoNameSessionFromLocation(sid, points) }
            }
            _state.update {
                it.copy(
                    isRecording = false,
                    sessionId = null,
                    sessionStartTimeMs = 0L,
                    sessionStartElapsedNs = 0L,
                    latestPoint = null,
                    pointCount = 0,
                    elapsedSeconds = 0
                )
            }
            startForeground(NOTIFICATION_ID, createNotification("Ready to record (Battery drain high)"))
        }
    }

    /**
     * F8: reverse-geocode the session's first GPS fix and set [Session.name] to the locality,
     * but only when the user hasn't named the ride (so a rename is never overwritten). If no GPS
     * fix was captured the name is left null and the UI falls back to the weekday label.
     */
    private suspend fun autoNameSessionFromLocation(sessionId: String, points: List<DataPoint>) {
        val firstFix = points.firstOrNull { it.latitude != null && it.longitude != null } ?: return
        val lat = firstFix.latitude ?: return
        val lon = firstFix.longitude ?: return

        val locality = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val geocoder = Geocoder(this@RaceRecordingService, Locale.getDefault())
                @Suppress("DEPRECATION") // async overload requires API 33; sync call is off-main here
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }.getOrNull()
        }?.takeIf { it.isNotBlank() } ?: return

        // Re-read so a rename made between finalisation and this lookup is not clobbered.
        val current = repository.getSession(sessionId) ?: return
        if (current.name.isNullOrBlank()) {
            repository.updateSession(current.copy(name = locality))
        }
    }

    private fun createNotification(content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        // F2: Stop action — sends ACTION_STOP_SENSORS to this service so the user can stop
        // recording and tear the service down from the notification without re-opening the app.
        // stopSensors() also finalises any in-progress recording and removes the notification.
        val stopIntent = Intent(this, RaceRecordingService::class.java).apply {
            action = ACTION_STOP_SENSORS
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Speed")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_speed_logo)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(point: DataPoint) {
        val speedKmh = (point.gpsSpeedMs ?: 0f) * 3.6f
        val notification = createNotification("Current Speed: %.1f km/h (Battery draining)".format(speedKmh))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
