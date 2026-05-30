package eu.monniot.speed.sensor

import android.annotation.SuppressLint
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class SatelliteInfo(
    val usedInFix: Int = 0,
    val visible: Int = 0
)

class GpsCollector(
    private val client: FusedLocationProviderClient,
    private val locationManager: LocationManager,
    private val rawSink: RawSensorSink? = null
) {

    private val _locationFlow = MutableSharedFlow<Location>(extraBufferCapacity = 16)
    val locationFlow: SharedFlow<Location> = _locationFlow

    private val _satellitesFlow = MutableStateFlow(SatelliteInfo())
    val satellitesFlow: StateFlow<SatelliteInfo> = _satellitesFlow

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                _locationFlow.tryEmit(it)
                rawSink?.onLocationEvent(it)
            }
        }
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            val visible = status.satelliteCount
            for (i in 0 until visible) {
                if (status.usedInFix(i)) {
                    used++
                }
            }
            _satellitesFlow.value = SatelliteInfo(used, visible)
            rawSink?.onGnssStatusEvent(used, visible)
        }
    }

    @SuppressLint("MissingPermission")
    fun start(intervalMs: Long = 100L) {
        if (_isActive.value) return

        // We do not set setMaxUpdateDelayMillis because it, for some reason, results in GPS
        // updates coming in every 5 seconds. Probably some interference with the built-in
        // 1Hz refresh rate of the GNSS receiver.
        // On my phone (Flip 7), leaving it out bring down the number of updates to 1 to 2
        // location update per second.
        // intervalMs is derived from the GPS-rate setting (1000/Hz); the fastest interval is
        // half of it so the receiver can deliver early when it has a fix sooner.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis((intervalMs / 2).coerceAtLeast(1L))
            .build()

        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null)
        } catch (e: Exception) {
            // Handle cases where GNSS status might not be available
        }
        
        _isActive.value = true
    }

    fun stop() {
        if (!_isActive.value) return

        client.removeLocationUpdates(locationCallback)
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        _isActive.value = false
        _satellitesFlow.value = SatelliteInfo()
    }
}
