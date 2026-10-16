package com.sam3360.standby.util

import android.os.BatteryManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object StandByLogic {
    fun formatTime(time: LocalDateTime, use24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun formatDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    }

    fun chargingStateLabel(state: Int): String {
        return when (state) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> "Unknown"
        }
    }

    fun batteryPercent(level: Int, scale: Int): Int {
        return if (level > -1 && scale > 0) {
            (level * 100f / scale).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    fun nextIndex(current: Int, size: Int): Int {
        if (size <= 0) return 0
        return (Math.floorMod(current, size) + 1) % size
    }

    fun previousIndex(current: Int, size: Int): Int {
        if (size <= 0) return 0
        return Math.floorMod(current - 1, size)
    }
}
