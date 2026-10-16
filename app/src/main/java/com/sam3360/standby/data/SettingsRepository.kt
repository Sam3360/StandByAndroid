package com.sam3360.standby.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val orderedUris = prefs[PHOTO_URIS_ORDERED]
        val storedUris = orderedUris?.split('\n')?.filter(String::isNotBlank)
            ?: (prefs[PHOTO_URIS] ?: emptySet()).sorted()
        storedUris.map(Uri::parse)
    }

    suspend fun saveSettings(settings: StandBySettings) {
        context.dataStore.edit { prefs ->
            prefs.writeSettings(settings)
        }
    }

    suspend fun updateSettings(mutator: (StandBySettings) -> StandBySettings) {
        context.dataStore.edit { prefs ->
            prefs.writeSettings(mutator(StandBySettings.fromPreferences(prefs)))
        }
    }

    suspend fun getCurrentSettings(): StandBySettings {
        return settingsFlow.firstOrNull() ?: StandBySettings()
    }

    suspend fun setPhotoUris(uris: List<Uri>) {
        context.dataStore.edit { prefs ->
            val uniqueUris = uris.distinctBy(Uri::toString)
            prefs[PHOTO_URIS] = uniqueUris.map(Uri::toString).toSet()
            prefs[PHOTO_URIS_ORDERED] = uniqueUris.joinToString("\n") { it.toString() }
            val photoCount = uniqueUris.size
            prefs[StandBySettings.PHOTO_INDEX] = if (photoCount == 0) {
                0
            } else {
                (prefs[StandBySettings.PHOTO_INDEX] ?: 0).coerceIn(0, photoCount - 1)
            }
        }
    }

    companion object {
        val PHOTO_URIS = stringSetPreferencesKey("photo_uris")
        private val PHOTO_URIS_ORDERED = stringPreferencesKey("photo_uris_ordered")
    }
}

private fun MutablePreferences.writeSettings(settings: StandBySettings) {
    this[StandBySettings.CLOCK_STYLE] = settings.clockStyle
    this[StandBySettings.USE_24_HOUR] = settings.use24Hour
    this[StandBySettings.DIMMING] = settings.dimming
    this[StandBySettings.PHOTO_INTERVAL] = settings.photoIntervalSeconds
    this[StandBySettings.KEEP_SCREEN_ON] = settings.keepScreenOn
    this[StandBySettings.AUTO_ACTIVATE_MODE] = settings.autoActivateMode
    this[StandBySettings.POWER_SAVE_MODE] = settings.powerSaveMode
    this[StandBySettings.PHOTO_INDEX] = settings.photoIndex
    this[StandBySettings.NIGHT_MODE] = settings.nightMode
    this[StandBySettings.SHOW_SETTINGS] = settings.showSettings
}
