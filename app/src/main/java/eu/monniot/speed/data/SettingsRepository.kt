package eu.monniot.speed.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val AUTO_START_SENSORS = booleanPreferencesKey("auto_start_sensors")
        val RECORD_RAW_TRACES = booleanPreferencesKey("record_raw_traces")
        // Redesign settings (§4.9)
        val GPS_RATE_HZ = intPreferencesKey("gps_rate_hz")
        val IMU_RATE_HZ = intPreferencesKey("imu_rate_hz")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val UNITS = stringPreferencesKey("units")
        // Legacy boolean key (pre-F4). Still read for migration; no longer written.
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")

        const val DEFAULT_GPS_RATE_HZ = 10
        const val DEFAULT_IMU_RATE_HZ = 100
    }

    val autoStartSensors: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_START_SENSORS] ?: false
        }

    val recordRawTraces: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[RECORD_RAW_TRACES] ?: false
        }

    val gpsRateHz: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[GPS_RATE_HZ] ?: DEFAULT_GPS_RATE_HZ }

    val imuRateHz: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[IMU_RATE_HZ] ?: DEFAULT_IMU_RATE_HZ }

    val autoPause: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_PAUSE] ?: false }

    val units: Flow<Units> = context.dataStore.data
        .map { preferences -> Units.fromStorage(preferences[UNITS]) }

    // F4: tri-state theme. New THEME_MODE key wins; if absent, migrate the legacy DARK_THEME
    // boolean (true -> DARK, false/absent -> LIGHT) so existing users keep their scheme.
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE]?.let { ThemeMode.fromStorage(it) }
                ?: if (preferences[DARK_THEME] == true) ThemeMode.DARK else ThemeMode.LIGHT
        }

    suspend fun setAutoStartSensors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_SENSORS] = enabled
        }
    }

    suspend fun setRecordRawTraces(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RECORD_RAW_TRACES] = enabled
        }
    }

    suspend fun setGpsRateHz(hz: Int) {
        context.dataStore.edit { preferences -> preferences[GPS_RATE_HZ] = hz }
    }

    suspend fun setImuRateHz(hz: Int) {
        context.dataStore.edit { preferences -> preferences[IMU_RATE_HZ] = hz }
    }

    suspend fun setAutoPause(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[AUTO_PAUSE] = enabled }
    }

    suspend fun setUnits(units: Units) {
        context.dataStore.edit { preferences -> preferences[UNITS] = units.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }
}
