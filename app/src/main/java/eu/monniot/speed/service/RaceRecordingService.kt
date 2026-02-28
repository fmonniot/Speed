package eu.monniot.speed.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import eu.monniot.speed.MainActivity
import eu.monniot.speed.data.*
import eu.monniot.speed.fusion.DataFusion
import eu.monniot.speed.sensor.GpsCollector
import eu.monniot.speed.sensor.ImuCollector
import eu.monniot.speed.sensor.SatelliteInfo
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ServiceState(
    val isRecording: Boolean = false,
    val sessionId: String? = null,
    val latestPoint: DataPoint? = null,
    val pointCount: Int = 0,
    val elapsedSeconds: Int = 0,
    val sessionStartElapsedNs: Long = 0L,
    val satellites: SatelliteInfo = SatelliteInfo(),
    // Live metrics even when not recording
    val currentSpeedMs: Float = 0f,
    val currentAccelMs2: Float = 0f,
    val currentG: Float = 0f,
    val currentAccuracyM: Float? = null
)

class RaceRecordingService : LifecycleService() {

    private lateinit var repository: RaceRepository
    private lateinit var gpsCollector: GpsCollector
    private lateinit var imuCollector: ImuCollector
    private var dataFusion: DataFusion? = null
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val ACTION_START_SENSORS = "ACTION_START_SENSORS"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "race_recording"
        const val NOTIFICATION_ID = 1

        private val _state = MutableStateFlow(ServiceState())
        val state: StateFlow<ServiceState> = _state.asStateFlow()
    }

    inner class LocalBinder : Binder() {
        fun getService(): RaceRecordingService = this@RaceRecordingService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        val dao = RaceDatabase.getDatabase(this).dataPointDao()
        repository = RaceRepository(dao)
        
        gpsCollector = GpsCollector(
            LocationServices.getFusedLocationProviderClient(this),
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        )
        imuCollector = ImuCollector(getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager)

        // Sensors stay active for the life of the service
        gpsCollector.start()
        imuCollector.start()

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
                            currentAccuracyM = point.gpsAccuracyM,
                            satellites = SatelliteInfo(point.satellitesUsed ?: 0, point.satellitesVisible ?: 0)
                        )
                    }

                    // Save to DB and update recording stats if recording
                    if (_state.value.isRecording) {
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

    override fun onDestroy() {
        gpsCollector.stop()
        imuCollector.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
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
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        
        return START_STICKY
    }

    private fun startSensors() {
        startForeground(NOTIFICATION_ID, createNotification("Ready to record"))
    }

    private fun startRecording() {
        if (_state.value.isRecording) return
        
        val sessionId = UUID.randomUUID().toString()
        val startNs = SystemClock.elapsedRealtimeNanos()
        val startMs = System.currentTimeMillis()

        _state.update {
            it.copy(
                isRecording = true,
                sessionId = sessionId,
                sessionStartElapsedNs = startNs,
                pointCount = 0,
                elapsedSeconds = 0,
                latestPoint = null
            )
        }
        
        dataFusion?.currentSessionId = sessionId

        startForeground(NOTIFICATION_ID, createNotification("Recording..."))

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

        val currentState = _state.value
        lifecycleScope.launch {
            currentState.sessionId?.let { sid ->
                val points = repository.getPointsForSession(sid)
                val maxSpeed = points.maxByOrNull { it.gpsSpeedMs ?: 0f }?.gpsSpeedMs ?: 0f
                repository.updateSession(Session(sid, 0L, System.currentTimeMillis(), points.size, maxSpeed))
            }
            _state.update { 
                it.copy(
                    isRecording = false,
                    sessionId = null,
                    latestPoint = null,
                    pointCount = 0,
                    elapsedSeconds = 0
                )
            }
            startForeground(NOTIFICATION_ID, createNotification("Ready to record"))
        }
    }

    private fun createNotification(content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Race Logger")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(point: DataPoint) {
        val speedKmh = (point.gpsSpeedMs ?: 0f) * 3.6f
        val notification = createNotification("Current Speed: %.1f km/h".format(speedKmh))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
