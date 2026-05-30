package eu.monniot.speed.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.monniot.speed.data.RaceDatabase
import eu.monniot.speed.data.RaceRepository
import eu.monniot.speed.data.Segment
import eu.monniot.speed.data.SegmentListItem
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.domain.SessionStats
import eu.monniot.speed.domain.SessionStatsComputer
import eu.monniot.speed.data.SettingsRepository
import eu.monniot.speed.data.Units
import eu.monniot.speed.service.RaceRecordingService
import eu.monniot.speed.service.ServiceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import android.content.Intent
import eu.monniot.speed.data.Session
import kotlinx.coroutines.flow.first
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RaceRepository
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
    
    val serviceState: StateFlow<ServiceState> = RaceRecordingService.state
    val sessions: Flow<List<SessionSummary>>
    
    val autoStartSensors: StateFlow<Boolean> = settingsRepository.autoStartSensors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val recordRawTraces: StateFlow<Boolean> = settingsRepository.recordRawTraces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Redesign settings (C1) exposed for screens (C2/C3, D1/D2).
    val units: StateFlow<Units> = settingsRepository.units
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Units.METRIC)

    val darkTheme: StateFlow<Boolean> = settingsRepository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gpsRateHz: StateFlow<Int> = settingsRepository.gpsRateHz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_GPS_RATE_HZ)

    val imuRateHz: StateFlow<Int> = settingsRepository.imuRateHz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_IMU_RATE_HZ)

    val autoPause: StateFlow<Boolean> = settingsRepository.autoPause
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _exportUri = MutableSharedFlow<Uri>()
    val exportUri: SharedFlow<Uri> = _exportUri

    init {
        val db = RaceDatabase.getDatabase(application)
        repository = RaceRepository(db.dataPointDao(), db.segmentDao())
        sessions = repository.sessionSummaries
        
        // Auto-start sensors if the setting is enabled
        viewModelScope.launch {
            if (settingsRepository.autoStartSensors.first()) {
                startSensors()
            }
        }
    }

    fun setAutoStartSensors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoStartSensors(enabled)
        }
    }

    fun setRecordRawTraces(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRecordRawTraces(enabled)
        }
    }

    // Redesign settings setters (C1) — used by the D9 Settings screen.
    fun setGpsRateHz(hz: Int) = viewModelScope.launch { settingsRepository.setGpsRateHz(hz) }
    fun setImuRateHz(hz: Int) = viewModelScope.launch { settingsRepository.setImuRateHz(hz) }
    fun setAutoPause(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoPause(enabled) }
    fun setUnits(units: Units) = viewModelScope.launch { settingsRepository.setUnits(units) }
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }

    private fun startSensors() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_START_SENSORS
        }
        context.startForegroundService(intent)
    }

    fun toggleSensors() {
        val context = getApplication<Application>().applicationContext
        val isEnabled = serviceState.value.isSensorsEnabled
        val intent = Intent(context, RaceRecordingService::class.java).apply {
            action = if (isEnabled) RaceRecordingService.ACTION_STOP_SENSORS else RaceRecordingService.ACTION_START_SENSORS
        }
        if (isEnabled) {
            context.startService(intent)
        } else {
            context.startForegroundService(intent)
        }
    }

    fun startRecording() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_START_RECORDING
        }
        context.startForegroundService(intent)
    }

    fun stopRecording() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, RaceRecordingService::class.java).apply {
            action = RaceRecordingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            // Also delete raw trace if it exists
            val context = getApplication<Application>().applicationContext
            val rawFile = File(context.getExternalFilesDir(null), "raw_traces/trace_$sessionId.csv")
            if (rawFile.exists()) {
                rawFile.delete()
            }
        }
    }

    fun exportSession(sessionId: String) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val startNs = serviceState.value.sessionStartElapsedNs
            
            // Check if raw trace exists
            val rawFile = File(context.getExternalFilesDir(null), "raw_traces/trace_$sessionId.csv")
            
            val finalUri = if (rawFile.exists()) {
                // Export as a ZIP containing both processed CSV and raw CSV
                val zipFile = File(context.cacheDir, "race_session_${sessionId}.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    // 1. Add Processed Data
                    zos.putNextEntry(ZipEntry("processed_data.csv"))
                    repository.exportToCsv(sessionId, zos, startNs)
                    zos.closeEntry()
                    
                    // 2. Add Raw Data
                    zos.putNextEntry(ZipEntry("raw_trace.csv"))
                    FileInputStream(rawFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
                FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
            } else {
                // Export as single CSV (standard behavior)
                val exportFile = File(context.cacheDir, "race_session_${sessionId}.csv")
                exportFile.outputStream().use { stream ->
                    repository.exportToCsv(sessionId, stream, startNs)
                }
                FileProvider.getUriForFile(context, "${context.packageName}.provider", exportFile)
            }
            
            _exportUri.emit(finalUri)
        }
    }
    
    suspend fun getPointsForSession(sessionId: String) = repository.getPointsForSession(sessionId)

    suspend fun getSession(sessionId: String) = repository.getSession(sessionId)

    // E2: per-session metrics computed on demand from the session's points.
    suspend fun getSessionStats(sessionId: String): SessionStats =
        SessionStatsComputer.compute(repository.getPointsForSession(sessionId))

    // E4/D7/D8: segments.
    val segmentListItems: StateFlow<List<SegmentListItem>> = repository.segmentListItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getSegment(segmentId: String) = repository.getSegment(segmentId)

    fun getAttemptsForSegment(segmentId: String) = repository.getAttemptsForSegment(segmentId)

    fun deleteSegment(segmentId: String) {
        viewModelScope.launch { repository.deleteSegment(segmentId) }
    }

    fun setSegmentGoal(segment: Segment, goal: Boolean) {
        viewModelScope.launch { repository.updateSegment(segment.copy(isGoal = goal)) }
    }

    fun updateSessionNotes(session: Session, notes: String) {
        viewModelScope.launch {
            repository.updateSession(session.copy(notes = notes))
        }
    }
}
