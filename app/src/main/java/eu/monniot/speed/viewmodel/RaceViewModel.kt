package eu.monniot.speed.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.monniot.speed.data.RaceDatabase
import eu.monniot.speed.data.RaceRepository
import eu.monniot.speed.data.SessionSummary
import eu.monniot.speed.service.RaceRecordingService
import eu.monniot.speed.service.ServiceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.content.Intent

class RaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RaceRepository
    val serviceState: StateFlow<ServiceState> = RaceRecordingService.state
    val sessions: Flow<List<SessionSummary>>

    private val _exportUri = MutableSharedFlow<Uri>()
    val exportUri: SharedFlow<Uri> = _exportUri

    init {
        val dao = RaceDatabase.getDatabase(application).dataPointDao()
        repository = RaceRepository(dao)
        sessions = repository.sessionSummaries
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
        }
    }

    fun exportSession(sessionId: String) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val exportFile = File(context.cacheDir, "race_session_${sessionId}.csv")
            
            val startNs = serviceState.value.sessionStartElapsedNs
            
            exportFile.outputStream().use { stream ->
                repository.exportToCsv(sessionId, stream, startNs)
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                exportFile
            )
            _exportUri.emit(uri)
        }
    }
    
    suspend fun getPointsForSession(sessionId: String) = repository.getPointsForSession(sessionId)
}
