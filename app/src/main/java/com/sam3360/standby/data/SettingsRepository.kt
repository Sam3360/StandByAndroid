package com.sam3360.standby.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sam3360.standby.model.StandBySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "standby_prefs")

class SettingsRepository(private val context: Context) {
    val settingsFlow: Flow<StandBySettings> = context.dataStore.data.map { prefs ->
        StandBySettings.fromPreferences(prefs)
    }

    val photoUrisFlow: Flow<List<Uri>> = context.dataStore.data.map { prefs ->
        (prefs[PHOTO_URIS] ?: emptySet()).map { Uri.parse(it) }
    }

    suspend fun saveSettings(settings: StandBySettings) {
        context.dataStore.edit { prefs ->
            prefs[StandBySettings.CLOCK_STYLE] = settings.clockStyle
            prefs[StandBySettings.USE_24_HOUR] = settings.use24Hour
            prefs[StandBySettings.DIMMING] = settings.dimming
            prefs[StandBySettings.PHOTO_INTERVAL] = settings.photoIntervalSeconds
            prefs[StandBySettings.KEEP_SCREEN_ON] = settings.keepScreenOn
            prefs[StandBySettings.AUTO_ACTIVATE_MODE] = settings.autoActivateMode
            prefs[StandBySettings.POWER_SAVE_MODE] = settings.powerSaveMode
            prefs[StandBySettings.PHOTO_INDEX] = settings.photoIndex
            prefs[StandBySettings.NIGHT_MODE] = settings.nightMode
            prefs[StandBySettings.SHOW_SETTINGS] = settings.showSettings
        }
    }

    suspend fun updateSettings(mutator: (StandBySettings) -> StandBySettings) {
        val current = getCurrentSettings()
        val updated = mutator(current)
        saveSettings(updated)
    }

    suspend fun getCurrentSettings(): StandBySettings {
        return settingsFlow.firstOrNull() ?: StandBySettings()
    }

    suspend fun setPhotoUris(uris: List<Uri>) {
        context.dataStore.edit { prefs ->
            prefs[PHOTO_URIS] = uris.map { it.toString() }.toSet()
        }
    }

    companion object {
        val PHOTO_URIS = stringSetPreferencesKey("photo_uris")
    }
}
