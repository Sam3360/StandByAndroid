package com.sam3360.standby.model

import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import java.time.LocalDateTime

data class StandBySettings(
    val clockStyle: String = "digital",
    val use24Hour: Boolean = true,
    val dimming: Float = 0.30f,
    val photoIntervalSeconds: Int = 10,
    val keepScreenOn: Boolean = true,
    val autoActivateMode: String = "manual",
    val powerSaveMode: Boolean = false,
    val photoIndex: Int = 0,
    val nightMode: Boolean = false,
    val showSettings: Boolean = false
) {
    companion object {
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val DIMMING = floatPreferencesKey("dimming")
        val PHOTO_INTERVAL = intPreferencesKey("photo_interval")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_ACTIVATE_MODE = stringPreferencesKey("auto_activate_mode")
        val POWER_SAVE_MODE = booleanPreferencesKey("power_save_mode")
        val PHOTO_INDEX = intPreferencesKey("photo_index")
        val NIGHT_MODE = booleanPreferencesKey("night_mode")
        val SHOW_SETTINGS = booleanPreferencesKey("show_settings")
        val PHOTO_URIS = stringPreferencesKey("photo_uris")

        fun fromPreferences(preferences: Preferences): StandBySettings = StandBySettings(
            clockStyle = preferences[CLOCK_STYLE] ?: "digital",
            use24Hour = preferences[USE_24_HOUR] ?: true,
            dimming = preferences[DIMMING] ?: 0.30f,
            photoIntervalSeconds = preferences[PHOTO_INTERVAL] ?: 10,
            keepScreenOn = preferences[KEEP_SCREEN_ON] ?: true,
            autoActivateMode = preferences[AUTO_ACTIVATE_MODE] ?: "manual",
            powerSaveMode = preferences[POWER_SAVE_MODE] ?: false,
            photoIndex = preferences[PHOTO_INDEX] ?: 0,
            nightMode = preferences[NIGHT_MODE] ?: false,
            showSettings = preferences[SHOW_SETTINGS] ?: false
        )
    }
}

data class StandByUiState(
    val settings: StandBySettings = StandBySettings(),
    val batteryLevel: Int = 0,
    val chargingState: Int = 0,
    val currentTime: LocalDateTime = LocalDateTime.now(),
    val currentDate: LocalDate = LocalDate.now(),
    val upcomingEvents: List<CalendarEvent> = emptyList(),
    val selectedPhotos: List<Uri> = emptyList(),
    val showSettings: Boolean = false,
    val nightMode: Boolean = false
)

data class CalendarEvent(
    val title: String,
    val startTimeMillis: Long,
    val location: String? = null
)
