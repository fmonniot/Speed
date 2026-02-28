package eu.monniot.speed.sensor

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class GpsCollector(private val client: FusedLocationProviderClient) {

    private val _locationFlow = MutableSharedFlow<Location>(extraBufferCapacity = 16)
    val locationFlow: SharedFlow<Location> = _locationFlow

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                _locationFlow.tryEmit(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100L)
            .setMinUpdateIntervalMillis(50L)
            .setMaxUpdateDelayMillis(200L)
            .build()

        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        _isActive.value = true
    }

    fun stop() {
        client.removeLocationUpdates(locationCallback)
        _isActive.value = false
    }
}
