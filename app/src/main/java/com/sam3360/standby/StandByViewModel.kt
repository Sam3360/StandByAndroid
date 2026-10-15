package com.sam3360.standby

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.BatteryManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam3360.standby.data.SettingsRepository
import com.sam3360.standby.model.CalendarEvent
import com.sam3360.standby.model.StandByUiState
import com.sam3360.standby.util.StandByLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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

    private val baseUiStateFlow = combine(
        settingsFlow,
        batteryFlow,
        timerFlow,
        calendarEventFlow,
        photoUrisFlow
    ) { settings,
        battery: StandByBatteryState,
        time: LocalDateTime,
        events: List<CalendarEvent>,
        photos: List<Uri> ->
        StandByUiState(
            settings = settings,
            batteryLevel = battery.level,
            chargingState = battery.chargingState,
            currentTime = time,
            currentDate = time.toLocalDate(),
            upcomingEvents = events,
            selectedPhotos = photos,
            nightMode = settings.nightMode
        )
    }

    val uiState: StateFlow<StandByUiState> = combine(baseUiStateFlow, showSettingsFlow) { state, showSettings ->
        state.copy(showSettings = showSettings)
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
                delay(1000)
            }
        }
        viewModelScope.launch {
            combine(photoUrisFlow, settingsFlow) { photos, settings -> photos to settings }
                .collectLatest { (photos, settings) ->
                    if (photos.isNotEmpty()) {
                        val intervalMillis = settings.photoIntervalSeconds
                            .coerceIn(MIN_PHOTO_INTERVAL_SECONDS, MAX_PHOTO_INTERVAL_SECONDS) * 1000L
                        while (true) {
                            delay(intervalMillis)
                            advancePhoto(photos.size)
                        }
                    }
                }
        }
    }

    private fun advancePhoto(photoCount: Int = photoUrisFlow.value.size) {
        if (photoCount > 0) {
            viewModelScope.launch {
                settingsRepository.updateSettings { settings ->
                    settings.copy(photoIndex = StandByLogic.nextIndex(settings.photoIndex, photoCount))
                }
            }
        }
    }

    fun toggleSettings() {
        showSettingsFlow.value = !showSettingsFlow.value
    }

    fun toggleNightMode() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(nightMode = !it.nightMode) }
        }
    }

    fun togglePowerSave() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(powerSaveMode = !it.powerSaveMode) }
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
            settingsRepository.updateSettings {
                it.copy(photoIntervalSeconds = value.coerceIn(MIN_PHOTO_INTERVAL_SECONDS, MAX_PHOTO_INTERVAL_SECONDS))
            }
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
        advancePhoto()
    }

    fun previousPhoto() {
        val photoCount = photoUrisFlow.value.size
        if (photoCount > 0) {
            viewModelScope.launch {
                settingsRepository.updateSettings { settings ->
                    settings.copy(photoIndex = StandByLogic.previousIndex(settings.photoIndex, photoCount))
                }
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

private const val MIN_PHOTO_INTERVAL_SECONDS = 4
private const val MAX_PHOTO_INTERVAL_SECONDS = 20
