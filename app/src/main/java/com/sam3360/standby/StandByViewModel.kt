package com.sam3360.standby

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam3360.standby.data.SettingsRepository
import com.sam3360.standby.model.CalendarEvent
import com.sam3360.standby.model.StandBySettings
import com.sam3360.standby.model.StandByUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.net.Uri

class StandByViewModel(
    private val appContext: Context,
    private val activity: Activity
) : ViewModel() {

    private val settingsRepository = SettingsRepository(appContext)
    private val batteryFlow = MutableStateFlow(StandByBatteryState())
    private val timerFlow = MutableStateFlow(LocalDateTime.now())
    private val calendarEventFlow = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val photoUrisFlow = MutableStateFlow<List<Uri>>(emptyList())
    private val settingsFlow = settingsRepository.settingsFlow
    private val showSettingsFlow = MutableStateFlow(false)
    private val nightModeFlow = MutableStateFlow(false)

    val uiState: StateFlow<StandByUiState> = combine(
        settingsFlow,
        batteryFlow,
        timerFlow,
        calendarEventFlow,
        photoUrisFlow,
        showSettingsFlow,
        nightModeFlow
    ) { settings: StandBySettings,
        battery: StandByBatteryState,
        time: LocalDateTime,
        events: List<CalendarEvent>,
        photos: List<Uri>,
        showSettings: Boolean,
        nightMode: Boolean ->
        StandByUiState(
            settings = settings,
            batteryLevel = battery.level,
            chargingState = battery.chargingState,
            currentTime = time,
            currentDate = time.toLocalDate(),
            upcomingEvents = events,
            selectedPhotos = photos,
            showSettings = showSettings,
            nightMode = nightMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StandByUiState())

    init {
        viewModelScope.launch {
            settingsRepository.photoUrisFlow.collect { uris ->
                photoUrisFlow.value = uris
            }
        }
        viewModelScope.launch {
            while (true) {
                timerFlow.value = LocalDateTime.now()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun toggleSettings() {
        showSettingsFlow.value = !showSettingsFlow.value
    }

    fun toggleNightMode() {
        nightModeFlow.value = !nightModeFlow.value
    }

    fun togglePowerSave() {
        viewModelScope.launch {
            val current = settingsRepository.getCurrentSettings()
            settingsRepository.updateSettings { it.copy(powerSaveMode = !current.powerSaveMode) }
        }
    }

    fun updateClockStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(clockStyle = style) }
        }
    }

    fun update24Hour(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(use24Hour = value) }
        }
    }

    fun updatePhotoInterval(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(photoIntervalSeconds = value) }
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(keepScreenOn = value) }
        }
    }

    fun toggleAutoActivate() {
        viewModelScope.launch {
            val current = settingsRepository.getCurrentSettings()
            val next = when (current.autoActivateMode) {
                "manual" -> "charging"
                "charging" -> "landscape"
                else -> "manual"
            }
            settingsRepository.updateSettings { it.copy(autoActivateMode = next) }
        }
    }

    fun updateBattery(level: Int, scale: Int, status: Int) {
        val pct = if (level > -1 && scale > 0) (level * 100f / scale).toInt() else 0
        batteryFlow.value = StandByBatteryState(
            level = pct,
            chargingState = status
        )
    }

    fun onCalendarPermissionResult(granted: Boolean) {
        if (granted) {
            loadCalendarEvents(activity)
        }
    }

    fun setPhotoSelection(uris: List<Uri>) {
        val existing = photoUrisFlow.value.toMutableList()
        existing.addAll(uris)
        val unique = existing.distinctBy { it.toString() }
        photoUrisFlow.value = unique
        viewModelScope.launch { settingsRepository.setPhotoUris(unique) }
    }

    fun nextPhoto() {
        if (photoUrisFlow.value.isNotEmpty()) {
            viewModelScope.launch {
                val currentIndex = settingsRepository.getCurrentSettings().photoIndex
                val nextIndex = (currentIndex + 1) % photoUrisFlow.value.size
                settingsRepository.updateSettings { it.copy(photoIndex = nextIndex) }
            }
        }
    }

    fun previousPhoto() {
        if (photoUrisFlow.value.isNotEmpty()) {
            viewModelScope.launch {
                val currentIndex = settingsRepository.getCurrentSettings().photoIndex
                val nextIndex = if (currentIndex == 0) photoUrisFlow.value.size - 1 else currentIndex - 1
                settingsRepository.updateSettings { it.copy(photoIndex = nextIndex) }
            }
        }
    }

    fun loadCalendarEvents(activity: Activity) {
        viewModelScope.launch {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                calendarEventFlow.value = emptyList()
                return@launch
            }

            try {
                val projection = arrayOf(
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.EVENT_LOCATION
                )
                val now = System.currentTimeMillis()
                val end = now + 1000L * 60L * 60L * 24L * 30L
                val cursor = CalendarContract.Instances.query(
                    activity.contentResolver,
                    projection,
                    now,
                    end,
                    null
                )
                val results = mutableListOf<CalendarEvent>()
                cursor?.use {
                    val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
                    val locationIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                    while (it.moveToNext()) {
                        val title = it.getString(titleIndex) ?: "Untitled event"
                        val begin = it.getLong(beginIndex)
                        val location = it.getString(locationIndex)
                        results.add(CalendarEvent(title = title, startTimeMillis = begin, location = location))
                    }
                }
                calendarEventFlow.value = results.sortedBy { it.startTimeMillis }.take(5)
            } catch (e: Exception) {
                Log.e("StandByViewModel", "Failed to load calendar events", e)
                calendarEventFlow.value = emptyList()
            }
        }
    }
}

private data class StandByBatteryState(
    val level: Int = 0,
    val chargingState: Int = BatteryManager.BATTERY_STATUS_DISCHARGING
)
