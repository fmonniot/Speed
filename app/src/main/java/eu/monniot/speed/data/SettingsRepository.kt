package eu.monniot.speed.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val AUTO_START_SENSORS = booleanPreferencesKey("auto_start_sensors")
    }

    val autoStartSensors: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_START_SENSORS] ?: false
        }

    suspend fun setAutoStartSensors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_SENSORS] = enabled
        }
    }
}
